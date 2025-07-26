package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.CrawlRequest;
import ai.falsify.crawlers.model.CrawlResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(CrawlerProxyServiceIT.TestProfileImpl.class)
class CrawlerProxyServiceIT {
    
    @Inject
    CrawlerProxyService proxyService;
    
    @InjectMock
    CrawlerConfigurationService configurationService;
    
    @InjectMock
    CircuitBreaker circuitBreaker;
    
    private WireMockServer wireMockServer;
    private CrawlerConfiguration testConfig;
    
    @BeforeEach
    void setUp() {
        // Start WireMock server on a random port
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        
        int port = wireMockServer.port();
        testConfig = new CrawlerConfiguration(
            "test-crawler",
            "Test Crawler",
            "http://localhost:" + port,
            port,
            "/test/health",
            "/test/crawl",
            "/test/status",
            true
        );
        
        // Configure WireMock client
        WireMock.configureFor("localhost", port);
    }
    
    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    @Test
    void testTriggerCrawl_Success() {
        // Mock successful crawl response
        stubFor(post(urlEqualTo("/test/crawl"))
            .willReturn(aResponse()
                .withStatus(202)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody("""
                    {
                        "status": "accepted",
                        "message": "Crawl started successfully",
                        "requestId": "crawl-123",
                        "crawlId": "test-crawl-456",
                        "timestamp": "2024-01-01T10:00:00",
                        "statusEndpoint": "/test/status"
                    }
                    """)));
        
        // Mock configuration and circuit breaker
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        when(circuitBreaker.allowRequest("test-crawler"))
            .thenReturn(true);
        
        // Execute test
        CrawlRequest request = new CrawlRequest("test-crawler");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        // Verify response
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.ACCEPTED, response.status);
        assertEquals("test-crawler", response.crawlerId);
        assertNotNull(response.requestId);
        assertNotNull(response.timestamp);
        
        // Verify WireMock received the request
        verify(postRequestedFor(urlEqualTo("/test/crawl")));
    }
    
    @Test
    void testTriggerCrawl_Conflict() {
        // Mock conflict response (crawl already in progress)
        stubFor(post(urlEqualTo("/test/crawl"))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody("""
                    {
                        "status": "conflict",
                        "message": "Crawl already in progress",
                        "currentStatus": "RUNNING",
                        "requestId": "crawl-123"
                    }
                    """)));
        
        // Mock configuration and circuit breaker
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        when(circuitBreaker.allowRequest("test-crawler"))
            .thenReturn(true);
        
        // Execute test
        CrawlRequest request = new CrawlRequest("test-crawler");
        CrawlResponse response = proxyService.triggerCrawl(request);
        
        // Verify response
        assertNotNull(response);
        assertEquals(CrawlResponse.Status.CONFLICT, response.status);
        assertEquals("test-crawler", response.crawlerId);
        assertTrue(response.message.contains("already in progress"));
        
        // Verify WireMock received the request
        verify(postRequestedFor(urlEqualTo("/test/crawl")));
    }
    
    @Test
    void testGetCrawlerStatus_Success() {
        // Mock successful status response
        stubFor(get(urlEqualTo("/test/status"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody("""
                    {
                        "currentStatus": "IDLE",
                        "crawlInProgress": false,
                        "lastCrawlTime": "2024-01-01T09:00:00",
                        "lastArticleCount": 42,
                        "message": "Crawler is idle and ready"
                    }
                    """)));
        
        // Mock configuration service
        when(configurationService.getCrawlerConfiguration("test-crawler"))
            .thenReturn(Optional.of(testConfig));
        
        // Execute test
        Response response = proxyService.getCrawlerStatus("test-crawler");
        
        // Verify response
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        
        // Verify WireMock received the request
        verify(getRequestedFor(urlEqualTo("/test/status")));
    }
    
    /**
     * Test profile to disable dev services for integration tests
     */
    public static class TestProfileImpl implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.redis.devservices.enabled", "true",
                "crawler.common.redis.enable-redis", "false"
            );
        }
    }
}