package ai.falsify;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class DruckerResourceTest {

    @Test
    public void testHealthEndpoint() {
        given()
                .when().get("/drucker/health")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("service", is("drucker-crawler"))
                .body("status", is("healthy"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testStatusEndpoint() {
        given()
                .when().get("/drucker/status")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("currentStatus", notNullValue())
                .body("crawlInProgress", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCrawlEndpointAcceptsRequest() {
        // Note: This test verifies the endpoint accepts the request
        // The actual crawl may fail due to configuration issues in test environment
        // but the REST endpoint should return 202 ACCEPTED
        given()
                .contentType(ContentType.JSON)
                .when().post("/drucker/crawl")
                .then()
                .statusCode(202) // ACCEPTED
                .contentType(ContentType.JSON)
                .body("status", is("accepted"))
                .body("message", is("Crawl started successfully"))
                .body("requestId", notNullValue())
                .body("crawlId", notNullValue())
                .body("timestamp", notNullValue())
                .body("statusEndpoint", is("/drucker/status"));
    }
}