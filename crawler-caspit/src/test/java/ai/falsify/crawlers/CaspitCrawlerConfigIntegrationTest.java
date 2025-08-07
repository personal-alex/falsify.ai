package ai.falsify.crawlers;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify configuration injection works properly.
 * Uses test-specific configuration values from test/resources/application.properties.
 */
@QuarkusTest
class CaspitCrawlerConfigIntegrationTest {

    @Inject
    CaspitCrawlerConfig config;

    @Test
    void testConfigurationInjection() {
        assertNotNull(config, "Configuration should be injected");
        
        // Test basic configuration properties (using test values)
        assertEquals("https://test.example.com", config.baseUrl());
        assertEquals(10, config.maxPages());
        assertEquals(5000, config.pageLoadTimeout());
        
        // Test WebDriver configuration
        assertNotNull(config.webdriver());
        assertTrue(config.webdriver().headless());
        assertEquals(800, config.webdriver().windowWidth());
        assertEquals(600, config.webdriver().windowHeight());
        assertEquals(1, config.webdriver().implicitWait());
        assertEquals(3, config.webdriver().elementWait());
        assertEquals("Test Agent", config.webdriver().userAgent());
        
        // Test Crawling configuration
        assertNotNull(config.crawling());
        assertEquals(1000, config.crawling().pageDelay());
        assertEquals(2000, config.crawling().scrollDelay());
        assertEquals(5000, config.crawling().connectionTimeout());
        assertEquals(50, config.crawling().minContentLength());
        
        // Test Author configuration
        assertNotNull(config.author());
        assertEquals("Test Author", config.author().name());
        assertTrue(config.author().avatarUrl().isPresent());
        assertEquals("https://test.example.com/avatar.jpg", config.author().avatarUrl().get());
        assertEquals("Unknown Author", config.author().fallbackName());
    }

    @Test
    void testConfigurationValidation() {
        // Test that required properties are not null or empty
        assertNotNull(config.baseUrl());
        assertFalse(config.baseUrl().trim().isEmpty());
        
        // Test that numeric values are within expected ranges
        assertTrue(config.maxPages() > 0);
        assertTrue(config.pageLoadTimeout() >= 1000);
        assertTrue(config.webdriver().windowWidth() >= 800);
        assertTrue(config.webdriver().windowHeight() >= 600);
        assertTrue(config.webdriver().implicitWait() >= 1);
        assertTrue(config.webdriver().elementWait() >= 1);
        assertTrue(config.crawling().pageDelay() >= 500);
        assertTrue(config.crawling().scrollDelay() >= 1000);
        assertTrue(config.crawling().connectionTimeout() >= 5000);
        assertTrue(config.crawling().minContentLength() >= 50);
        
        // Test Author configuration validation
        assertNotNull(config.author().name());
        assertFalse(config.author().name().trim().isEmpty());
        assertNotNull(config.author().fallbackName());
        assertFalse(config.author().fallbackName().trim().isEmpty());
    }
}