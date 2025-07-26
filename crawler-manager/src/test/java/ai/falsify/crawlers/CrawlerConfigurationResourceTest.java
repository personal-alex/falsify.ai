package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CrawlerConfigurationResourceTest {
    
    @Test
    void testGetAllCrawlers() {
        given()
                .when().get("/api/crawlers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThanOrEqualTo(2))
                .body("id", hasItems("caspit", "drucker"))
                .body("name", hasItems("Caspit Crawler", "Drucker Crawler"))
                .body("enabled", everyItem(is(true)));
    }
    
    @Test
    void testGetEnabledCrawlers() {
        given()
                .when().get("/api/crawlers/enabled")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThanOrEqualTo(2))
                .body("enabled", everyItem(is(true)));
    }
    
    @Test
    void testGetCrawlerById() {
        given()
                .when().get("/api/crawlers/caspit")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo("caspit"))
                .body("name", equalTo("Caspit Crawler"))
                .body("baseUrl", equalTo("http://localhost:8080"))
                .body("port", equalTo(8080))
                .body("healthEndpoint", equalTo("/caspit/health"))
                .body("crawlEndpoint", equalTo("/caspit/crawl"))
                .body("statusEndpoint", equalTo("/caspit/status"))
                .body("enabled", equalTo(true));
    }
    
    @Test
    void testGetCrawlerByIdNotFound() {
        given()
                .when().get("/api/crawlers/nonexistent")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", containsString("Crawler not found: nonexistent"));
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
        
        given()
                .contentType(ContentType.JSON)
                .body(validConfig)
                .when().post("/api/crawlers/validate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("message", equalTo("Configuration is valid"))
                .body("errors", nullValue());
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
        
        given()
                .contentType(ContentType.JSON)
                .body(invalidConfig)
                .when().post("/api/crawlers/validate")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("message", equalTo("Configuration validation failed"))
                .body("errors", notNullValue())
                .body("errors.size()", greaterThan(0));
    }
    
    @Test
    void testRefreshConfigurations() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/api/crawlers/refresh")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("message", equalTo("Configurations refreshed successfully"))
                .body("crawlerCount", greaterThanOrEqualTo(2));
    }
    
    @Test
    void testGetCrawlerByIdDrucker() {
        given()
                .when().get("/api/crawlers/drucker")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo("drucker"))
                .body("name", equalTo("Drucker Crawler"))
                .body("baseUrl", equalTo("http://localhost:8081"))
                .body("port", equalTo(8081))
                .body("healthEndpoint", equalTo("/drucker/health"))
                .body("crawlEndpoint", equalTo("/drucker/crawl"))
                .body("statusEndpoint", equalTo("/drucker/status"))
                .body("enabled", equalTo(true));
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
        
        given()
                .contentType(ContentType.JSON)
                .body(invalidConfig)
                .when().post("/api/crawlers/validate")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("errors", hasItem(containsString("alphanumeric characters")));
    }
    
    @Test
    void testValidateConfigurationWithInvalidUrl() {
        CrawlerConfiguration invalidConfig = new CrawlerConfiguration(
                "test-crawler",
                "Test Crawler",
                "ftp://localhost:9000", // Invalid: not HTTP/HTTPS
                9000,
                "/health",
                "/crawl",
                "/status",
                true
        );
        
        given()
                .contentType(ContentType.JSON)
                .body(invalidConfig)
                .when().post("/api/crawlers/validate")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("errors", hasItem(containsString("valid HTTP or HTTPS URL")));
    }
}