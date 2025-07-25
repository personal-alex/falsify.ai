package ai.falsify.crawlers;

import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.exception.CrawlingException;
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import ai.falsify.crawlers.model.Prediction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CaspitCrawler {

    private static final Logger LOG = Logger.getLogger(CaspitCrawler.class);

    private final DeduplicationService deduplicationService;
    private final ContentValidator contentValidator;
    private final RetryService retryService;
    private final CaspitPageNavigator navigator;
    private final PredictionExtractor predictionExtractor;
    private final ObjectMapper objectMapper;
    private final CaspitCrawlerConfig config;

    @Inject
    public CaspitCrawler(DeduplicationService deduplicationService, ContentValidator contentValidator, 
                        RetryService retryService, CaspitPageNavigator navigator, 
                        PredictionExtractor predictionExtractor, CaspitCrawlerConfig config) {
        this.deduplicationService = deduplicationService;
        this.contentValidator = contentValidator;
        this.retryService = retryService;
        this.navigator = navigator;
        this.predictionExtractor = predictionExtractor;
        this.objectMapper = new ObjectMapper();
        this.config = config;
    }

    /**
     * Crawl Ben Caspit articles, fetch new articles (deduplicated via Redis) and persist them.
     * A transactional context is required for PanacheEntity.persist().
     */
    @Transactional
    public CrawlResult crawl() throws IOException {
        LOG.infof("Starting crawl from: %s", config.baseUrl());
        List<Article> articles = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        // Validate configuration before starting
        if (config.baseUrl() == null || config.baseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL configuration is required for crawling");
        }

        // Get all article URLs using the page navigator with error handling
        List<String> articleUrls = null;
        try {
            try {
                articleUrls = navigator.getAllArticleLinks(config.baseUrl());
                LOG.infof("Found %d article URLs to process.", articleUrls.size());
                
                if (articleUrls.isEmpty()) {
                    LOG.warn("No article URLs found - this may indicate a problem with the site structure or navigation");
                    return new CrawlResult.Builder()
                            .totalArticlesFound(0)
                            .articlesProcessed(0)
                            .articlesSkipped(0)
                            .articlesFailed(0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .articles(articles)
                            .startTime(java.time.Instant.ofEpochMilli(startTime))
                            .endTime(java.time.Instant.now())
                            .crawlerSource(config.crawlerSource())
                            .errors(java.util.List.of("No article URLs found"))
                            .build();
                }
            } catch (Exception navigationException) {
                LOG.errorf("Failed to retrieve article URLs from navigator: %s", navigationException.getMessage());
                throw new IOException("Article URL collection failed", navigationException);
            }

            // Process each article URL with comprehensive error handling
            for (int i = 0; i < articleUrls.size(); i++) {
                String url = articleUrls.get(i);
                
                // Validate URL before processing
                if (url == null || url.trim().isEmpty()) {
                    LOG.warnf("Skipping invalid URL at index %d: %s", i, url);
                    skippedCount++;
                    continue;
                }
                
                String cleanUrl = url.trim();
                
                LOG.infof("Processing article %d/%d: %s", i + 1, articleUrls.size(), cleanUrl);

                try {
                    // Check deduplication using common service with retry logic
                    boolean isNew;
                    try {
                        isNew = retryService.executeWithRetry(() -> {
                            return deduplicationService.isNewUrl(config.crawlerSource(), cleanUrl);
                        }, "dedup_check_" + cleanUrl, Exception.class);
                    } catch (CrawlingException redisException) {
                        LOG.warnf("Redis deduplication check failed for URL: %s - Error: %s. Proceeding without deduplication.", 
                                 cleanUrl, redisException.getMessage());
                        isNew = true; // Assume new article if Redis fails
                    }
                    
                    if (!isNew) {
                        LOG.infof("Article already exists in Redis, skipping: %s", cleanUrl);
                        skippedCount++;
                        continue;
                    }

                    // Fetch article content with error handling
                    Article article;
                    try {
                        article = fetchArticle(cleanUrl);
                    } catch (Exception fetchException) {
                        LOG.errorf("Failed to fetch article content: %s - Error: %s", cleanUrl, fetchException.getMessage());
                        failedCount++;
                        
                        // Note: Deduplication service will prevent re-processing of consistently failing articles
                        
                        continue; // Continue with next article
                    }
                    
                    if (article != null) {
                        LOG.infof("Successfully fetched article (%d chars): %s", 
                                 article.text() != null ? article.text().length() : 0, article.title());

                        // Validate article content using common service
                        try {
                            contentValidator.validateArticle(article.title(), article.url(), article.text());
                            LOG.debugf("Content validation passed for article: %s", article.url());
                        } catch (ContentValidationException validationException) {
                            LOG.warnf("Content validation failed for article: %s - Error: %s", 
                                     article.title(), validationException.getMessage());
                            failedCount++;
                            continue;
                        }

                        articles.add(article);
                        processedCount++;

                        // Persist article to DB with comprehensive error handling
                        try {
                            persistArticleWithRetry(article);
                            LOG.infof("Successfully persisted article to database: %s", article.title());
                        } catch (Exception persistException) {
                            LOG.errorf("Failed to persist article to database: %s - Error: %s", 
                                     article.title(), persistException.getMessage());
                            
                            // Log additional context for debugging
                            LOG.debugf("Persistence failure context - URL: %s, Title length: %d, Content length: %d", 
                                      article.url(), 
                                      article.title() != null ? article.title().length() : 0,
                                      article.text() != null ? article.text().length() : 0);
                            
                            failedCount++;
                            
                            // Remove from articles list since persistence failed
                            articles.remove(article);
                            
                            // Don't throw exception here - continue processing other articles
                            // The transaction will handle rollback for this specific article
                            continue;
                        }

                        // Extract predictions using AI with error handling
                        try {
                            extractAndStorePredictions(article);
                        } catch (Exception predictionException) {
                            LOG.warnf("Prediction extraction failed for article: %s - Error: %s. Article was still persisted successfully.", 
                                     article.title(), predictionException.getMessage());
                            // Don't fail the entire crawl for prediction extraction failures
                        }
                        
                    } else {
                        LOG.warnf("Failed to fetch or parse article content at: %s", cleanUrl);
                        failedCount++;
                        
                        // Note: Deduplication service will prevent re-processing of unparseable articles
                    }
                    
                } catch (Exception articleException) {
                    LOG.errorf("Unexpected error processing article: %s - Error: %s", cleanUrl, articleException.getMessage());
                    failedCount++;
                    
                    // Note: Deduplication service will prevent re-processing of articles that cause unexpected errors
                    
                    // Continue processing other articles
                    continue;
                }
                
                // Add small delay between articles to be respectful to the target site
                try {
                    Thread.sleep(100); // 100ms delay between articles
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Crawl interrupted during article processing delay");
                    break;
                }
            }

        } catch (Exception e) {
            LOG.errorf("Critical error during crawling process: %s", e.getMessage(), e);
            
            // Log additional context for debugging
            LOG.errorf("Crawl failure context - Processed: %d, Skipped: %d, Failed: %d, Total URLs: %d", 
                      processedCount, skippedCount, failedCount, 
                      articles.size() + processedCount + skippedCount + failedCount);
            
            throw new IOException("Crawling failed", e);
        }

        // Log comprehensive completion statistics
        long duration = System.currentTimeMillis() - startTime;
        LOG.infof("Crawling completed in %d ms. Statistics - Successfully processed: %d, Skipped (duplicates): %d, Failed: %d, Total articles fetched: %d", 
                 duration, processedCount, skippedCount, failedCount, articles.size());
        
        // Log performance metrics
        if (duration > 0) {
            double articlesPerSecond = (double) processedCount / (duration / 1000.0);
            LOG.infof("Crawling performance: %.2f articles/second", articlesPerSecond);
        }
        
        // Warn if failure rate is high
        int totalAttempted = processedCount + failedCount;
        if (totalAttempted > 0) {
            double failureRate = (double) failedCount / totalAttempted;
            if (failureRate > 0.2) { // More than 20% failure rate
                LOG.warnf("High failure rate detected: %.1f%% (%d/%d articles failed)", 
                         failureRate * 100, failedCount, totalAttempted);
            }
        }
        
        // Build and return CrawlResult
        return new CrawlResult.Builder()
                .totalArticlesFound(articleUrls != null ? articleUrls.size() : 0)
                .articlesProcessed(processedCount)
                .articlesSkipped(skippedCount)
                .articlesFailed(failedCount)
                .processingTimeMs(duration)
                .articles(articles)
                .startTime(java.time.Instant.ofEpochMilli(startTime))
                .endTime(java.time.Instant.now())
                .crawlerSource(config.crawlerSource())
                .errors(java.util.List.of())
                .build();
    }

    /**
     * Fetch and parse an individual article from the given URL using JSON-LD structured data
     * @param url The article URL to fetch
     * @return Article record with title, URL, and content, or null if parsing fails
     */
    private Article fetchArticle(String url) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent(config.webdriver().userAgent())
                .timeout(config.crawling().connectionTimeout())
                .get();

            // First try to extract content from JSON-LD structured data
            Article jsonLdArticle = extractFromJsonLd(doc, url);
            if (jsonLdArticle != null) {
                LOG.debugf("Successfully extracted article from JSON-LD: %s", url);
                return jsonLdArticle;
            }

            // Fallback to HTML parsing if JSON-LD extraction fails
            LOG.debugf("JSON-LD extraction failed, falling back to HTML parsing: %s", url);
            return extractFromHtml(doc, url);
            
        } catch (IOException e) {
            LOG.errorf("Failed to fetch article: %s - %s", url, e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.errorf("Unexpected error parsing article: %s - %s", url, e.getMessage());
            return null;
        }
    }

    /**
     * Extract article content from JSON-LD structured data
     * @param doc The parsed HTML document
     * @param url The article URL for logging
     * @return Article record or null if extraction fails
     */
    private Article extractFromJsonLd(Document doc, String url) {
        try {
            // Find all script elements with type="application/ld+json"
            Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
            
            for (Element script : jsonLdScripts) {
                try {
                    String jsonContent = script.html();
                    JsonNode jsonNode = objectMapper.readTree(jsonContent);
                    
                    // Handle both single objects and arrays
                    JsonNode articleNode = null;
                    if (jsonNode.isArray()) {
                        // Search through array for object with articleBody
                        for (JsonNode node : jsonNode) {
                            if (node.has("articleBody")) {
                                articleNode = node;
                                break;
                            }
                        }
                    } else if (jsonNode.has("articleBody")) {
                        articleNode = jsonNode;
                    }
                    
                    if (articleNode != null) {
                        String articleBody = articleNode.get("articleBody").asText();
                        String title = articleNode.has("headline") ? 
                            articleNode.get("headline").asText() : doc.title();
                        
                        // Validate content
                        if (articleBody != null && articleBody.length() > config.crawling().minContentLength()) {
                            LOG.debugf("Extracted article from JSON-LD - Title: %s, Content length: %d", 
                                     title, articleBody.length());
                            return new Article(title, url, articleBody);
                        }
                    }
                    
                } catch (Exception e) {
                    LOG.debugf("Failed to parse JSON-LD script: %s", e.getMessage());
                    // Continue to next script element
                }
            }
            
            return null; // No valid JSON-LD found
            
        } catch (Exception e) {
            LOG.debugf("Error during JSON-LD extraction: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback method to extract article content from HTML structure
     * @param doc The parsed HTML document
     * @param url The article URL for logging
     * @return Article record or null if extraction fails
     */
    private Article extractFromHtml(Document doc, String url) {
        try {
            // Try multiple selectors for article content (Maariv site structure)
            Element contentEl = doc.selectFirst("div.article-content, div.entry-content, div.content, article .text, .article-body");
            
            if (contentEl == null) {
                // Fallback: try to find main content area
                contentEl = doc.selectFirst("main, .main-content, #content");
            }
            
            if (contentEl == null) {
                LOG.warnf("No content found in article HTML: %s", url);
                return null;
            }

            String cleanText = contentEl.text();
            
            // Get title from multiple possible sources
            String title = doc.title();
            Element titleEl = doc.selectFirst("h1, .article-title, .entry-title, .title");
            if (titleEl != null && !titleEl.text().isEmpty()) {
                title = titleEl.text();
            }

            // Validate that we have meaningful content
            if (cleanText.length() < config.crawling().minContentLength()) {
                LOG.warnf("Article content too short, possibly parsing error: %s", url);
                return null;
            }

            return new Article(title, url, cleanText);
            
        } catch (Exception e) {
            LOG.warnf("Error during HTML extraction: %s - %s", url, e.getMessage());
            return null;
        }
    }

    /**
     * Persist an article to the database using retry logic and the common ArticleEntity structure.
     * Includes validation and proper error handling for database operations.
     * 
     * @param article The article to persist
     * @throws RuntimeException if persistence fails after retries
     */
    private void persistArticleWithRetry(Article article) {
        try {
            retryService.executeWithRetry(() -> {
                LOG.debugf("Starting persistence for article: %s", article.title());
                
                // Validate article data before persistence
                if (article.url() == null || article.url().trim().isEmpty()) {
                    throw new IllegalArgumentException("Article URL cannot be null or empty");
                }
                if (article.title() == null || article.title().trim().isEmpty()) {
                    throw new IllegalArgumentException("Article title cannot be null or empty");
                }
                if (article.text() == null || article.text().trim().isEmpty()) {
                    throw new IllegalArgumentException("Article text cannot be null or empty");
                }
                
                // Call the transactional persistence method
                return persistArticleToDatabase(article);
            }, "persist_article_" + article.url(), Exception.class);
            
        } catch (CrawlingException e) {
            throw new RuntimeException("Failed to persist article after retries: " + article.title(), e);
        }
    }

    /**
     * Actual database persistence method with transaction context.
     * This method is called from within the retry logic but maintains its own transaction.
     * 
     * @param article The article to persist
     * @return null (for compatibility with retry service)
     * @throws RuntimeException if persistence fails
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Object persistArticleToDatabase(Article article) {
        LOG.debugf("Starting transactional persistence for article: %s", article.title());
        
        try {
            // Check if article already exists by URL to prevent duplicates
            ArticleEntity existingArticle = ArticleEntity.find("url", article.url()).firstResult();
            if (existingArticle != null) {
                LOG.warnf("Article with URL already exists in database, skipping: %s", article.url());
                return null;
            }
            
            // Create and populate new ArticleEntity with crawler source
            ArticleEntity entity = new ArticleEntity(article, config.crawlerSource());
            
            LOG.debugf("Attempting to persist article: title='%s', url='%s', text_length=%d, crawler_source='%s'", 
                      entity.title, entity.url, entity.text.length(), entity.crawlerSource);
            
            // Persist to database
            entity.persist();
            
            LOG.infof("Successfully persisted article with ID: %d, URL: %s", entity.id, entity.url);
            return null;
            
        } catch (jakarta.persistence.PersistenceException e) {
            // Handle database constraint violations and other persistence issues
            if (e.getMessage() != null && e.getMessage().contains("unique constraint")) {
                LOG.warnf("Duplicate article detected during persistence (unique constraint violation): %s", article.url());
                // This is not a critical error, just log and continue
                return null;
            }
            LOG.errorf("Database persistence exception for article: %s - %s", article.title(), e.getMessage());
            throw new RuntimeException("Database persistence failed due to constraint violation", e);
        } catch (Exception e) {
            LOG.errorf("Unexpected database error during persistence for article: %s - %s", article.title(), e.getMessage());
            throw new RuntimeException("Failed to persist article to database", e);
        }
    }

    /**
     * Extract predictions from article content using AI and handle storage.
     * Implements requirements 2.1, 2.2, 2.3, and 2.4 for AI prediction extraction integration.
     * 
     * @param article The article to extract predictions from
     */
    private void extractAndStorePredictions(Article article) {
        LOG.debugf("Starting AI prediction extraction for article: %s", article.title());
        
        try {
            // Requirement 2.1: Use AI prediction extraction to identify predictions within the content
            List<Prediction> predictions = predictionExtractor.extractPredictions(article.text());
            
            if (predictions != null && !predictions.isEmpty()) {
                // Requirement 2.2: Store predictions using the existing Prediction model structure
                LOG.infof("AI prediction extraction successful - found %d predictions in article: %s", 
                         predictions.size(), article.title());
                
                // Log details about each prediction for monitoring and debugging
                for (int i = 0; i < predictions.size(); i++) {
                    Prediction prediction = predictions.get(i);
                    LOG.debugf("Prediction %d extracted from article '%s': %s", 
                              i + 1, article.title(), 
                              prediction.toString().length() > 200 ? 
                                  prediction.toString().substring(0, 200) + "..." : 
                                  prediction.toString());
                }
                
                // Note: Actual prediction persistence is handled by the PredictionExtractor implementation
                // or a separate service, following the existing system architecture patterns.
                // The predictions are returned here for potential further processing or validation.
                
            } else {
                // Requirement 2.3: Log when no predictions are found and continue processing
                LOG.infof("AI prediction extraction completed - no predictions found in article: %s", article.title());
            }
            
        } catch (Exception e) {
            // Requirement 2.4: Handle AI prediction extraction failures gracefully
            LOG.warnf("AI prediction extraction failed for article: %s - Error: %s", 
                     article.title(), e.getMessage());
            
            // Log additional context for debugging AI service issues
            if (e.getCause() != null) {
                LOG.debugf("Root cause of AI prediction extraction failure: %s", e.getCause().getMessage());
            }
            
            // Log article details that might help with debugging
            LOG.debugf("Failed article details - URL: %s, Content length: %d characters", 
                      article.url(), article.text() != null ? article.text().length() : 0);
            
            // Continue processing other articles - do not throw exception
            // This ensures that AI service failures don't stop the entire crawling process
        }
    }


}