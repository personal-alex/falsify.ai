package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.JobRecord;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the JobTrackerService properly handles
 * entity state management and avoids the "detached entity" issue.
 */
@QuarkusTest
class JobTrackerServiceIntegrationTest {
    
    @Inject
    JobTrackerService jobTrackerService;
    
    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing data
        JobRecord.deleteAll();
    }
    
    @Test
    void testJobLifecycleWithoutDetachedEntityError() {
        // This test simulates the real scenario that was causing the detached entity error
        
        // Start a job
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        
        // Verify job was created
        assertNotNull(jobId);
        assertEquals(JobRecord.JobStatus.RUNNING, jobRecord.status);
        
        // Update progress multiple times (this was causing the detached entity error)
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 1, 0, 0, "Processing first article");
        });
        
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 2, 0, 0, "Processing second article");
        });
        
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 3, 1, 0, "Processing third article");
        });
        
        // Complete the job
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobCompletion(jobId, 5, 2, 1);
        });
        
        // Verify final state
        JobRecord finalRecord = JobRecord.findByJobId(jobId);
        assertNotNull(finalRecord);
        assertEquals(JobRecord.JobStatus.COMPLETED, finalRecord.status);
        assertEquals(5, finalRecord.articlesProcessed);
        assertEquals(2, finalRecord.articlesSkipped);
        assertEquals(1, finalRecord.articlesFailed);
        assertNotNull(finalRecord.endTime);
    }
    
    @Test
    void testJobFailureWithoutDetachedEntityError() {
        // Start a job
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-456");
        String jobId = jobRecord.jobId;
        
        // Update progress
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 2, 0, 1, "Processing with errors");
        });
        
        // Fail the job
        String errorMessage = "Network connection failed";
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobFailure(jobId, errorMessage);
        });
        
        // Verify final state
        JobRecord finalRecord = JobRecord.findByJobId(jobId);
        assertNotNull(finalRecord);
        assertEquals(JobRecord.JobStatus.FAILED, finalRecord.status);
        assertEquals(errorMessage, finalRecord.errorMessage);
        assertNotNull(finalRecord.endTime);
    }
    
    @Test
    void testJobCancellationWithoutDetachedEntityError() {
        // Start a job
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-789");
        String jobId = jobRecord.jobId;
        
        // Update progress
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 1, 0, 0, "Processing");
        });
        
        // Cancel the job
        assertDoesNotThrow(() -> {
            jobTrackerService.cancelJob(jobId);
        });
        
        // Verify final state
        JobRecord finalRecord = JobRecord.findByJobId(jobId);
        assertNotNull(finalRecord);
        assertEquals(JobRecord.JobStatus.CANCELLED, finalRecord.status);
        assertEquals("Cancelled", finalRecord.currentActivity);
        assertNotNull(finalRecord.endTime);
    }
}