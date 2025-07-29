package ai.falsify.crawlers;

import ai.falsify.crawlers.model.JobRecord;
import ai.falsify.crawlers.model.JobStatus;
import ai.falsify.crawlers.service.JobTrackerService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for JobHistoryResource.
 */
@QuarkusTest

class JobHistoryResourceTest {
    
    @InjectMock
    JobTrackerService jobTrackerService;
    
    @BeforeEach
    void setUp() {
        reset(jobTrackerService);
    }
    
    @Test
    void testGetRecentJobs() {
        // Given
        String crawlerId = "test-crawler";
        List<JobStatus> mockJobs = Arrays.asList(
            createMockJobStatus("job-1", crawlerId, JobRecord.JobStatus.COMPLETED),
            createMockJobStatus("job-2", crawlerId, JobRecord.JobStatus.RUNNING),
            createMockJobStatus("job-3", crawlerId, JobRecord.JobStatus.FAILED)
        );
        
        when(jobTrackerService.getRecentJobs(crawlerId)).thenReturn(mockJobs);
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/crawler/{crawlerId}/recent", crawlerId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(3))
                .body("[0].jobId", equalTo("job-1"))
                .body("[0].crawlerId", equalTo(crawlerId))
                .body("[0].status", equalTo("COMPLETED"))
                .body("[1].jobId", equalTo("job-2"))
                .body("[1].status", equalTo("RUNNING"))
                .body("[2].jobId", equalTo("job-3"))
                .body("[2].status", equalTo("FAILED"));
        
        verify(jobTrackerService).getRecentJobs(crawlerId);
    }
    
    @Test
    void testGetRecentJobsServiceException() {
        // Given
        String crawlerId = "test-crawler";
        when(jobTrackerService.getRecentJobs(crawlerId))
            .thenThrow(new RuntimeException("Database error"));
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/crawler/{crawlerId}/recent", crawlerId)
            .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Failed to retrieve recent jobs"))
                .body("message", equalTo("Database error"));
    }
    
    @Test
    void testGetJobHistory() {
        // Given
        String crawlerId = "test-crawler";
        int page = 0;
        int size = 10;
        List<JobStatus> mockJobs = Arrays.asList(
            createMockJobStatus("job-1", crawlerId, JobRecord.JobStatus.COMPLETED),
            createMockJobStatus("job-2", crawlerId, JobRecord.JobStatus.FAILED)
        );
        
        when(jobTrackerService.getJobHistory(crawlerId, page, size)).thenReturn(mockJobs);
        
        // When/Then
        given()
            .queryParam("page", page)
            .queryParam("size", size)
            .when()
                .get("/api/jobs/crawler/{crawlerId}", crawlerId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(2))
                .body("[0].jobId", equalTo("job-1"))
                .body("[1].jobId", equalTo("job-2"));
        
        verify(jobTrackerService).getJobHistory(crawlerId, page, size);
    }
    
    @Test
    void testGetJobHistoryWithDefaults() {
        // Given
        String crawlerId = "test-crawler";
        when(jobTrackerService.getJobHistory(eq(crawlerId), eq(0), eq(10)))
            .thenReturn(Arrays.asList());
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/crawler/{crawlerId}", crawlerId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(0));
        
        verify(jobTrackerService).getJobHistory(crawlerId, 0, 10);
    }
    
    @Test
    void testGetJobHistoryInvalidPage() {
        // When/Then
        given()
            .queryParam("page", -1)
            .when()
                .get("/api/jobs/crawler/test-crawler")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Page number must be non-negative"));
        
        verify(jobTrackerService, never()).getJobHistory(any(), anyInt(), anyInt());
    }
    
    @Test
    void testGetJobHistoryInvalidSize() {
        // When/Then
        given()
            .queryParam("size", 0)
            .when()
                .get("/api/jobs/crawler/test-crawler")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Page size must be between 1 and 100"));
        
        given()
            .queryParam("size", 101)
            .when()
                .get("/api/jobs/crawler/test-crawler")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Page size must be between 1 and 100"));
        
        verify(jobTrackerService, never()).getJobHistory(any(), anyInt(), anyInt());
    }
    
    @Test
    void testGetJob() {
        // Given
        String jobId = "job-123";
        JobStatus mockJob = createMockJobStatus(jobId, "test-crawler", JobRecord.JobStatus.RUNNING);
        
        when(jobTrackerService.getJob(jobId)).thenReturn(Optional.of(mockJob));
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/{jobId}", jobId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jobId", equalTo(jobId))
                .body("crawlerId", equalTo("test-crawler"))
                .body("status", equalTo("RUNNING"));
        
