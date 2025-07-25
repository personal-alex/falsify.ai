package ai.falsify.crawlers;

import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.service.CrawlingMetrics;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that ContentValidator and CrawlingMetrics
 * are properly integrated into the DruckerCrawler.
 */
@QuarkusTest
class DruckerCrawlerIntegrationTest {

    @Inject
    DruckerCrawler crawler;

    @Inject
    ContentValidator contentValidator;

    @Inject
    CrawlingMetrics metrics;

    @Test
    void testDependenciesAreInjected() {
        // Verify that all dependencies are properly injected
        assertNotNull(crawler, "DruckerCrawler should be injected");
        assertNotNull(contentValidator, "ContentValidator should be injected");
        assertNotNull(metrics, "CrawlingMetrics should be injected");
    }

    @Test
    void testContentValidatorConfiguration() {
        // Test that content validator is properly configured
        assertNotNull(contentValidator);
        
        // Test cache operations
        int initialSize = contentValidator.getContentCacheSize();
        contentValidator.clearContentCache();
        assertEquals(0, contentValidator.getContentCacheSize());
    }

    @Test
    void testCrawlingMetricsConfiguration() {
        // Test that metrics service is properly configured
        assertNotNull(metrics);
        
        // Test metrics reset
        metrics.reset();
        
        // Verify metrics can track operations
        CrawlingMetrics.MetricsContext context = metrics.startArticleProcessing("https://test.com");
        assertNotNull(context);
        
        metrics.recordArticleCompletion(context, true);
        
        // Verify metrics summary can be generated
        assertDoesNotThrow(() -> metrics.logSummary());
    }
}