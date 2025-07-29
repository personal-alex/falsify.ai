package ai.falsify.crawlers;

import ai.falsify.crawlers.model.JobRecord;
import ai.falsify.crawlers.service.JobTrackerService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@QuarkusTest
class JobCallbackResourceTest {

    @InjectMock
    JobTrackerService jobTrackerService;

    @Test
    void testUpdateJobProgress() {
        // Given
        String jobId = "test-job-123";
        Map<String, Object> progressData = Map.of(
            "articlesProcessed", 5,
            "articlesSkipped", 2,
            "articlesFailed", 1,
            "currentActivity", "Processing articles"
        );

        doNothing().when(jobTrackerService).trackJobProgress(anyString(), anyInt(), anyInt(), anyInt(), anyString());

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(progressData)
        .when()
            .put("/api/jobs/{jobId}/progress", jobId)
        .then()
            .statusCode(200)
            .body("message", equalTo("Progress updated successfully"))
            .body("jobId", equalTo(jobId));

        verify(jobTrackerService).trackJobProgress(jobId, 5, 2, 1, "Processing articles");
    }

    @Test
    void testUpdateJobProgressMissingFields() {
        // Given
        String jobId = "test-job-123";
        Map<String, Object> incompleteData = Map.of(
            "articlesProcessed", 5
            // Missing required fields
        );

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(incompleteData)
        .when()
            .put("/api/jobs/{jobId}/progress", jobId)
        .then()
            .statusCode(400)
            .body("error", equalTo("Missing required progress fields"));
    }

    @Test
    void testCompleteJob() {
        // Given
        String jobId = "test-job-123";
        Map<String, Object> completionData = Map.of(
            "articlesProcessed", 10,
            "articlesSkipped", 3,
            "articlesFailed", 2
        );

        doNothing().when(jobTrackerService).trackJobCompletion(anyString(), anyInt(), anyInt(), anyInt());

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(completionData)
        .when()
            .put("/api/jobs/{jobId}/complete", jobId)
        .then()
            .statusCode(200)
            .body("message", equalTo("Job completed successfully"))
            .body("jobId", equalTo(jobId));

        verify(jobTrackerService).trackJobCompletion(jobId, 10, 3, 2);
    }

    @Test
    void testCompleteJobMissingFields() {
        // Given
        String jobId = "test-job-123";
        Map<String, Object> incompleteData = Map.of(
            "articlesProcessed", 10
            // Missing required fields
        );

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(incompleteData)
        .when()
            .put("/api/jobs/{jobId}/complete", jobId)
        .then()
            .statusCode(400)
            .body("error", equalTo("Missing required completion fields"));
    }

    @Test
    void testFailJob() {
        // Given
        String jobId = "test-job-123";
        String errorMessage = "Network connection failed";
        Map<String, Object> failureData = Map.of(
            "errorMessage", errorMessage
        );

        doNothing().when(jobTrackerService).trackJobFailure(anyString(), anyString());

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(failureData)
        .when()
            .put("/api/jobs/{jobId}/fail", jobId)
        .then()
            .statusCode(200)
            .body("message", equalTo("Job failure recorded"))
            .body("jobId", equalTo(jobId));

        verify(jobTrackerService).trackJobFailure(jobId, errorMessage);
    }

    @Test
    void testFailJobWithoutErrorMessage() {
        // Given
        String jobId = "test-job-123";
        Map<String, Object> failureData = Map.of();

        doNothing().when(jobTrackerService).trackJobFailure(anyString(), anyString());

        // When/Then
        given()
            .contentType(ContentType.JSON)
            .body(failureData)
        .when()
            .put("/api/jobs/{jobId}/fail", jobId)
        .then()
            .statusCode(200)
            .body("message", equalTo("Job failure recorded"))
            .body("jobId", equalTo(jobId));

        verify(jobTrackerService).trackJobFailure(jobId, "Unknown error");
    }
}