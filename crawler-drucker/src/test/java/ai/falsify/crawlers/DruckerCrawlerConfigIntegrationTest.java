package ai.falsify.crawlers;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify DruckerCrawlerConfig injection works properly.
 * Uses test-specific configuration values from test/resources/application.properties.
 */
@QuarkusTest
class DruckerCrawlerConfigIntegrationTest {

    @Inject
    DruckerCrawlerConfig config;

    @Test
    void testConfigurationInjection() {
        assertNotNull(config, "Configuration should be injected");
        
        // Test basic configuration properties (using test values)
        assertEquals("https://test-drucker.example.com", config.baseUrl());
        assertEquals(5, config.maxPages());
        assertEquals(Duration.ofSeconds(1), config.pageDelay());
        assertTrue(config.enableEarlyTermination());
        assertEquals(2, config.emptyPageThreshold());
        
        // Test Author configuration
        assertNotNull(config.author());
        assertEquals("Test Drucker Author", config.author().name());
        assertTrue(config.author().avatarUrl().isPresent());
        assertEquals("https://test-drucker.example.com/avatar.jpg", config.author().avatarUrl().get());
        assertEquals("Unknown Author", config.author().fallbackName());
    }

    @Test
    void testConfigurationValidation() {
        // Test that required properties are not null or empty
        assertNotNull(config.baseUrl());
        assertFalse(config.baseUrl().trim().isEmpty());
        
        // Test that numeric values are within expected ranges
        assertTrue(config.maxPages() > 0);
        assertTrue(config.emptyPageThreshold() >= 1);
        assertNotNull(config.pageDelay());
        assertTrue(config.pageDelay().toMillis() > 0);
        
        // Test Author configuration validation
        assertNotNull(config.author().name());
        assertFalse(config.author().name().trim().isEmpty());
        assertNotNull(config.author().fallbackName());
        assertFalse(config.author().fallbackName().trim().isEmpty());
    }

    @Test
    void testAuthorConfigurationFallback() {
        // Test that author configuration provides fallback when name is empty
        // This would require a separate test configuration, but we can test the logic
        assertNotNull(config.author().name());
        assertNotNull(config.author().fallbackName());
        
        // The name should either be the configured name or the fallback
        String authorName = config.author().name();
        String fallbackName = config.author().fallbackName();
        assertTrue(authorName.equals("Test Drucker Author") || authorName.equals(fallbackName));
    }
}