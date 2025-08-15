package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BatchJobStatusTest {

    @Test
    void testValidConstruction() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        
        assertEquals("job-123", status.jobId());
        assertEquals("batch-456", status.batchId());
        assertEquals(BatchState.PROCESSING, status.state());
        assertEquals(10, status.totalRequests());
        assertEquals(5, status.completedRequests());
        assertEquals(2, status.failedRequests());
        assertEquals(now, status.createdAt());
        assertEquals(now, status.updatedAt());
        assertNull(status.errorMessage());
    }

    @Test
    void testValidationJobIdRequired() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus(null, "batch-456", BatchState.PROCESSING, 10, 5, 2, now, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("", "batch-456", BatchState.PROCESSING, 10, 5, 2, now, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("   ", "batch-456", BatchState.PROCESSING, 10, 5, 2, now, now, null)
        );
    }

    @Test
    void testValidationBatchIdRequired() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", null, BatchState.PROCESSING, 10, 5, 2, now, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "", BatchState.PROCESSING, 10, 5, 2, now, now, null)
        );
    }

    @Test
    void testValidationStateRequired() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", null, 10, 5, 2, now, now, null)
        );
    }

    @Test
    void testValidationNegativeValues() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, -1, 5, 2, now, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, -1, 2, now, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, 5, -1, now, now, null)
        );
    }

    @Test
    void testValidationRequestCounts() {
        Instant now = Instant.now();
        
        // Completed + failed cannot exceed total
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, 8, 5, now, now, null)
        );
    }

    @Test
    void testValidationTimestamps() {
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(60);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, 5, 2, null, now, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, 5, 2, now, null, null)
        );
        
        // Updated cannot be before created
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchJobStatus("job-123", "batch-456", BatchState.PROCESSING, 10, 5, 2, now, earlier, null)
        );
    }

    @Test
    void testSubmittedFactory() {
        BatchJobStatus status = BatchJobStatus.submitted("job-123", "batch-456", 10);
        
        assertEquals("job-123", status.jobId());
        assertEquals("batch-456", status.batchId());
        assertEquals(BatchState.SUBMITTED, status.state());
        assertEquals(10, status.totalRequests());
        assertEquals(0, status.completedRequests());
        assertEquals(0, status.failedRequests());
        assertNotNull(status.createdAt());
        assertNotNull(status.updatedAt());
        assertNull(status.errorMessage());
    }

    @Test
    void testProcessingFactory() {
        BatchJobStatus status = BatchJobStatus.processing("job-123", "batch-456", 10, 5, 2);
        
        assertEquals(BatchState.PROCESSING, status.state());
        assertEquals(5, status.completedRequests());
        assertEquals(2, status.failedRequests());
    }

    @Test
    void testCompletedFactory() {
        BatchJobStatus status = BatchJobStatus.completed("job-123", "batch-456", 10, 8, 2);
        
        assertEquals(BatchState.COMPLETED, status.state());
        assertEquals(8, status.completedRequests());
        assertEquals(2, status.failedRequests());
    }

    @Test
    void testFailedFactory() {
        BatchJobStatus status = BatchJobStatus.failed("job-123", "batch-456", 10, 5, 5, "API Error");
        
        assertEquals(BatchState.FAILED, status.state());
        assertEquals("API Error", status.errorMessage());
    }

    @Test
    void testWithProgress() {
        Instant now = Instant.now();
        BatchJobStatus original = new BatchJobStatus(
            "job-123", "batch-456", BatchState.SUBMITTED, 
            10, 0, 0, now, now, null
        );
        
        BatchJobStatus updated = original.withProgress(5, 2);
        
        assertEquals(BatchState.PROCESSING, updated.state());
        assertEquals(5, updated.completedRequests());
        assertEquals(2, updated.failedRequests());
        assertTrue(updated.updatedAt().isAfter(original.updatedAt()));
    }

    @Test
    void testWithProgressCompleted() {
        Instant now = Instant.now();
        BatchJobStatus original = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        
        BatchJobStatus updated = original.withProgress(8, 2);
        
        assertEquals(BatchState.COMPLETED, updated.state());
    }

    @Test
    void testWithProgressAllFailed() {
        Instant now = Instant.now();
        BatchJobStatus original = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 0, 5, now, now, null
        );
        
        BatchJobStatus updated = original.withProgress(0, 10);
        
        assertEquals(BatchState.FAILED, updated.state());
    }

    @Test
    void testWithState() {
        Instant now = Instant.now();
        BatchJobStatus original = new BatchJobStatus(
            "job-123", "batch-456", BatchState.SUBMITTED, 
            10, 0, 0, now, now, null
        );
        
        BatchJobStatus updated = original.withState(BatchState.CANCELLED);
        
        assertEquals(BatchState.CANCELLED, updated.state());
        assertTrue(updated.updatedAt().isAfter(original.updatedAt()));
    }

    @Test
    void testWithError() {
        Instant now = Instant.now();
        BatchJobStatus original = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        
        BatchJobStatus updated = original.withError("Network timeout");
        
        assertEquals(BatchState.FAILED, updated.state());
        assertEquals("Network timeout", updated.errorMessage());
    }

    @Test
    void testGetPendingRequests() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        
        assertEquals(3, status.getPendingRequests());
    }

    @Test
    void testGetCompletionPercentage() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        
        assertEquals(0.7, status.getCompletionPercentage(), 0.001);
    }

    @Test
    void testGetCompletionPercentageZeroTotal() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.COMPLETED, 
            0, 0, 0, now, now, null
        );
        
        assertEquals(1.0, status.getCompletionPercentage(), 0.001);
    }

    @Test
    void testGetSuccessRate() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 6, 2, now, now, null
        );
        
        assertEquals(0.75, status.getSuccessRate(), 0.001);
    }

    @Test
    void testGetSuccessRateNoProcessed() {
        Instant now = Instant.now();
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.SUBMITTED, 
            10, 0, 0, now, now, null
        );
        
        assertEquals(0.0, status.getSuccessRate(), 0.001);
    }

    @Test
    void testIsComplete() {
        Instant now = Instant.now();
        
        BatchJobStatus incomplete = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        assertFalse(incomplete.isComplete());
        
        BatchJobStatus complete = new BatchJobStatus(
            "job-123", "batch-456", BatchState.COMPLETED, 
            10, 8, 2, now, now, null
        );
        assertTrue(complete.isComplete());
    }

    @Test
    void testHasFailures() {
        Instant now = Instant.now();
        
        BatchJobStatus noFailures = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 0, now, now, null
        );
        assertFalse(noFailures.hasFailures());
        
        BatchJobStatus hasFailures = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, now, now, null
        );
        assertTrue(hasFailures.hasFailures());
    }

    @Test
    void testGetDurationMillis() {
        Instant created = Instant.now();
        Instant updated = created.plusSeconds(30);
        
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.PROCESSING, 
            10, 5, 2, created, updated, null
        );
        
        assertEquals(30000, status.getDurationMillis());
    }

    @Test
    void testErrorMessageSanitization() {
        Instant now = Instant.now();
        
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.FAILED, 
            10, 5, 5, now, now, "<script>alert('xss')</script>"
        );
        
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;", status.errorMessage());
    }

    @Test
    void testErrorMessageTruncation() {
        Instant now = Instant.now();
        String longMessage = "x".repeat(1500);
        
        BatchJobStatus status = new BatchJobStatus(
            "job-123", "batch-456", BatchState.FAILED, 
            10, 5, 5, now, now, longMessage
        );
        
        assertTrue(status.errorMessage().length() <= 1000);
        assertTrue(status.errorMessage().endsWith("..."));
    }
}