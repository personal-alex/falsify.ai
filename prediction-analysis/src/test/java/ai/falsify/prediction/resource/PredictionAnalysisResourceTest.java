package ai.falsify.prediction.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PredictionAnalysisResourceTest {

    @Test
    public void testGetSystemStatus() {
        given()
            .when()
                .get("/api/prediction-analysis/status")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("runningJobs", notNullValue())
                .body("maxConcurrentJobs", notNullValue())
                .body("availableSlots", notNullValue());
    }

    @Test
    public void testGetJobHistory() {
        given()
            .when()
                .get("/api/prediction-analysis/history")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(List.class));
    }

    @Test
    public void testStartAnalysisWithEmptyArticleIds() {
        Map<String, Object> request = Map.of(
            "articleIds", List.of(),
            "analysisType", "mock"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
                .post("/api/prediction-analysis/start")
            .then()
                .statusCode(400)
                .body("error", containsString("Article IDs are required"));
    }

    @Test
    public void testGetNonExistentJob() {
        given()
            .when()
                .get("/api/prediction-analysis/jobs/non-existent-job-id")
            .then()
                .statusCode(404)
                .body("error", containsString("Job not found"));
    }
}