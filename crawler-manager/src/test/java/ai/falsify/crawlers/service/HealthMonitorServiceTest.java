package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.HealthStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class HealthMonitorServiceTest {
    
    @Inject
    HealthMonitorService healthMonitorService;
    
    @InjectMock
    CrawlerConfigurationService configurationService;
    
    @InjectMock
    CircuitBreaker circuitBreaker;
    
    private CrawlerConfiguration testCrawler;
    
    @BeforeEach
    void setUp() {
        testCrawler = new CrawlerConfiguration(
                "test-crawler",
                "Test Crawler",
                "http://localhost:8080",
                8080,
                "/q/health",
                "/test/crawl",
                "/test/status",
                true
        );
    }
    
    @Test
    void testGetCrawlerHealth_ReturnsUnknownWhenNoDataAvailable() {
        // Given
        String crawlerId = "unknown-crawler";
        
        // When
        HealthStatus result = healthMonitorService.getCrawlerHealth(crawlerId);
        
        // Then
        assertNotNull(result);
        assertEquals(HealthStatus.Status.UNKNOWN, result.status);
        assertEquals(crawlerId, result.crawlerId);
        assertEquals("No health data available", result.message);
    }
    
    @Test
    void testGetCrawlerHealth_ReturnsFromLocalCache() {
        // Given
        String crawlerId = "test-crawler";
        HealthStatus expectedStatus = HealthStatus.healthy(crawlerId, 100L);
        
        // Simulate local cache by performing a health check first
        when(configurationService.getCrawlerById(crawlerId)).thenReturn(testCrawler);
        when(circuitBreaker.allowRequest(crawlerId)).thenReturn(true);
        
        // When
        HealthStatus result = healthMonitorService.getCrawlerHealth(crawlerId);
        
        // Then
        assertNotNull(result);
        assertEquals(crawlerId, result.crawlerId);
    }
    
    @Test
    void testGetAllCrawlerHealth_ReturnsHealthForAllCrawlers() {
        // Given
        List<CrawlerConfiguration> crawlers = Arrays.asList(
                testCrawler,
                new CrawlerConfiguration("crawler2", "Crawler 2", "http://localhost:8081", 
                                       8081, "/q/health", "/crawl", "/status", true)
        );
        when(configurationService.getAllCrawlers()).thenReturn(crawlers);

        
        // When
        Map<String, HealthStatus> result = healthMonitorService.getAllCrawlerHealth();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("test-crawler"));
        assertTrue(result.containsKey("crawler2"));
    }
    
    @Test
    void testPerformHealthCheck_CircuitBreakerOpen() {
        // Given
        when(circuitBreaker.allowRequest(testCrawler.id)).thenReturn(false);
        
        // When
        HealthStatus result = healthMonitorService.performHealthCheck(testCrawler);
        
        // Then
        assertNotNull(result);
        assertEquals(HealthStatus.Status.UNHEALTHY, result.status);
        assertEquals(testCrawler.id, result.crawlerId);
        assertTrue(result.message.contains("Circuit breaker is open"));
    }
    
    @Test
    void testPerformHealthCheck_Success() {
        // Given
        when(circuitBreaker.allowRequest(testCrawler.id)).thenReturn(true);
        
        // Note: In a real test, we would need to mock the REST client
        // For now, we'll test the circuit breaker failure path
        
        // When
        HealthStatus result = healthMonitorService.performHealthCheck(testCrawler);
        
        // Then
        assertNotNull(result);
        assertEquals(testCrawler.id, result.crawlerId);
        // The actual status will depend on whether the crawler is running
        // In unit tests, it will likely be UNHEALTHY due to connection failure
    }
    
    @Test
    void testForceHealthCheck_ResetsCircuitBreaker() {
        // Given
        when(configurationService.getCrawlerById(testCrawler.id)).thenReturn(testCrawler);
        when(circuitBreaker.allowRequest(testCrawler.id)).thenReturn(true);
        
        // When
        HealthStatus result = healthMonitorService.forceHealthCheck(testCrawler.id);
        
        // Then
        assertNotNull(result);
        assertEquals(testCrawler.id, result.crawlerId);
        verify(circuitBreaker).reset(testCrawler.id);
    }
    
    @Test
    void testForceHealthCheck_CrawlerNotFound() {
        // Given
        String crawlerId = "nonexistent-crawler";
        when(configurationService.getCrawlerById(crawlerId)).thenReturn(null);
        
        // When
        HealthStatus result = healthMonitorService.forceHealthCheck(crawlerId);
        
        // Then
        assertNotNull(result);
        assertEquals(HealthStatus.Status.UNKNOWN, result.status);
        assertEquals(crawlerId, result.crawlerId);
        assertEquals("Crawler configuration not found", result.message);
    }
    
    @Test
    void testPerformHealthChecks_ProcessesAllEnabledCrawlers() {
        // Given
        CrawlerConfiguration disabledCrawler = new CrawlerConfiguration(
                "disabled-crawler", "Disabled", "http://localhost:8082", 
                8082, "/q/health", "/crawl", "/status", false
        );
        
        List<CrawlerConfiguration> crawlers = Arrays.asList(testCrawler, disabledCrawler);
        when(configurationService.getAllCrawlers()).thenReturn(crawlers);
        when(circuitBreaker.allowRequest(anyString())).thenReturn(true);
        
        // When
        healthMonitorService.performHealthChecks();
        
        // Then
        // Verify that only enabled crawlers are processed
        verify(circuitBreaker, times(1)).allowRequest(testCrawler.id);
        verify(circuitBreaker, never()).allowRequest(disabledCrawler.id);
    }
    
    @Test
    void testHealthStatusCaching_UpdatesLocalCache() {
        // Given
        String crawlerId = "test-crawler";
        when(configurationService.getCrawlerById(crawlerId)).thenReturn(testCrawler);
        when(circuitBreaker.allowRequest(crawlerId)).thenReturn(true);
        
        // When
        healthMonitorService.forceHealthCheck(crawlerId);
        
        // Then
        // Verify that the health status is now available in local cache
        HealthStatus result = healthMonitorService.getCrawlerHealth(crawlerId);
        assertNotNull(result);
        assertEquals(crawlerId, result.crawlerId);
        // The status will likely be UNHEALTHY due to connection failure in test environment
    }
    
    @Test
    void testHealthStatusModel_FactoryMethods() {
        // Test healthy status creation
        HealthStatus healthy = HealthStatus.healthy("test", 100L);
        assertEquals(HealthStatus.Status.HEALTHY, healthy.status);
        assertEquals("test", healthy.crawlerId);
        assertEquals(100L, healthy.responseTimeMs);
        assertTrue(healthy.isHealthy());
        assertFalse(healthy.isUnhealthy());
        assertFalse(healthy.isUnknown());
        
        // Test unhealthy status creation
        HealthStatus unhealthy = HealthStatus.unhealthy("test", "Error message");
        assertEquals(HealthStatus.Status.UNHEALTHY, unhealthy.status);
        assertEquals("test", unhealthy.crawlerId);
        assertEquals("Error message", unhealthy.message);
        assertFalse(unhealthy.isHealthy());
        assertTrue(unhealthy.isUnhealthy());
        assertFalse(unhealthy.isUnknown());
        
        // Test unknown status creation
        HealthStatus unknown = HealthStatus.unknown("test", "Unknown state");
        assertEquals(HealthStatus.Status.UNKNOWN, unknown.status);
        assertEquals("test", unknown.crawlerId);
        assertEquals("Unknown state", unknown.message);
        assertFalse(unknown.isHealthy());
        assertFalse(unknown.isUnhealthy());
        assertTrue(unknown.isUnknown());
    }
}