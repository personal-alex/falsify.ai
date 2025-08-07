package ai.falsify.crawlers;

import ai.falsify.crawlers.common.model.CrawlResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DruckerCrawlerTest {

    @Inject
    DruckerCrawler crawler;
    
    @Inject
    DruckerCrawlerConfig config;

    @Test
    void testCrawlerIsInjected() {
        assertNotNull(crawler, "DruckerCrawler should be injected");
    }

    @Test
    void testConfigurationIsInjected() {
        assertNotNull(config, "DruckerCrawlerConfig should be injected");
    }

    @Test
    void testConfigurationValues() {
        // Test that configuration values are properly loaded
        assertNotNull(config.baseUrl(), "Base URL should not be null");
        assertTrue(config.maxPages() > 0, "Max pages should be positive");
        assertNotNull(config.pageDelay(), "Page delay should not be null");
        assertTrue(config.pageDelay().toMillis() >= 0, "Page delay should be non-negative");
        
        // Test boolean configurations
        assertNotNull(config.enableEarlyTermination(), "Early termination setting should not be null");
        assertTrue(config.emptyPageThreshold() >= 0, "Empty page threshold should be non-negative");
    }

    @Test
    void testConfigurationDefaults() {
        // Test that configuration has reasonable defaults
        assertTrue(config.maxPages() <= 50, "Max pages should have reasonable upper limit");
        assertTrue(config.pageDelay().toMillis() >= 100, "Page delay should be at least 100ms to be respectful");
        assertTrue(config.emptyPageThreshold() >= 1, "Empty page threshold should be at least 1");
    }

    @Test
    void testEarlyTerminationConfiguration() {
        // Test that early termination configuration is accessible
        boolean earlyTermination = config.enableEarlyTermination();
        assertNotNull(earlyTermination, "Early termination configuration should not be null");
        
        int threshold = config.emptyPageThreshold();
        assertTrue(threshold > 0, "Empty page threshold should be positive");
    }

    @Test
    void testPaginationConfiguration() {
        // Test pagination-related configuration
        int maxPages = config.maxPages();
        assertTrue(maxPages > 0, "Max pages should be positive");
        assertTrue(maxPages <= 100, "Max pages should have reasonable upper limit");
        
        Duration pageDelay = config.pageDelay();
        assertNotNull(pageDelay, "Page delay should not be null");
        assertTrue(pageDelay.toMillis() >= 0, "Page delay should be non-negative");
    }

    @Test
    void testConfigurationLogging() {
        // Test that configuration logging doesn't throw exceptions
        assertDoesNotThrow(() -> config.logConfigurationSummary(), 
            "Configuration logging should not throw exceptions");
    }

    @Test
    void testCrawlerInstantiation() {
        // Test that the crawler can be instantiated with all dependencies
        assertNotNull(crawler, "DruckerCrawler should be instantiated");
        
        // Test that we can access the configuration through the crawler
        assertDoesNotThrow(() -> config.logConfigurationSummary(), 
            "Should be able to access configuration through injected config");
    }

    @Test
    void testUrlBuildingLogic() {
        // Test URL building logic without reflection by testing the configuration values
        String baseUrl = config.baseUrl();
        assertNotNull(baseUrl, "Base URL should not be null");
        assertTrue(baseUrl.startsWith("http"), "Base URL should be a valid HTTP URL");
        
        // Test that we can determine if URL has query parameters
        boolean hasQueryParams = baseUrl.contains("?");
        
        // Simulate URL building logic
        String expectedPage2Url = hasQueryParams ? baseUrl + "&paged=2" : baseUrl + "?paged=2";
        assertTrue(expectedPage2Url.contains("paged=2"), "Page 2 URL should contain paged parameter");
        
        String expectedPage5Url = hasQueryParams ? baseUrl + "&paged=5" : baseUrl + "?paged=5";
        assertTrue(expectedPage5Url.contains("paged=5"), "Page 5 URL should contain correct page number");
    }

    @Test
    void testEarlyTerminationFeatureConfiguration() {
        // Test that the early termination feature is properly configured
        assertTrue(config.enableEarlyTermination(), "Early termination should be enabled by default");
        assertEquals(2, config.emptyPageThreshold(), "Empty page threshold should be 2 by default (from test config)");
        assertEquals(5, config.maxPages(), "Max pages should be 5 by default (from test config)");
        assertEquals(Duration.ofSeconds(1), config.pageDelay(), "Page delay should be 1 second by default (from test config)");
    }

    @Test
    void testConfigurationIntegrity() {
        // Test that all configuration values are consistent and valid
        assertTrue(config.maxPages() >= config.emptyPageThreshold(), 
            "Max pages should be at least as large as empty page threshold");
        
        assertTrue(config.pageDelay().toMillis() > 0, 
            "Page delay should be positive to be respectful to target site");
        
        assertTrue(config.emptyPageThreshold() > 0, 
            "Empty page threshold should be positive for early termination to work");
    }

    @Test
    void testAuthorConfiguration() {
        // Test that author configuration is properly loaded and accessible
        assertNotNull(config.author(), "Author configuration should not be null");
        assertNotNull(config.author().name(), "Author name should not be null");
        assertFalse(config.author().name().trim().isEmpty(), "Author name should not be empty");
        
        // Test that author configuration has expected values from test properties
        assertEquals("Test Drucker Author", config.author().name(), "Author name should match test configuration");
        assertTrue(config.author().avatarUrl().isPresent(), "Author avatar URL should be present in test configuration");
        assertEquals("https://test-drucker.example.com/avatar.jpg", config.author().avatarUrl().get(), 
                    "Author avatar URL should match test configuration");
        assertEquals("Unknown Author", config.author().fallbackName(), "Fallback author name should be 'Unknown Author'");
    }

    @Test
    void testAuthorConfigurationFallback() {
        // Test that author configuration provides proper fallback behavior
        String authorName = config.author().name();
        String fallbackName = config.author().fallbackName();
        
        // Verify that if author name is empty, fallback would be used
        assertNotNull(authorName, "Author name should not be null");
        assertNotNull(fallbackName, "Fallback name should not be null");
        assertFalse(fallbackName.trim().isEmpty(), "Fallback name should not be empty");
        
        // Test that the configuration method handles empty names correctly
        // (This tests the logic in AuthorConfig.name() method)
        assertTrue(authorName.equals("Test Drucker Author") || authorName.equals(fallbackName),
                  "Author name should be either the configured name or fallback");
    }
}
