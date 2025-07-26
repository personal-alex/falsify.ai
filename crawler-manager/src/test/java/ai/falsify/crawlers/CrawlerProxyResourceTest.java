package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlRequest;
import ai.falsify.crawlers.model.CrawlResponse;
import ai.falsify.crawlers.service.CrawlerProxyService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class CrawlerProxyResourceTest {
    
    @InjectMock
    CrawlerProxyService proxyService;
    
    @Test
    void testTriggerCrawl_Success() {
        // Mock successful response from proxy service
        CrawlResponse mockResponse = CrawlResponse.accepted(
            "test-crawler", 
            "req-123", 
            "crawl-456", 
            "/test/status"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
            .body(new CrawlRequest("test-crawler"))
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(202)
            .contentType(ContentType.JSON)
            .body("status", equalTo("ACCEPTED"))
            .body("crawlerId", equalTo("test-crawler"))
            .body("message", equalTo("Crawl started successfully"))
            .body("requestId", notNullValue())
            .body("crawlId", equalTo("crawl-456"))
            .body("statusEndpoint", equalTo("/test/status"))
            .body("timestamp", notNullValue());
    }
    
    @Test
    void testTriggerCrawl_WithoutRequestBody() {
        // Mock successful response from proxy service
        CrawlResponse mockResponse = CrawlResponse.accepted(
            "test-crawler", 
            "req-123", 
            "crawl-456", 
            "/test/status"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(202)
            .contentType(ContentType.JSON)
            .body("status", equalTo("ACCEPTED"))
            .body("crawlerId", equalTo("test-crawler"));
    }
    
    @Test
    void testTriggerCrawl_Conflict() {
        // Mock conflict response from proxy service
        CrawlResponse mockResponse = CrawlResponse.conflict(
            "test-crawler", 
            "req-123", 
            "RUNNING"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
            .body(new CrawlRequest("test-crawler"))
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(409)
            .contentType(ContentType.JSON)
            .body("status", equalTo("CONFLICT"))
            .body("crawlerId", equalTo("test-crawler"))
            .body("message", equalTo("Crawl already in progress"))
            .body("suggestion", notNullValue());
    }
    
    @Test
    void testTriggerCrawl_Error() {
        // Mock error response from proxy service
        CrawlResponse mockResponse = CrawlResponse.error(
            "test-crawler", 
            "req-123", 
            "Crawler configuration not found", 
            "configuration"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
            .body(new CrawlRequest("test-crawler"))
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", equalTo("ERROR"))
            .body("crawlerId", equalTo("test-crawler"))
            .body("message", equalTo("Crawler configuration not found"))
            .body("errorCategory", equalTo("configuration"))
            .body("suggestion", notNullValue());
    }
    
    @Test
    void testTriggerCrawl_ServiceUnavailable() {
        // Mock service unavailable response from proxy service
        CrawlResponse mockResponse = CrawlResponse.serviceUnavailable(
            "test-crawler", 
            "req-123", 
            "Crawler is temporarily unavailable"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
            .body(new CrawlRequest("test-crawler"))
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(503)
            .contentType(ContentType.JSON)
            .body("status", equalTo("SERVICE_UNAVAILABLE"))
            .body("crawlerId", equalTo("test-crawler"))
            .body("message", equalTo("Crawler is temporarily unavailable"))
            .body("suggestion", notNullValue());
    }
    
    @Test
    void testTriggerCrawl_EmptyCrawlerId() {
        given()
            .contentType(ContentType.JSON)
            .body(new CrawlRequest(""))
        .when()
            .post("/api/crawlers//crawl")
        .then()
            .statusCode(404); // Path not found due to empty crawler ID
    }
    
    @Test
    void testStartCrawl_ConvenienceEndpoint() {
        // Mock successful response from proxy service
        CrawlResponse mockResponse = CrawlResponse.accepted(
            "test-crawler", 
            "req-123", 
            "crawl-456", 
            "/test/status"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/crawlers/test-crawler/crawl/start")
        .then()
            .statusCode(202)
            .contentType(ContentType.JSON)
            .body("status", equalTo("ACCEPTED"))
            .body("crawlerId", equalTo("test-crawler"));
    }
    
    @Test
    void testGetCrawlerStatus_Success() {
        // Mock successful response from proxy service
        Response mockResponse = Response.ok(Map.of(
            "currentStatus", "IDLE",
            "crawlInProgress", false,
            "lastCrawlTime", "2024-01-01T10:00:00",
            "lastArticleCount", 42,
            "message", "Crawler is idle and ready"
        )).build();
        
        when(proxyService.getCrawlerStatus("test-crawler"))
            .thenReturn(mockResponse);
        
        given()
        .when()
            .get("/api/crawlers/test-crawler/status")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("currentStatus", equalTo("IDLE"))
            .body("crawlInProgress", equalTo(false))
            .body("lastArticleCount", equalTo(42))
            .body("message", equalTo("Crawler is idle and ready"));
    }
    
    @Test
    void testGetCrawlerStatus_NotFound() {
        // Mock not found response from proxy service
        Response mockResponse = Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of(
                "error", "Crawler not found",
                "crawlerId", "nonexistent"
            ))
            .build();
        
        when(proxyService.getCrawlerStatus("nonexistent"))
            .thenReturn(mockResponse);
        
        given()
        .when()
            .get("/api/crawlers/nonexistent/status")
        .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("error", equalTo("Crawler not found"))
            .body("crawlerId", equalTo("nonexistent"));
    }
    
    @Test
    void testGetCrawlerStatus_EmptyCrawlerId() {
        given()
        .when()
            .get("/api/crawlers//status")
        .then()
            .statusCode(404); // Path not found due to empty crawler ID
    }
    
    @Test
    void testTriggerCrawl_WithParameters() {
        // Mock successful response from proxy service
        CrawlResponse mockResponse = CrawlResponse.accepted(
            "test-crawler", 
            "req-123", 
            "crawl-456", 
            "/test/status"
        );
        
        when(proxyService.triggerCrawl(any(CrawlRequest.class)))
            .thenReturn(mockResponse);
        
        // Test with additional parameters
        CrawlRequest requestWithParams = new CrawlRequest(
            "test-crawler", 
            "high", 
            100, 
            "2024-01-01"
        );
        
        given()
            .contentType(ContentType.JSON)
            .body(requestWithParams)
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(202)
            .contentType(ContentType.JSON)
            .body("status", equalTo("ACCEPTED"))
            .body("crawlerId", equalTo("test-crawler"));
    }
    
    @Test
    void testTriggerCrawl_InvalidJson() {
        given()
            .contentType(ContentType.JSON)
            .body("{ invalid json }")
        .when()
            .post("/api/crawlers/test-crawler/crawl")
        .then()
            .statusCode(400); // Bad request due to invalid JSON
    }
}