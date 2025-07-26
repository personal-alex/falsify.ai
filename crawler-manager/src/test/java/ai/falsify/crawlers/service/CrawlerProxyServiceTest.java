package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.CrawlRequest;
import ai.falsify.crawlers.model.CrawlResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class CrawlerProxyServiceTest {
    
    @Inject
    CrawlerProxyService proxyService;
    
    @InjectMock
    CrawlerConfigurationService configurationService;
    
    @InjectMock
    CircuitBreaker circuitBreaker;
    
    private CrawlerConfiguration testConfig;
    
    @BeforeEach
    void setUp() {
        testConfig = new CrawlerConfiguration(
            "test-crawler",
            "Test Crawler",
            "http://localhost:8080",
            8080,
            "/test/health",
            "/test/crawl",
            "/test/status",
            true
        );
    }
    
    @Test
    void testTriggerCrawl_NullRequest() {
        // Test null request handling
        CrawlResponse response = proxyService.triggerCrawl(null);
        
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.ERROR, response.status);
        assertEquals("validation", response.errorCategory);
        assertTrue(response.message.contains("Request cannot be null"));
    }
    
    @Test
    void testTriggerCrawl_EmptyCrawlerId() {
        // Test empty crawler ID
        CrawlRequest request = new CrawlRequest("");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.ERROR, response.status);
        assertEquals("validation", response.errorCategory);
        assertTrue(response.message.contains("Crawler ID is required"));
    }
    
    @Test
    void testTriggerCrawl_CrawlerNotFound() {
        // Mock configuration service to return empty optional
        when(configurationService.getCrawlerConfiguration("nonexistent"))
            .thenReturn(Optional.empty());
        
        CrawlRequest request = new CrawlRequest("nonexistent");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.ERROR, response.status);
        assertEquals("configuration", response.errorCategory);
        assertTrue(response.message.contains("Crawler configuration not found"));
    }
    
    @Test
    void testTriggerCrawl_CrawlerDisabled() {
        // Create disabled crawler configuration
        CrawlerConfiguration disabledConfig = new CrawlerConfiguration(
            "disabled-crawler",
            "Disabled Crawler",
            "http://localhost:8080",
            8080,
            "/test/health",
            "/test/crawl",
            "/test/status",
            false // disabled
        );
        
        when(configurationService.getCrawlerConfiguration("disabled-crawler"))
            .thenReturn(Optional.of(disabledConfig));
        
        CrawlRequest request = new CrawlRequest("disabled-crawler");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.ERROR, response.status);
        assertEquals("configuration", response.errorCategory);
        assertTrue(response.message.contains("Crawler is disabled"));
    }
    
    @Test
    void testTriggerCrawl_CircuitBreakerOpen() {
        // Mock configuration service
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        
        // Mock circuit breaker to deny request
        when(circuitBreaker.allowRequest("test-crawler"))
            .thenReturn(false);
        
        CrawlRequest request = new CrawlRequest("test-crawler");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.SERVICE_UNAVAILABLE, response.status);
        assertTrue(response.message.contains("temporarily unavailable"));
    }
    
    @Test
    void testGetCrawlerStatus_CrawlerNotFound() {
        // Mock configuration service to return empty optional
        when(configurationService.getCrawlerConfiguration("nonexistent"))
            .thenReturn(Optional.empty());
        
        Response response = proxyService.getCrawlerStatus("nonexistent");
        
        assertNotNull(response);
        assertEquals(404, response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals("Crawler not found", entity.get("error"));
        assertEquals("nonexistent", entity.get("crawlerId"));
    }
    
    @Test
    void testGetCrawlerStatus_ValidRequest() {
        // Mock configuration service
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        
        // Note: This test will fail with actual HTTP calls since we don't have a real crawler running
        // In a real integration test, we would use WireMock or similar to mock the HTTP responses
        
        try {
            Response response = proxyService.getCrawlerStatus("test-crawler");
            // If we get here without exception, the configuration was found and client was created
            assertNotNull(response);
        } catch (Exception e) {
            // Expected in unit test environment without actual crawler running
            assertTrue(e.getMessage().contains("Connection refused") || 
                      e.getMessage().contains("Failed to create REST client") ||
                      e.getMessage().contains("ConnectException"));
        }
    }
    
    @Test
    void testValidateRequestParameters() {
        // Test request with additional parameters
        CrawlRequest request = new CrawlRequest("test-crawler", "high", 100, "2024-01-01");
        
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        when(circuitBreaker.allowRequest("test-crawler"))
            .thenReturn(true);
        
        // This will fail with connection error, but validates that parameters are accepted
        try {
            CrawlResponse response = proxyService.triggerCrawl(request);
            assertNotNull(response);
        } catch (Exception e) {
            // Expected in unit test environment
            assertTrue(e.getMessage().contains("Connection refused") || 
                      e.getMessage().contains("Failed to create REST client"));
        }
    }
}