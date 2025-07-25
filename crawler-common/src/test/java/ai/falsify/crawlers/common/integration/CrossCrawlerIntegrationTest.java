package ai.falsify.crawlers.common.integration;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that validate the common module works correctly
 * with different crawler implementations and patterns.
 */
@QuarkusTest
class CrossCrawlerIntegrationTest {

    @Inject
    CrawlerConfiguration config;

    @Inject
    ContentValidator contentValidator;

    @Inject
    RetryService retryService;

    @Inject
    DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        // Clear test data
        deduplicationService.clearProcessedUrls("test-drucker");
        deduplicationService.clearProcessedUrls("test-caspit");
    }

    @Test
    @DisplayName("Should handle different crawler patterns with shared services")
    void shouldHandleDifferentCrawlerPatternsWithSharedServices() {
        // Simulate drucker crawler pattern
        CrawlResult druckerResult = simulateDruckerCrawlerPattern();
        
        // Simulate caspit crawler pattern  
        CrawlResult caspitResult = simulateCaspitCrawlerPattern();

        // Verify both patterns work with shared services
        assertNotNull(druckerResult, "Drucker crawler result should not be null");
        assertNotNull(caspitResult, "Caspit crawler result should not be null");
        
        assertTrue(druckerResult.isSuccessful(), "Drucker crawler should be successful");
        assertTrue(caspitResult.isSuccessful(), "Caspit crawler should be successful");
        
        // Verify independent deduplication
        assertEquals(2, druckerResult.articlesProcessed(), "Drucker should process 2 articles");
        assertEquals(2, caspitResult.articlesProcessed(), "Caspit should process 2 articles");
    }

    @Test
    @DisplayName("Should maintain crawler isolation in deduplication")
    void shouldMaintainCrawlerIsolationInDeduplication() {
        String sharedUrl = "https://example.com/shared-article";
        
        // Both crawlers should be able to process the same URL independently
        assertTrue(deduplicationService.isNewUrl("drucker", sharedUrl), 
                  "URL should be new for drucker crawler");
        assertTrue(deduplicationService.isNewUrl("caspit", sharedUrl), 
                  "URL should be new for caspit crawler even after drucker processed it");
        
        // After processing, both should show as processed for their respective crawlers
        assertFalse(deduplicationService.isNewUrl("drucker", sharedUrl), 
                   "URL should not be new for drucker crawler on second check");
        assertFalse(deduplicationService.isNewUrl("caspit", sharedUrl), 
                   "URL should not be new for caspit crawler on second check");
    }

    @Test
    @DisplayName("Should provide consistent configuration across crawlers")
    void shouldProvideConsistentConfigurationAcrossCrawlers() {
        // Test that configuration is consistent and accessible
        assertNotNull(config, "Configuration should be available");
        
        // Test configuration values that should be consistent across crawlers
        assertTrue(config.content().enableContentValidation(), 
                  "Content validation should be enabled for all crawlers");
        assertTrue(config.retry().enableCircuitBreaker(), 
                  "Circuit breaker should be enabled for all crawlers");
        
        // Test that configuration provides reasonable defaults
        assertTrue(config.content().minContentLength() > 0, 
                  "Min content length should be positive");
        assertTrue(config.retry().maxAttempts() > 0, 
                  "Max retry attempts should be positive");
    }

    @Test
    @DisplayName("Should handle error scenarios consistently across crawlers")
    void shouldHandleErrorScenariosConsistentlyAcrossCrawlers() {
        // Test that both crawler patterns handle errors consistently
        
        // Test content validation errors
        String invalidContent = "x"; // Too short
        
        assertThrows(Exception.class, () -> {
            contentValidator.validateArticle("Title", "https://example.com/invalid", invalidContent);
        }, "Both crawlers should reject invalid content consistently");
        
        // Test retry behavior consistency
        int[] attemptCounts = {0, 0};
        
        for (int crawlerIndex = 0; crawlerIndex < 2; crawlerIndex++) {
            final int index = crawlerIndex;
            try {
                retryService.executeWithRetry(() -> {
                    attemptCounts[index]++;
                    throw new RuntimeException("Simulated failure");
                }, "test-crawler-" + index);
            } catch (Exception e) {
                // Expected to fail
            }
        }
        
        // Both crawlers should make the same number of retry attempts
        assertEquals(attemptCounts[0], attemptCounts[1], 
                    "Both crawlers should make the same number of retry attempts");
    }

    @Test
    @DisplayName("Should support concurrent crawler operations")
    void shouldSupportConcurrentCrawlerOperations() {
        // Test that multiple crawlers can operate concurrently without interference
        
        Thread druckerThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                String url = "https://drucker.example.com/article-" + i;
                deduplicationService.isNewUrl("drucker", url);
                try {
                    contentValidator.validateArticle(
                        "Drucker Article " + i, 
                        url, 
                        "This is drucker article content " + i + " with sufficient length for validation."
                    );
                } catch (Exception e) {
                    // Ignore validation errors for this test
                }
            }
        });
        
        Thread caspitThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                String url = "https://caspit.example.com/article-" + i;
                deduplicationService.isNewUrl("caspit", url);
                try {
                    contentValidator.validateArticle(
                        "Caspit Article " + i, 
                        url, 
                        "This is caspit article content " + i + " with sufficient length for validation."
                    );
                } catch (Exception e) {
                    // Ignore validation errors for this test
                }
            }
        });
        
        // Run both threads concurrently
        druckerThread.start();
        caspitThread.start();
        
        // Wait for completion
        assertDoesNotThrow(() -> {
            druckerThread.join(5000); // 5 second timeout
            caspitThread.join(5000);
        }, "Concurrent crawler operations should complete without errors");
        
        // Verify both crawlers processed their URLs
        assertEquals(5, deduplicationService.getProcessedUrlCount("drucker"), 
                    "Drucker should have processed 5 URLs");
        assertEquals(5, deduplicationService.getProcessedUrlCount("caspit"), 
                    "Caspit should have processed 5 URLs");
    }

    /**
     * Simulates the drucker crawler pattern using shared services
     */
    private CrawlResult simulateDruckerCrawlerPattern() {
        Instant startTime = Instant.now();
        List<Article> articles = List.of(
            new Article("Drucker Article 1", "https://drucker.example.com/article1", 
                       "This is the first drucker article with sufficient content for validation testing."),
            new Article("Drucker Article 2", "https://drucker.example.com/article2", 
                       "This is the second drucker article with sufficient content for validation testing.")
        );
        
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        
        for (Article article : articles) {
            try {
                // Check deduplication
                if (!deduplicationService.isNewUrl("test-drucker", article.url())) {
                    skipped++;
                    continue;
                }
                
                // Validate content
                try {
                    contentValidator.validateArticle(article.title(), article.url(), article.text());
                } catch (Exception e) {
                    throw new RuntimeException("Validation failed", e);
                }
                
                processed++;
            } catch (Exception e) {
                failed++;
            }
        }
        
        return new CrawlResult.Builder()
                .totalArticlesFound(articles.size())
                .articlesProcessed(processed)
                .articlesSkipped(skipped)
                .articlesFailed(failed)
                .processingTimeMs(100)
                .articles(articles)
                .startTime(startTime)
                .endTime(Instant.now())
                .crawlerSource("test-drucker")
                .errors(List.of())
                .build();
    }

    /**
     * Simulates the caspit crawler pattern using shared services
     */
    private CrawlResult simulateCaspitCrawlerPattern() {
        Instant startTime = Instant.now();
        List<Article> articles = List.of(
            new Article("Caspit Article 1", "https://caspit.example.com/article1", 
                       "This is the first caspit article with sufficient content for validation testing."),
            new Article("Caspit Article 2", "https://caspit.example.com/article2", 
                       "This is the second caspit article with sufficient content for validation testing.")
        );
        
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        
        for (Article article : articles) {
            try {
                // Check deduplication with retry
                boolean isNew = retryService.executeWithRetry(() -> 
                    deduplicationService.isNewUrl("test-caspit", article.url()), 
                    "dedup-check"
                );
                
                if (!isNew) {
                    skipped++;
                    continue;
                }
                
                // Validate content
                try {
                    contentValidator.validateArticle(article.title(), article.url(), article.text());
                } catch (Exception e) {
                    throw new RuntimeException("Validation failed", e);
                }
                
                processed++;
            } catch (Exception e) {
                failed++;
            }
        }
        
        return new CrawlResult.Builder()
                .totalArticlesFound(articles.size())
                .articlesProcessed(processed)
                .articlesSkipped(skipped)
                .articlesFailed(failed)
                .processingTimeMs(150)
                .articles(articles)
                .startTime(startTime)
                .endTime(Instant.now())
                .crawlerSource("test-caspit")
                .errors(List.of())
                .build();
    }
}