package ai.falsify.crawlers;

import ai.falsify.crawlers.service.CrawlingMetrics;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DruckerCrawlerMetricsIntegrationTest {

    @Inject
    DruckerCrawler crawler;

    @Inject
    CrawlingMetrics metrics;

    @BeforeEach
    void setUp() {
        // Reset metrics before each test
        metrics.reset();
    }

    @Test
    void testCrawlerHasMetricsInjected() {
        // Verify that the crawler has the metrics service injected
        assertNotNull(crawler);
        assertNotNull(metrics);
        
        // Verify initial metrics state
        CrawlingMetrics.MetricsSummary summary = metrics.getSummary();
        assertEquals(0, summary.totalArticles());
        assertEquals(0, summary.successfulArticles());
        assertEquals(0, summary.failedArticles());
        assertEquals(0.0, summary.successRate());
    }

    @Test
    void testMetricsContextCreation() {
        // Test that we can create metrics contexts (simulating what DruckerCrawler does)
        String testUrl = "https://example.com/test-article";
        
        CrawlingMetrics.MetricsContext context = metrics.startArticleProcessing(testUrl);
        
        assertNotNull(context);
        assertEquals(testUrl, context.getUrl());
        assertNotNull(context.getStartTime());
    }

    @Test
    void testMetricsRecording() {
        // Test that we can record metrics (simulating what DruckerCrawler does)
        String testUrl = "https://example.com/test-article";
        
        // Start article processing
        CrawlingMetrics.MetricsContext context = metrics.startArticleProcessing(testUrl);
        
        // Simulate some operations
        metrics.recordNetworkOperation("listing_page_fetch", java.time.Duration.ofMillis(100));
        metrics.recordNetworkOperation("article_fetch", java.time.Duration.ofMillis(200));
        metrics.recordDatabaseOperation("redis_dedup_check", java.time.Duration.ofMillis(10));
        metrics.recordDatabaseOperation("article_persist", java.time.Duration.ofMillis(50));
        
        // Complete article processing
        metrics.recordArticleCompletion(context, true);
        
        // Verify metrics were recorded
        CrawlingMetrics.MetricsSummary summary = metrics.getSummary();
        assertEquals(1, summary.totalArticles());
        assertEquals(1, summary.successfulArticles());
        assertEquals(0, summary.failedArticles());
        assertEquals(100.0, summary.successRate());
        assertEquals(300, summary.totalNetworkTime()); // 100 + 200
        assertEquals(60, summary.totalDatabaseTime()); // 10 + 50
        
        // Verify operation-specific metrics
        assertTrue(summary.operationMetrics().containsKey("network_listing_page_fetch"));
        assertTrue(summary.operationMetrics().containsKey("network_article_fetch"));
        assertTrue(summary.operationMetrics().containsKey("database_redis_dedup_check"));
        assertTrue(summary.operationMetrics().containsKey("database_article_persist"));
    }

    @Test
    void testMetricsLoggingDoesNotThrow() {
        // Test that metrics logging works without throwing exceptions
        String testUrl = "https://example.com/test-article";
        
        CrawlingMetrics.MetricsContext context = metrics.startArticleProcessing(testUrl);
        metrics.recordNetworkOperation("test_operation", java.time.Duration.ofMillis(50));
        metrics.recordArticleCompletion(context, true);
        
        // This should not throw any exceptions
        assertDoesNotThrow(() -> metrics.logSummary());
    }
}