        verify(jobTrackerService).getJob(jobId);
    }
    
    @Test
    void testGetJobNotFound() {
        // Given
        String jobId = "non-existent-job";
        when(jobTrackerService.getJob(jobId)).thenReturn(Optional.empty());
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/{jobId}", jobId)
            .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Job not found"))
                .body("jobId", equalTo(jobId));
        
        verify(jobTrackerService).getJob(jobId);
    }
    
    @Test
    void testGetRunningJobs() {
        // Given
        List<JobStatus> mockJobs = Arrays.asList(
            createMockJobStatus("job-1", "crawler-1", JobRecord.JobStatus.RUNNING),
            createMockJobStatus("job-2", "crawler-2", JobRecord.JobStatus.RUNNING)
        );
        
        when(jobTrackerService.getRunningJobs()).thenReturn(mockJobs);
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/running")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(2))
                .body("[0].status", equalTo("RUNNING"))
                .body("[1].status", equalTo("RUNNING"));
        
        verify(jobTrackerService).getRunningJobs();
    }
    
    @Test
    void testGetRunningJobsForCrawler() {
        // Given
        String crawlerId = "test-crawler";
        List<JobStatus> mockJobs = Arrays.asList(
            createMockJobStatus("job-1", crawlerId, JobRecord.JobStatus.RUNNING)
        );
        
        when(jobTrackerService.getRunningJobs(crawlerId)).thenReturn(mockJobs);
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/crawler/{crawlerId}/running", crawlerId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].jobId", equalTo("job-1"))
                .body("[0].crawlerId", equalTo(crawlerId))
                .body("[0].status", equalTo("RUNNING"));
        
        verify(jobTrackerService).getRunningJobs(crawlerId);
    }
    
    @Test
    void testCancelJob() {
        // Given
        String jobId = "job-123";
        JobStatus mockJob = createMockJobStatus(jobId, "test-crawler", JobRecord.JobStatus.RUNNING);
        
        when(jobTrackerService.getJob(jobId)).thenReturn(Optional.of(mockJob));
        doNothing().when(jobTrackerService).cancelJob(jobId);
        
        // When/Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/jobs/{jobId}/cancel", jobId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Job cancelled successfully"))
                .body("jobId", equalTo(jobId));
        
        verify(jobTrackerService).getJob(jobId);
        verify(jobTrackerService).cancelJob(jobId);
    }
    
    @Test
    void testCancelJobNotFound() {
        // Given
        String jobId = "non-existent-job";
        when(jobTrackerService.getJob(jobId)).thenReturn(Optional.empty());
        
        // When/Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/jobs/{jobId}/cancel", jobId)
            .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Job not found"))
                .body("jobId", equalTo(jobId));
        
        verify(jobTrackerService).getJob(jobId);
        verify(jobTrackerService, never()).cancelJob(any());
    }
    
    @Test
    void testCancelJobNotRunning() {
        // Given
        String jobId = "job-123";
        JobStatus mockJob = createMockJobStatus(jobId, "test-crawler", JobRecord.JobStatus.COMPLETED);
        
        when(jobTrackerService.getJob(jobId)).thenReturn(Optional.of(mockJob));
        
        // When/Then
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/jobs/{jobId}/cancel", jobId)
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Job is not running"))
                .body("jobId", equalTo(jobId))
                .body("status", equalTo("COMPLETED"));
        
        verify(jobTrackerService).getJob(jobId);
        verify(jobTrackerService, never()).cancelJob(any());
    }
    
    @Test
    void testHasRunningJobs() {
        // Given
        String crawlerId = "test-crawler";
        when(jobTrackerService.hasRunningJobs(crawlerId)).thenReturn(true);
        
        // When/Then
        given()
            .when()
                .get("/api/jobs/crawler/{crawlerId}/has-running", crawlerId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("crawlerId", equalTo(crawlerId))
                .body("hasRunningJobs", equalTo(true));
        
        verify(jobTrackerService).hasRunningJobs(crawlerId);
    }
    
    private JobStatus createMockJobStatus(String jobId, String crawlerId, JobRecord.JobStatus status) {
        JobStatus jobStatus = new JobStatus();
        jobStatus.jobId = jobId;
        jobStatus.crawlerId = crawlerId;
        jobStatus.status = status;
        jobStatus.startTime = Instant.now();
        jobStatus.articlesProcessed = 5;
        jobStatus.articlesSkipped = 1;
        jobStatus.articlesFailed = 0;
        jobStatus.currentActivity = "Test activity";
        jobStatus.lastUpdated = Instant.now();
        jobStatus.elapsedTimeMs = 1000L;
        jobStatus.successRate = 83.33;
        jobStatus.totalArticlesAttempted = 6;
        return jobStatus;
    }
}