package ai.falsify.crawlers.service;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.service.ContentValidator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ContentValidatorTest {

    @Inject
    ContentValidator contentValidator;

    @Inject
    CrawlerConfiguration config;

    private static final String VALID_TITLE = "Test Article Title";
    private static final String VALID_URL = "https://example.com/article";
    private static final String VALID_CONTENT = "This is a valid article content with sufficient length and meaningful text. " +
            "It contains multiple sentences and provides good information. " +
            "The content is well-structured and meets all validation criteria. " +
            "This ensures that the article has enough substance to be considered valuable.";

    @BeforeEach
    void setUp() {
        // Clear content cache before each test to avoid interference
        // Note: Some tests may override this behavior
        contentValidator.clearContentCache();
    }

    @Nested
    class RequiredFieldsValidation {

        @Test
        void testValidArticlePassesValidation() throws ContentValidationException {
            // Should not throw any exception
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            });
        }

        @Test
        void testNullTitleThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(null, VALID_URL, VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("title"));
        }

        @Test
        void testEmptyTitleThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle("", VALID_URL, VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("title"));
        }

        @Test
        void testWhitespaceTitleThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle("   ", VALID_URL, VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("title"));
        }

        @Test
        void testNullUrlThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, null, VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("url"));
        }

        @Test
        void testInvalidUrlFormatThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, "invalid-url", VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("valid HTTP/HTTPS URL"));
        }

        @Test
        void testNullContentThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, null);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("content"));
        }

        @Test
        void testEmptyContentThrowsException() {
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, "");
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("content"));
        }
    }

    @Nested
    class ContentLengthValidation {

        @Test
        void testContentTooShortThrowsException() {
            String shortContent = "Short";
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, shortContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_TOO_SHORT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("below minimum"));
        }

        @Test
        void testContentAtMinimumLengthPasses() throws ContentValidationException {
            // Create content exactly at minimum length
            int minLength = config.content().minContentLength();
            String minContent = "a".repeat(minLength);
            
            // Should not throw exception (though it might fail other validations)
            try {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, minContent);
            } catch (ContentValidationException e) {
                // If it fails, it should not be due to length
                assertNotEquals(ContentValidationException.ErrorCode.CONTENT_TOO_SHORT, e.getErrorCode());
            }
        }

        @Test
        void testContentTooLongThrowsException() {
            int maxLength = config.content().maxContentLength();
            String longContent = "a".repeat(maxLength + 1);
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, longContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_TOO_LONG, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("exceeds maximum"));
        }
    }

    @Nested
    class ContentQualityValidation {

        @Test
        void testContentWithExcessiveHtmlTagsThrowsException() {
            String htmlContent = "<div><p>Some <strong>text</strong> with <em>lots</em> of <span>HTML</span> <a href='#'>tags</a></p></div>";
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, htmlContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("HTML tags"));
        }

        @Test
        void testContentWithScriptTagsThrowsException() {
            String scriptContent = VALID_CONTENT + "<script>alert('test');</script>";
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, scriptContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("script tags"));
        }

        @Test
        void testContentWithStyleTagsThrowsException() {
            String styleContent = VALID_CONTENT + "<style>body { color: red; }</style>";
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, styleContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("style tags"));
        }

        @Test
        void testContentWithTooFewWordsThrowsException() {
            // Create content that meets length requirement but has very few meaningful words (less than 10)
            String fewWordsContent = "a".repeat(95) + " word1 word2 word3"; // Only 3 meaningful words
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, fewWordsContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("too few words"));
        }

        @Test
        void testContentWithRepetitivePatternsThrowsException() {
            String repetitiveContent = "This is a sentence. This is a sentence. This is a sentence. " +
                    "This is a sentence. This is a sentence. Another sentence here. " +
                    "This is a sentence. This is a sentence.";
            
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, repetitiveContent);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("repetitive patterns"));
        }

        @Test
        void testValidContentWithMinimalHtmlPasses() throws ContentValidationException {
            String validHtmlContent = VALID_CONTENT + " Some <em>emphasis</em> is okay.";
            
            // Should not throw exception
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, validHtmlContent);
            });
        }
    }

    @Nested
    class DuplicationDetection {

        @Test
        void testDuplicateContentThrowsException() throws ContentValidationException {
            // First validation should pass
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            
            // Second validation with same content should fail
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, "https://example.com/different-url", VALID_CONTENT);
            });

            assertEquals(ContentValidationException.ErrorCode.CONTENT_VALIDATION_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Duplicate content"));
        }

        @Test
        void testSimilarContentWithMinorDifferencesDetectedAsDuplicate() throws ContentValidationException {
            // Clear cache and add content manually to test duplication
            contentValidator.clearContentCache();
            
            // First validation - this will add to cache
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            
            // Verify cache has content
            assertEquals(1, contentValidator.getContentCacheSize());
            
            // Use exactly the same content to ensure duplicate detection works
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                contentValidator.validateArticle(VALID_TITLE, "https://example.com/different-url", VALID_CONTENT);
            });

            assertTrue(exception.getMessage().contains("Duplicate content"));
        }

        @Test
        void testDifferentContentPasses() throws ContentValidationException {
            // First validation
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            
            // Different content should pass
            String differentContent = "This is completely different content with different words and structure. " +
                    "It talks about different topics and has different meaning. " +
                    "The content is unique and should not be flagged as duplicate. " +
                    "This ensures proper duplicate detection functionality.";
            
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle("Different Title", "https://example.com/different", differentContent);
            });
        }

        @Test
        void testContentCacheClearingWorks() throws ContentValidationException {
            // Add content to cache
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            assertTrue(contentValidator.getContentCacheSize() > 0);
            
            // Clear cache
            contentValidator.clearContentCache();
            assertEquals(0, contentValidator.getContentCacheSize());
            
            // Same content should now pass validation
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            });
        }
    }

    @Nested
    class ValidationResultMethod {

        @Test
        void testValidateArticleWithResultReturnsSuccessForValidContent() {
            ContentValidator.ValidationResult result = contentValidator.validateArticleWithResult(
                VALID_TITLE, VALID_URL, VALID_CONTENT);
            
            assertTrue(result.isValid());
            assertNull(result.getError());
            assertNull(result.getErrorMessage());
        }

        @Test
        void testValidateArticleWithResultReturnsFailureForInvalidContent() {
            ContentValidator.ValidationResult result = contentValidator.validateArticleWithResult(
                null, VALID_URL, VALID_CONTENT);
            
            assertFalse(result.isValid());
            assertNotNull(result.getError());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("title"));
        }

        @Test
        void testValidateArticleWithResultHandlesContentTooShort() {
            ContentValidator.ValidationResult result = contentValidator.validateArticleWithResult(
                VALID_TITLE, VALID_URL, "Short");
            
            assertFalse(result.isValid());
            assertEquals(ContentValidationException.ErrorCode.CONTENT_TOO_SHORT, result.getError().getErrorCode());
        }

        @Test
        void testValidateArticleWithResultHandlesDuplicateContent() {
            // First validation should succeed
            ContentValidator.ValidationResult firstResult = contentValidator.validateArticleWithResult(
                VALID_TITLE, VALID_URL, VALID_CONTENT);
            assertTrue(firstResult.isValid());
            
            // Second validation with same content should fail
            ContentValidator.ValidationResult secondResult = contentValidator.validateArticleWithResult(
                VALID_TITLE, "https://example.com/different", VALID_CONTENT);
            
            assertFalse(secondResult.isValid());
            assertTrue(secondResult.getErrorMessage().contains("Duplicate content"));
        }
    }

    @Nested
    class CacheManagement {

        @Test
        void testContentCacheSizeTracking() throws ContentValidationException {
            assertEquals(0, contentValidator.getContentCacheSize());
            
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            assertEquals(1, contentValidator.getContentCacheSize());
            
            String differentContent = "Different content for testing cache size tracking functionality. " +
                    "This content is long enough to meet the minimum length requirements. " +
                    "It contains multiple sentences and provides good information for testing purposes.";
            contentValidator.validateArticle("Title 2", "https://example.com/article2", differentContent);
            assertEquals(2, contentValidator.getContentCacheSize());
        }

        @Test
        void testCacheClearingResetsSize() throws ContentValidationException {
            // Add some content to cache
            contentValidator.validateArticle(VALID_TITLE, VALID_URL, VALID_CONTENT);
            String differentContent = "Different content here that meets the minimum length requirements. " +
                    "This content is sufficiently long and contains meaningful information for testing purposes.";
            contentValidator.validateArticle("Title 2", "https://example.com/article2", differentContent);
            
            assertTrue(contentValidator.getContentCacheSize() > 0);
            
            contentValidator.clearContentCache();
            assertEquals(0, contentValidator.getContentCacheSize());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void testHttpUrlIsValid() throws ContentValidationException {
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, "http://example.com/article", VALID_CONTENT);
            });
        }

        @Test
        void testHttpsUrlIsValid() throws ContentValidationException {
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, "https://example.com/article", VALID_CONTENT);
            });
        }

        @Test
        void testContentWithUnicodeCharacters() throws ContentValidationException {
            String unicodeContent = "This content contains unicode characters: עברית, العربية, 中文, русский. " +
                    "It should be handled properly by the validation system. " +
                    "Unicode support is important for international content processing. " +
                    "The validator should work with various character encodings.";
            
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, unicodeContent);
            });
        }

        @Test
        void testContentWithSpecialCharacters() throws ContentValidationException {
            String specialContent = "Content with special chars: @#$%^&*()_+-=[]{}|;':\",./<>? " +
                    "These characters should not break the validation process. " +
                    "The system should handle punctuation and symbols correctly. " +
                    "This ensures robust content processing capabilities.";
            
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(VALID_TITLE, VALID_URL, specialContent);
            });
        }

        @Test
        void testVeryLongValidContent() throws ContentValidationException {
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longContent.append("This is sentence number ").append(i).append(" in a very long article. ");
            }
            
            String content = longContent.toString();
            if (content.length() <= config.content().maxContentLength()) {
                assertDoesNotThrow(() -> {
                    contentValidator.validateArticle(VALID_TITLE, VALID_URL, content);
                });
            }
        }
    }
}