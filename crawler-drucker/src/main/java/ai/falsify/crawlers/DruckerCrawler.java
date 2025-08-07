package ai.falsify.crawlers;

import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.exception.CrawlingException;
import ai.falsify.crawlers.common.exception.NetworkException;
import ai.falsify.crawlers.common.exception.PersistenceException;
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.AuthorEntity;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import ai.falsify.crawlers.service.CrawlingMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DruckerCrawler {

    private static final Logger LOG = Logger.getLogger(DruckerCrawler.class);

    private final DeduplicationService deduplicationService;
    private final CrawlingMetrics metrics;
    private final RetryService retryService;
    private final ContentValidator contentValidator;
    private final DruckerCrawlerConfig config;
    
    @ConfigProperty(name = "crawler.source.name")
    String crawlerSourceName;

    // Metrics tracking for empty pages
    private int emptyPagesCount = 0;

    @Inject
    public DruckerCrawler(DeduplicationService deduplicationService, CrawlingMetrics metrics, RetryService retryService, ContentValidator contentValidator, DruckerCrawlerConfig config) {
        this.deduplicationService = deduplicationService;
        this.metrics = metrics;
        this.retryService = retryService;
        this.contentValidator = contentValidator;
        this.config = config;
    }

    /**
     * Crawl multiple pages with early termination support when no articles are found.
     * A transactional context is required for PanacheEntity.persist().
     */
    @Transactional
    public CrawlResult crawl() throws IOException {
        Instant crawlStart = Instant.now();
        LOG.infof("CRAWL_INITIATED: Starting crawl session at %s from URL: %s", crawlStart, config.baseUrl());
        
        // Log configuration summary
        config.logConfigurationSummary();
        
        // Reset metrics for this crawl session
        emptyPagesCount = 0;
        
        List<Article> articles = new ArrayList<>();
        int processedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        int totalArticlesFound = 0;
        boolean earlyTerminated = false;

        try {
            // Main pagination loop with early termination support
            int pageNumber = 1;
            int consecutiveEmptyPages = 0;
            
            while (pageNumber <= config.maxPages()) {
                LOG.infof("PAGE_PROCESSING: Starting to process page %d/%d", pageNumber, config.maxPages());
                
                // Build page URL (for Drucker, we'll use the base URL for page 1, and add pagination for subsequent pages)
                String pageUrl = buildPageUrl(pageNumber);
                
                try {
                    // Track network operation for fetching the listing page
                    LOG.debugf("NETWORK_REQUEST: Fetching listing page %d from %s", pageNumber, pageUrl);
                    Instant networkStart = Instant.now();
                    Document listingDoc = Jsoup.connect(pageUrl).get();
                    Duration networkDuration = Duration.between(networkStart, Instant.now());
                    metrics.recordNetworkOperation("listing_page_fetch", networkDuration);
                    LOG.debugf("NETWORK_RESPONSE: Listing page %d fetched in %d ms", pageNumber, networkDuration.toMillis());
                    
                    Elements articleElements = listingDoc.select("article");
                    int newArticlesOnPage = 0;
                    
                    LOG.infof("ARTICLES_DISCOVERED: Found %d article elements on page %d", articleElements.size(), pageNumber);
                    totalArticlesFound += articleElements.size();

                    // Process articles on this page
                    for (Element articleEl : articleElements) {
                        Element linkEl = articleEl.selectFirst("a");
                        if (linkEl != null) {
                            String url = linkEl.absUrl("href");

                            // Check for duplicates using deduplication service
                            if (!isNewUrl(url)) {
                                skippedCount++;
                                LOG.debugf("ARTICLE_SKIPPED: Article already exists in Redis (skipped=%d): %s", skippedCount, url);
                                continue;
                            }

                            processedCount++;
                            newArticlesOnPage++;
                            LOG.infof("ARTICLE_PROCESSING: Starting processing of article %d (page %d): %s", processedCount, pageNumber, url);
                            
                            // Start metrics tracking for this article
                            CrawlingMetrics.MetricsContext context = metrics.startArticleProcessing(url);
                            
                            try {
                                Article article = fetchArticleWithRetry(url);
                                if (article != null) {
                                    LOG.debugf("ARTICLE_FETCHED: Successfully fetched article (title_length=%d, content_length=%d): %s", 
                                            article.title().length(), article.text().length(), article.title());

                                    // Validate article content
                                    try {
                                        LOG.debugf("CONTENT_VALIDATION: Starting content validation for article: %s", url);
                                        Instant validationStart = Instant.now();
                                        contentValidator.validateArticle(article.title(), article.url(), article.text());
                                        Duration validationDuration = Duration.between(validationStart, Instant.now());
                                        metrics.recordDatabaseOperation("content_validation", validationDuration);
                                        LOG.debugf("CONTENT_VALIDATION_SUCCESS: Content validation passed in %d ms: %s", validationDuration.toMillis(), url);
                                        
                                        articles.add(article);
                                        
                                        // Persist article with retry logic
                                        try {
                                            persistArticleWithRetry(article);
                                            LOG.debugf("ARTICLE_SUCCESS: Article processing completed successfully: %s", url);
                                            // Record successful article processing
                                            metrics.recordArticleCompletion(context, true);
                                        } catch (PersistenceException e) {
                                            failedCount++;
                                            LOG.errorf(e, "PERSISTENCE_ERROR: Failed to persist article (failed=%d): %s", failedCount, url);
                                            // Remove from articles list since persistence failed
                                            articles.remove(article);
                                            metrics.recordArticleCompletion(context, false);
                                        }
                                    } catch (ContentValidationException e) {
                                        failedCount++;
                                        LOG.warnf(e, "CONTENT_VALIDATION_FAILED: Article content validation failed (failed=%d): %s - %s", 
                                                failedCount, url, e.getMessage());
                                        // Record failed article processing due to validation
                                        metrics.recordArticleCompletion(context, false);
                                    }
                                } else {
                                    failedCount++;
                                    LOG.warnf("ARTICLE_FAILED: Failed to fetch or parse article (failed=%d): %s", failedCount, url);
                                    // Record failed article processing
                                    metrics.recordArticleCompletion(context, false);
                                }
                            } catch (Exception e) {
                                failedCount++;
                                LOG.errorf(e, "ARTICLE_ERROR: Unexpected exception during article processing (failed=%d): %s", failedCount, url);
                                metrics.recordArticleCompletion(context, false);
                            }
                        } else {
                            LOG.debugf("PARSING_WARNING: No link found in article element on page %d, skipping", pageNumber);
                        }
                    }
                    
                    // Track empty pages for metrics and early termination
                    if (newArticlesOnPage == 0) {
                        emptyPagesCount++;
                        consecutiveEmptyPages++;
                        LOG.infof("EMPTY_PAGE_DETECTED: Page %d had no new articles (total empty pages: %d, consecutive: %d)", 
                                pageNumber, emptyPagesCount, consecutiveEmptyPages);
                    } else {
                        consecutiveEmptyPages = 0; // Reset consecutive counter
                        LOG.infof("PAGE_COMPLETED: Page %d processed successfully with %d new articles", pageNumber, newArticlesOnPage);
                    }
                    
                    // Check for early termination
                    if (config.enableEarlyTermination() && consecutiveEmptyPages >= config.emptyPageThreshold()) {
                        LOG.infof("EARLY_TERMINATION: Stopping crawl after %d consecutive empty pages (threshold: %d)", 
                                consecutiveEmptyPages, config.emptyPageThreshold());
                        LOG.infof("EARLY_TERMINATION: Total articles collected before termination: %d", articles.size());
                        earlyTerminated = true;
                        break;
                    }
                    
                    // Log progress
                    Duration elapsed = Duration.between(crawlStart, Instant.now());
                    LOG.infof("CRAWL_PROGRESS: Page %d completed - processed=%d, skipped=%d, failed=%d, elapsed=%d ms", 
                            pageNumber, processedCount, skippedCount, failedCount, elapsed.toMillis());
                    
                    // Wait before processing next page (except for the last page)
                    if (pageNumber < config.maxPages() && !earlyTerminated) {
                        try {
                            Thread.sleep(config.pageDelay().toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOG.warn("PAGE_DELAY_INTERRUPTED: Page delay interrupted, stopping crawl");
                            break;
                        }
                    }
                    
                } catch (IOException e) {
                    LOG.errorf(e, "PAGE_ERROR: Failed to fetch page %d: %s", pageNumber, pageUrl);
                    // Continue with next page instead of failing completely
                    consecutiveEmptyPages++;
                    if (config.enableEarlyTermination() && consecutiveEmptyPages >= config.emptyPageThreshold()) {
                        LOG.warnf("EARLY_TERMINATION_ERROR: Stopping crawl due to consecutive page errors (%d)", consecutiveEmptyPages);
                        earlyTerminated = true;
                        break;
                    }
                }
                
                pageNumber++;
            }

            Duration totalDuration = Duration.between(crawlStart, Instant.now());
            
            // Log completion status with detailed reason
            if (earlyTerminated) {
                LOG.infof("CRAWL_COMPLETED_EARLY: Crawling terminated early after %d pages due to empty page threshold", pageNumber - 1);
            } else if (pageNumber > config.maxPages()) {
                LOG.infof("CRAWL_COMPLETED_MAX: Crawling completed after reaching maximum pages limit (%d)", config.maxPages());
            } else {
                LOG.infof("CRAWL_COMPLETED: Crawling completed naturally");
            }
            
            LOG.infof("CRAWL_SUMMARY: Session completed in %d ms - processed=%d, skipped=%d, failed=%d, fetched=%d", 
                    totalDuration.toMillis(), processedCount, skippedCount, failedCount, articles.size());
            LOG.infof("PAGINATION_METRICS: %d pages processed, %d empty pages encountered", pageNumber - 1, emptyPagesCount);
            
            // Log metrics summary at the end of crawling
            metrics.logSummary();
            
            // Build and return CrawlResult
            return new CrawlResult.Builder()
                    .totalArticlesFound(totalArticlesFound)
                    .articlesProcessed(processedCount)
                    .articlesSkipped(skippedCount)
                    .articlesFailed(failedCount)
                    .processingTimeMs(totalDuration.toMillis())
                    .articles(articles)
                    .startTime(crawlStart)
                    .endTime(Instant.now())
                    .crawlerSource(crawlerSourceName)
                    .errors(List.of())
                    .build();
                    
        } catch (Exception e) {
            Duration totalDuration = Duration.between(crawlStart, Instant.now());
            LOG.errorf(e, "CRAWL_ERROR: Unexpected error during crawling session after %d ms - processed=%d, skipped=%d, failed=%d", 
                    totalDuration.toMillis(), processedCount, skippedCount, failedCount);
            throw new IOException("Crawling failed due to unexpected error", e);
        }
    }

    /**
     * Build the URL for a specific page number.
     * For Drucker site, pagination typically uses page parameter.
     */
    private String buildPageUrl(int pageNumber) {
        if (pageNumber == 1) {
            return config.baseUrl();
        }
        
        // For Drucker site, pagination typically uses &paged= parameter
        String separator = config.baseUrl().contains("?") ? "&" : "?";
        return config.baseUrl() + separator + "paged=" + pageNumber;
    }

    /**
     * Fetch article content with retry logic and comprehensive error handling
     */
    private Article fetchArticleWithRetry(String url) {
        try {
            return retryService.executeWithRetry(() -> {
                LOG.debugf("ARTICLE_FETCH: Starting to fetch article content from: %s", url);
                
                try {
                    // Track network operation for fetching individual article
                    Instant networkStart = Instant.now();
                    Document doc = Jsoup.connect(url).get();
                    Duration networkDuration = Duration.between(networkStart, Instant.now());
                    metrics.recordNetworkOperation("article_fetch", networkDuration);
                    LOG.debugf("NETWORK_RESPONSE: Article page fetched in %d ms: %s", networkDuration.toMillis(), url);
                    
                    Element contentEl = doc.selectFirst("div.entry-content");
                    if (contentEl == null) {
                        LOG.warnf("CONTENT_MISSING: No content element found in article: %s", url);
                        throw new RuntimeException(new NetworkException(
                            CrawlingException.ErrorCode.CONTENT_PARSING_FAILED,
                            "No content element found in article",
                            url
                        ));
                    }

                    String cleanText = contentEl.text();
                    String title = doc.title();
                    
                    LOG.debugf("CONTENT_EXTRACTED: Article content extracted (title_length=%d, content_length=%d): %s", 
                            title.length(), cleanText.length(), title);

                    return new Article(title, url, cleanText);
                    
                } catch (IOException e) {
                    LOG.debugf(e, "NETWORK_ERROR: Network error fetching article: %s", url);
                    throw new RuntimeException(NetworkException.connectionFailed(url, e));
                } catch (Exception e) {
                    LOG.debugf(e, "PARSING_ERROR: Error parsing article content: %s", url);
                    throw new RuntimeException(new NetworkException(
                        CrawlingException.ErrorCode.CONTENT_PARSING_FAILED,
                        "Failed to parse article content",
                        url,
                        e
                    ));
                }
            }, "fetch_article_" + url, IOException.class);
            
        } catch (CrawlingException e) {
            LOG.errorf(e, "ARTICLE_FETCH_FAILED: Failed to fetch article after retries: %s", url);
            return null;
        }
    }

    /**
     * Check if URL is new using deduplication service with retry logic
     */
    private boolean isNewUrl(String url) {
        try {
            return retryService.executeWithRetry(() -> {
                LOG.debugf("DEDUPLICATION_CHECK: Checking if article already processed: %s", url);
                
                try {
                    // Track Redis deduplication operation
                    Instant redisStart = Instant.now();
                    boolean isNew = deduplicationService.isNewUrl("drucker", url);
                    Duration redisDuration = Duration.between(redisStart, Instant.now());
                    metrics.recordDatabaseOperation("redis_dedup_check", redisDuration);
                    LOG.debugf("REDIS_OPERATION: Deduplication check completed in %d ms for %s", redisDuration.toMillis(), url);
                    
                    return isNew;
                    
                } catch (Exception e) {
                    LOG.debugf(e, "REDIS_ERROR: Error during deduplication check: %s", url);
                    throw new RuntimeException(new CrawlingException(
                        CrawlingException.ErrorCode.PERSISTENCE_CONNECTION_FAILED,
                        "Redis deduplication check failed",
                        url,
                        e
                    ));
                }
            }, "redis_dedup_" + url, Exception.class);
            
        } catch (CrawlingException e) {
            LOG.errorf(e, "DEDUPLICATION_FAILED: Failed to check duplication after retries, treating as new: %s", url);
            // If Redis is completely unavailable, treat as new article to avoid losing data
            return true;
        }
    }

    /**
     * Persist article to database with retry logic
     */
    private void persistArticleWithRetry(Article article) throws PersistenceException {
        try {
            retryService.executeWithRetry(() -> {
                LOG.debugf("DATABASE_PERSIST: Persisting article to database: %s", article.url());
                
                try {
                    // Track database persistence operation
                    Instant dbStart = Instant.now();
                    
                    // Find or create author entity using configured author information with error handling
                    AuthorEntity author;
                    try {
                        author = AuthorEntity.findOrCreate(
                            config.author().name(), 
                            config.author().avatarUrl().orElse(null)
                        );
                    } catch (Exception authorException) {
                        LOG.warnf("Failed to create or find author '%s', using unknown author fallback: %s", 
                                 config.author().name(), authorException.getMessage());
                        author = AuthorEntity.getUnknownAuthor();
                    }
                    
                    // Create ArticleEntity with author
                    ArticleEntity entity = new ArticleEntity(article, crawlerSourceName, author);
                    entity.persist();
                    Duration dbDuration = Duration.between(dbStart, Instant.now());
                    metrics.recordDatabaseOperation("article_persist", dbDuration);
                    LOG.debugf("DATABASE_OPERATION: Article persisted in %d ms: %s", dbDuration.toMillis(), article.url());
                    
                    return null; // Void operation
                    
                } catch (Exception e) {
                    LOG.debugf(e, "DATABASE_ERROR: Error persisting article: %s", article.url());
                    throw new RuntimeException(PersistenceException.saveFailed("ArticleEntity", article.url(), e));
                }
            }, "persist_article_" + article.url(), Exception.class);
            
        } catch (CrawlingException e) {
            if (e instanceof PersistenceException) {
                throw (PersistenceException) e;
            }
            throw new PersistenceException(
                CrawlingException.ErrorCode.PERSISTENCE_SAVE_FAILED,
                "Failed to persist article after retries",
                "persist",
                "ArticleEntity",
                article.url(),
                e
            );
        }
    }


}
