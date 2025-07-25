package ai.falsify.crawlers.common.service;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.exception.ContentValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for validating crawled content quality and integrity.
 * Performs content length validation, required fields validation,
 * duplication detection, and malformed content detection.
 */
@ApplicationScoped
public class ContentValidator {

    private static final Logger LOG = Logger.getLogger(ContentValidator.class);

    @Inject
    CrawlerConfiguration config;

    // Cache for duplicate content detection (in production, this should be backed
    // by Redis)
    private final Set<String> contentHashes = new HashSet<>();

    // Patterns for detecting malformed content
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern EXCESSIVE_WHITESPACE_PATTERN = Pattern.compile("\\s{3,}");
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>.*?</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("<style[^>]*>.*?</style>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Validates an article's content according to configured rules.
     *
     * @param title   the article title
     * @param url     the article URL
     * @param content the article content
     * @throws ContentValidationException if validation fails
     */
    public void validateArticle(String title, String url, String content) throws ContentValidationException {
        if (!config.content().enableContentValidation()) {
            LOG.debug("Content validation is disabled, skipping validation");
            return;
        }

        LOG.debugf("Validating article content for URL: %s", url);

        validateRequiredFields(title, url, content);
        validateContentLength(content);
        validateContentQuality(content);
        validateNoDuplication(content, url);

        LOG.debugf("Content validation passed for URL: %s", url);
    }

    /**
     * Validates that all required fields are present and not empty.
     */
    private void validateRequiredFields(String title, String url, String content) throws ContentValidationException {
        if (title == null || title.trim().isEmpty()) {
            throw ContentValidationException.missingRequiredField("title");
        }

        if (url == null || url.trim().isEmpty()) {
            throw ContentValidationException.missingRequiredField("url");
        }

        if (content == null || content.trim().isEmpty()) {
            throw ContentValidationException.missingRequiredField("content");
        }

        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    "URL must be a valid HTTP/HTTPS URL: " + url,
                    "url");
        }
    }

    /**
     * Validates content length against configured minimum and maximum limits.
     */
    private void validateContentLength(String content) throws ContentValidationException {
        int contentLength = content.trim().length();
        int minLength = config.content().minContentLength();
        int maxLength = config.content().maxContentLength();

        if (contentLength < minLength) {
            throw ContentValidationException.contentTooShort(contentLength, minLength);
        }

        if (contentLength > maxLength) {
            throw ContentValidationException.contentTooLong(contentLength, maxLength);
        }
    }

    /**
     * Validates content quality by detecting malformed or low-quality content.
     */
    private void validateContentQuality(String content) throws ContentValidationException {
        // Check for excessive HTML tags (indicates poor content extraction)
        String htmlTagsRemoved = HTML_TAG_PATTERN.matcher(content).replaceAll("");
        double htmlRatio = (double) (content.length() - htmlTagsRemoved.length()) / content.length();

        if (htmlRatio > 0.3) { // More than 30% HTML tags
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    String.format("Content contains too many HTML tags (%.1f%%), indicating poor extraction",
                            htmlRatio * 100),
                    "content.htmlRatio");
        }

        // Check for script or style tags (should be cleaned during extraction)
        if (SCRIPT_TAG_PATTERN.matcher(content).find()) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    "Content contains script tags, indicating incomplete cleaning",
                    "content.scriptTags");
        }

        if (STYLE_TAG_PATTERN.matcher(content).find()) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    "Content contains style tags, indicating incomplete cleaning",
                    "content.styleTags");
        }

        // Check for excessive whitespace
        if (EXCESSIVE_WHITESPACE_PATTERN.matcher(content).find()) {
            LOG.warn("Content contains excessive whitespace, may indicate formatting issues");
        }

        // Check for minimum word count (rough estimate)
        String[] words = htmlTagsRemoved.trim().split("\\s+");
        if (words.length < 10) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    String.format("Content has too few words (%d), may not be meaningful", words.length),
                    "content.wordCount");
        }

        // Check for repetitive content patterns
        if (hasRepetitivePatterns(htmlTagsRemoved)) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    "Content appears to have repetitive patterns, may be auto-generated",
                    "content.repetitivePatterns");
        }
    }

    /**
     * Validates that the content is not a duplicate of previously processed
     * content.
     */
    private void validateNoDuplication(String content, String url) throws ContentValidationException {
        String contentHash = generateContentHash(content);

        if (contentHashes.contains(contentHash)) {
            throw new ContentValidationException(
                    ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED,
                    "Duplicate content detected for URL: " + url,
                    "content.duplicate");
        }

        // Add to cache for future duplicate detection
        contentHashes.add(contentHash);

        // Prevent memory growth by limiting cache size (in production, use Redis with
        // TTL)
        if (contentHashes.size() > 10000) {
            LOG.warn("Content hash cache is getting large, consider using Redis for production");
            // Remove oldest entries (simple approach - in production use LRU cache)
            if (contentHashes.size() > 15000) {
                contentHashes.clear();
                LOG.info("Cleared content hash cache to prevent memory issues");
            }
        }
    }

    /**
     * Generates a hash for content deduplication.
     * Uses a normalized version of the content to handle minor formatting
     * differences.
     */
    private String generateContentHash(String content) {
        // Normalize content for hashing
        String normalized = content
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("[\\p{Punct}&&[^.!?]]", "") // Remove punctuation except sentence endings
                .toLowerCase()
                .trim();

        return String.valueOf(normalized.hashCode());
    }

    /**
     * Detects repetitive patterns in content that might indicate auto-generated
     * text.
     */
    private boolean hasRepetitivePatterns(String content) {
        String[] sentences = content.split("[.!?]+");

        if (sentences.length < 3) {
            return false;
        }

        // Check for repeated sentences
        Set<String> uniqueSentences = new HashSet<>();
        int duplicateSentences = 0;

        for (String sentence : sentences) {
            String normalizedSentence = sentence.trim().toLowerCase();
            if (normalizedSentence.length() > 10) { // Only check meaningful sentences
                if (!uniqueSentences.add(normalizedSentence)) {
                    duplicateSentences++;
                }
            }
        }

        // If more than 20% of sentences are duplicates, consider it repetitive
        double duplicateRatio = (double) duplicateSentences / sentences.length;
        return duplicateRatio > 0.2;
    }

    /**
     * Clears the content hash cache (for testing or administrative purposes).
     */
    public void clearContentCache() {
        contentHashes.clear();
        LOG.info("Content hash cache cleared");
    }

    /**
     * Gets the current size of the content hash cache (for monitoring).
     */
    public int getContentCacheSize() {
        return contentHashes.size();
    }

    /**
     * Validates content without throwing exceptions, returning validation result.
     * Useful for batch processing where you want to collect all validation errors.
     */
    public ValidationResult validateArticleWithResult(String title, String url, String content) {
        try {
            validateArticle(title, url, content);
            return ValidationResult.success();
        } catch (ContentValidationException e) {
            return ValidationResult.failure(e);
        }
    }

    /**
     * Result of content validation operation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final ContentValidationException error;

        private ValidationResult(boolean valid, ContentValidationException error) {
            this.valid = valid;
            this.error = error;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(ContentValidationException error) {
            return new ValidationResult(false, error);
        }

        public boolean isValid() {
            return valid;
        }

        public ContentValidationException getError() {
            return error;
        }

        public String getErrorMessage() {
            return error != null ? error.getMessage() : null;
        }
    }
}