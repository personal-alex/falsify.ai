package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlerMetrics;
import ai.falsify.crawlers.service.MetricsCollectorService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class MetricsResourceTest {
    
    @InjectMock
    MetricsCollectorService metricsCollectorService;
    
    private static final String TEST_CRAWLER_ID = "test-crawler";
    
    @BeforeEach
    void setUp() {
        reset(metricsCollectorService);
    }
    
    @Test
    void testGetAllMetrics_Success() {
        // Given
        CrawlerMetrics metrics1 = createTestMetrics("crawler1");
        CrawlerMetrics metrics2 = createTestMetrics("crawler2");
        Map<String, CrawlerMetrics> allMetrics = Map.of(
            "crawler1", metrics1,
            "crawler2", metrics2
        );
        when(metricsCollectorService.getAllMetrics()).thenReturn(allMetrics);
        
        // When & Then
        given()
            .when()
                .get("/api/metrics")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(2))
                .body("crawler1.crawlerId", equalTo("crawler1"))
                .body("crawler1.articlesProcessed", equalTo(100))
                .body("crawler2.crawlerId", equalTo("crawler2"))
                .body("crawler2.articlesProcessed", equalTo(100));
        
        verify(metricsCollectorService).getAllMetrics();
    }
    
    @Test
    void testGetAllMetrics_ServiceException() {
        // Given
        when(metricsCollectorService.getAllMetrics()).thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        given()
            .when()
                .get("/api/metrics")
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to retrieve metrics"))
                .body("message", equalTo("Service error"));
    }
    
    @Test
    void testGetCrawlerMetrics_Success() {
        // Given
        CrawlerMetrics metrics = createTestMetrics(TEST_CRAWLER_ID);
        when(metricsCollectorService.getMetrics(TEST_CRAWLER_ID)).thenReturn(metrics);
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/{crawlerId}", TEST_CRAWLER_ID)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("crawlerId", equalTo(TEST_CRAWLER_ID))
                .body("articlesProcessed", equalTo(200))
                .body("successRate", equalTo(95.0f))
                .body("errorCount", equalTo(5))
                .body("totalCrawlsExecuted", equalTo(10))
                .body("activeCrawls", equalTo(1));
        
        verify(metricsCollectorService).getMetrics(TEST_CRAWLER_ID);
    }
    
    @Test
    void testGetCrawlerMetrics_NotFound() {
        // Given
        when(metricsCollectorService.getMetrics(TEST_CRAWLER_ID)).thenReturn(null);
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/{crawlerId}", TEST_CRAWLER_ID)
            .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", containsString("Metrics not found"));
    }
    
    @Test
    void testGetCrawlerMetrics_EmptyCrawlerId() {
        // When & Then - Empty path parameter gets treated as a valid crawler ID
        given()
            .when()
                .get("/api/metrics/{crawlerId}", "")
            .then()
                .statusCode(200); // Empty string is treated as a valid crawler ID
    }
    
    @Test
    void testGetCrawlerMetrics_ServiceException() {
        // Given
        when(metricsCollectorService.getMetrics(TEST_CRAWLER_ID))
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/{crawlerId}", TEST_CRAWLER_ID)
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to retrieve metrics"))
                .body("message", equalTo("Database error"))
                .body("crawlerId", equalTo(TEST_CRAWLER_ID));
    }
    
    @Test
    void testGetCrawlerMetricsHistory_Success() {
        // Given
        CrawlerMetrics metrics = createTestMetricsWithTrends(TEST_CRAWLER_ID);
        when(metricsCollectorService.getMetrics(eq(TEST_CRAWLER_ID), any(Duration.class)))
            .thenReturn(metrics);
        
        // When & Then
        given()
            .queryParam("hours", 24)
            .when()
                .get("/api/metrics/{crawlerId}/history", TEST_CRAWLER_ID)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("crawlerId", equalTo(TEST_CRAWLER_ID))
                .body("trendsData", hasSize(2))
                .body("trendsData[0].articlesProcessed", equalTo(50))
                .body("trendsData[1].articlesProcessed", equalTo(75));
        
        verify(metricsCollectorService).getMetrics(eq(TEST_CRAWLER_ID), eq(Duration.ofHours(24)));
    }
    
    @Test
    void testGetCrawlerMetricsHistory_DefaultHours() {
        // Given
        CrawlerMetrics metrics = createTestMetrics(TEST_CRAWLER_ID);
        when(metricsCollectorService.getMetrics(eq(TEST_CRAWLER_ID), any(Duration.class)))
            .thenReturn(metrics);
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/{crawlerId}/history", TEST_CRAWLER_ID)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("crawlerId", equalTo(TEST_CRAWLER_ID));
        
        verify(metricsCollectorService).getMetrics(eq(TEST_CRAWLER_ID), eq(Duration.ofHours(24)));
    }
    
    @Test
    void testGetCrawlerMetricsHistory_InvalidHours() {
        // When & Then - Test negative hours
        given()
            .queryParam("hours", -1)
            .when()
                .get("/api/metrics/{crawlerId}/history", TEST_CRAWLER_ID)
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", containsString("Hours must be between 1 and 168"));
        
        // When & Then - Test too many hours
        given()
            .queryParam("hours", 200)
            .when()
                .get("/api/metrics/{crawlerId}/history", TEST_CRAWLER_ID)
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", containsString("Hours must be between 1 and 168"));
    }
    
    @Test
    void testTriggerMetricsCollection_Success() {
        // Given
        doNothing().when(metricsCollectorService).collectCrawlerMetrics(TEST_CRAWLER_ID);
        
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/metrics/{crawlerId}/collect", TEST_CRAWLER_ID)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Metrics collection triggered successfully"))
                .body("crawlerId", equalTo(TEST_CRAWLER_ID))
                .body("timestamp", notNullValue());
        
        verify(metricsCollectorService).collectCrawlerMetrics(TEST_CRAWLER_ID);
    }
    
    @Test
    void testTriggerMetricsCollection_ServiceException() {
        // Given
        doThrow(new RuntimeException("Collection failed"))
            .when(metricsCollectorService).collectCrawlerMetrics(TEST_CRAWLER_ID);
        
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/metrics/{crawlerId}/collect", TEST_CRAWLER_ID)
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to trigger metrics collection"))
                .body("message", equalTo("Collection failed"))
                .body("crawlerId", equalTo(TEST_CRAWLER_ID));
    }
    
    @Test
    void testTriggerAllMetricsCollection_Success() {
        // Given
        doNothing().when(metricsCollectorService).collectMetrics();
        
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/metrics/collect")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Metrics collection triggered for all crawlers"))
                .body("timestamp", notNullValue());
        
        verify(metricsCollectorService).collectMetrics();
    }
    
    @Test
    void testTriggerAllMetricsCollection_ServiceException() {
        // Given
        doThrow(new RuntimeException("Collection failed"))
            .when(metricsCollectorService).collectMetrics();
        
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/metrics/collect")
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to trigger metrics collection"))
                .body("message", equalTo("Collection failed"));
    }
    
    @Test
    void testClearMetricsCache_Success() {
        // Given
        doNothing().when(metricsCollectorService).clearMetricsCache(TEST_CRAWLER_ID);
        
        // When & Then
        given()
            .when()
                .delete("/api/metrics/{crawlerId}/cache", TEST_CRAWLER_ID)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Metrics cache cleared successfully"))
                .body("crawlerId", equalTo(TEST_CRAWLER_ID))
                .body("timestamp", notNullValue());
        
        verify(metricsCollectorService).clearMetricsCache(TEST_CRAWLER_ID);
    }
    
    @Test
    void testClearMetricsCache_ServiceException() {
        // Given
        doThrow(new RuntimeException("Cache clear failed"))
            .when(metricsCollectorService).clearMetricsCache(TEST_CRAWLER_ID);
        
        // When & Then
        given()
            .when()
                .delete("/api/metrics/{crawlerId}/cache", TEST_CRAWLER_ID)
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to clear metrics cache"))
                .body("message", equalTo("Cache clear failed"))
                .body("crawlerId", equalTo(TEST_CRAWLER_ID));
    }
    
    @Test
    void testGetMetricsStatus_Success() {
        // Given
        when(metricsCollectorService.getCachedMetricsCount()).thenReturn(5);
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/status")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("cachedMetricsCount", equalTo(5))
                .body("collectionEnabled", equalTo(true))
                .body("lastUpdate", notNullValue());
        
        verify(metricsCollectorService).getCachedMetricsCount();
    }
    
    @Test
    void testGetMetricsStatus_ServiceException() {
        // Given
        when(metricsCollectorService.getCachedMetricsCount())
            .thenThrow(new RuntimeException("Status error"));
        
        // When & Then
        given()
            .when()
                .get("/api/metrics/status")
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to get metrics status"))
                .body("message", equalTo("Status error"));
    }
    
    // Helper methods
    
    private CrawlerMetrics createTestMetrics(String crawlerId) {
        CrawlerMetrics metrics = new CrawlerMetrics(crawlerId);
        metrics.articlesProcessed = crawlerId.equals("test-crawler") ? 200 : 100;
        metrics.successRate = 95.0;
        metrics.averageProcessingTimeMs = 1500;
        metrics.errorCount = 5;
        metrics.totalCrawlsExecuted = 10;
        metrics.activeCrawls = 1;
        metrics.lastCrawlTime = Instant.now().minusSeconds(3600);
        return metrics;
    }
    
    private CrawlerMetrics createTestMetricsWithTrends(String crawlerId) {
        CrawlerMetrics metrics = createTestMetrics(crawlerId);
        
        // Add trend data
        Instant now = Instant.now();
        CrawlerMetrics.MetricPoint point1 = new CrawlerMetrics.MetricPoint(
            now.minusSeconds(3600), 50, 90.0, 1200, 2
        );
        CrawlerMetrics.MetricPoint point2 = new CrawlerMetrics.MetricPoint(
            now.minusSeconds(1800), 75, 92.0, 1300, 3
        );
        
        metrics.trendsData = java.util.List.of(point1, point2);
        return metrics;
    }
}