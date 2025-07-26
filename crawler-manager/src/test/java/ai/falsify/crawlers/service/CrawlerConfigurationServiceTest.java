package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CrawlerConfigurationServiceTest {
    
    @Inject
    CrawlerConfigurationService configurationService;
    
    @BeforeEach
    void setUp() {
        // Refresh configurations before each test to ensure clean state
        configurationService.refreshConfigurations();
    }
    
    @Test
    void testDiscoverCrawlers() {
        List<CrawlerConfiguration> crawlers = configurationService.discoverCrawlers();
        
        assertNotNull(crawlers);
        assertFalse(crawlers.isEmpty());
        
        // Should find at least the caspit and drucker crawlers from application.properties
        assertTrue(crawlers.size() >= 2);
        
        // Verify crawler IDs are present
        Set<String> crawlerIds = new HashSet<>();
        for (CrawlerConfiguration crawler : crawlers) {
            crawlerIds.add(crawler.id);
        }
        
        assertTrue(crawlerIds.contains("caspit"));
        assertTrue(crawlerIds.contains("drucker"));
    }
    
    @Test
    void testGetCrawlerConfiguration() {
        Optional<CrawlerConfiguration> caspitConfig = configurationService.getCrawlerConfiguration("caspit");
        
        assertTrue(caspitConfig.isPresent());
        
        CrawlerConfiguration config = caspitConfig.get();
        assertEquals("caspit", config.id);
        assertEquals("Caspit Crawler", config.name);
        assertEquals("http://localhost:8080", config.baseUrl);
        assertEquals(Integer.valueOf(8080), config.port);
        assertEquals("/caspit/health", config.healthEndpoint);
        assertEquals("/caspit/crawl", config.crawlEndpoint);
        assertEquals("/caspit/status", config.statusEndpoint);
        assertTrue(config.enabled);
    }
    
    @Test
    void testGetCrawlerConfigurationNotFound() {
        Optional<CrawlerConfiguration> config = configurationService.getCrawlerConfiguration("nonexistent");
        
        assertFalse(config.isPresent());
    }
    
    @Test
    void testGetEnabledCrawlers() {
        List<CrawlerConfiguration> enabledCrawlers = configurationService.getEnabledCrawlers();
        
        assertNotNull(enabledCrawlers);
        assertFalse(enabledCrawlers.isEmpty());
        
        // All returned crawlers should be enabled
        for (CrawlerConfiguration crawler : enabledCrawlers) {
            assertTrue(crawler.enabled);
        }
    }
    
    @Test
    void testValidateValidConfiguration() {
        CrawlerConfiguration validConfig = new CrawlerConfiguration(
                "test-crawler",
                "Test Crawler",
                "http://localhost:9000",
                9000,
                "/health",
                "/crawl",
                "/status",
                true
        );
        
        Set<ConstraintViolation<CrawlerConfiguration>> violations = 
                configurationService.validateConfiguration(validConfig);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testValidateInvalidConfiguration() {
        CrawlerConfiguration invalidConfig = new CrawlerConfiguration(
                "", // Invalid: blank ID
                "", // Invalid: blank name
                "invalid-url", // Invalid: not HTTP/HTTPS
                -1, // Invalid: negative port
                "health", // Invalid: doesn't start with /
                "crawl", // Invalid: doesn't start with /
                "status", // Invalid: doesn't start with /
                null // Invalid: null enabled
        );
        
        Set<ConstraintViolation<CrawlerConfiguration>> violations = 
                configurationService.validateConfiguration(invalidConfig);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 7); // Should have multiple violations
    }
    
    @Test
    void testValidateConfigurationWithInvalidId() {
        CrawlerConfiguration invalidConfig = new CrawlerConfiguration(
                "invalid@id", // Invalid: contains special characters
                "Test Crawler",
                "http://localhost:9000",
                9000,
                "/health",
                "/crawl",
                "/status",
                true
        );
        
        Set<ConstraintViolation<CrawlerConfiguration>> violations = 
                configurationService.validateConfiguration(invalidConfig);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("alphanumeric characters")));
    }
    
    @Test
    void testConfigurationUrlMethods() {
        CrawlerConfiguration config = new CrawlerConfiguration(
                "test",
                "Test Crawler",
                "http://localhost:9000",
                9000,
                "/health",
                "/crawl",
                "/status",
                true
        );
        
        assertEquals("http://localhost:9000/health", config.getHealthUrl());
        assertEquals("http://localhost:9000/crawl", config.getCrawlUrl());
        assertEquals("http://localhost:9000/status", config.getStatusUrl());
    }
    
    @Test
    void testRefreshConfigurations() {
        // Get initial count
        List<CrawlerConfiguration> initialCrawlers = configurationService.discoverCrawlers();
        int initialCount = initialCrawlers.size();
        
        // Refresh configurations
        configurationService.refreshConfigurations();
        
        // Should still have the same configurations
        List<CrawlerConfiguration> refreshedCrawlers = configurationService.discoverCrawlers();
        assertEquals(initialCount, refreshedCrawlers.size());
    }
    
    @Test
    void testToString() {
        CrawlerConfiguration config = new CrawlerConfiguration(
                "test",
                "Test Crawler",
                "http://localhost:9000",
                9000,
                "/health",
                "/crawl",
                "/status",
                true
        );
        
        String toString = config.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test"));
        assertTrue(toString.contains("Test Crawler"));
        assertTrue(toString.contains("http://localhost:9000"));
    }
}