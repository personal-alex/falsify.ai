package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatchNotificationEventTest {

    @Test
    void testValidConstruction() {
        Instant timestamp = Instant.now();
        Map<String, Object> data = Map.of("status", "PROCESSING", "progress", 50);
        
        BatchNotificationEvent event = new BatchNotificationEvent(
            "batch.processing", "job-123", "batch-456", timestamp, data
        );
        
        assertEquals("batch.processing", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals("batch-456", event.batchId());
        assertEquals(timestamp, event.timestamp());
        assertEquals(data, event.data());
    }

    @Test
    void testValidationTypeRequired() {
        Instant timestamp = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent(null, "job-123", "batch-456", timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("", "job-123", "batch-456", timestamp, Map.of())
        );
    }

    @Test
    void testValidationJobIdRequired() {
        Instant timestamp = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("batch.processing", null, "batch-456", timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("batch.processing", "", "batch-456", timestamp, Map.of())
        );
    }

    @Test
    void testValidationBatchIdRequired() {
        Instant timestamp = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("batch.processing", "job-123", null, timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("batch.processing", "job-123", "", timestamp, Map.of())
        );
    }

    @Test
    void testValidationTimestampRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchNotificationEvent("batch.processing", "job-123", "batch-456", null, Map.of())
        );
    }

    @Test
    void testBatchSubmittedFactory() {
        BatchNotificationEvent event = BatchNotificationEvent.batchSubmitted("job-123", "batch-456", 10);
        
        assertEquals("batch.submitted", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals("batch-456", event.batchId());
        assertEquals("SUBMITTED", event.getStatus());
        assertEquals(10, event.getTotalRequests());
        assertNotNull(event.timestamp());
    }

    @Test
    void testBatchProcessingFactory() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing(
            "job-123", "batch-456", 5, 2, 10
        );
        
        assertEquals("batch.processing", event.type());
        assertEquals("PROCESSING", event.getStatus());
        assertEquals(5, event.getCompletedRequests());
        assertEquals(2, event.getFailedRequests());
        assertEquals(10, event.getTotalRequests());
        assertEquals(70.0, event.getProgressPercentage(), 0.001); // (5+2)/10 * 100
    }

    @Test
    void testBatchProgressFactory() {
        Map<String, Object> partialResults = Map.of("result1", "data1");
        BatchNotificationEvent event = BatchNotificationEvent.batchProgress(
            "job-123", "batch-456", 3, 1, 10, partialResults
        );
        
        assertEquals("batch.progress", event.type());
        assertEquals("PROCESSING", event.getStatus());
        assertEquals(3, event.getCompletedRequests());
        assertEquals(1, event.getFailedRequests());
        assertEquals(40.0, event.getProgressPercentage(), 0.001); // (3+1)/10 * 100
        assertTrue(event.data().containsKey("partialResults"));
    }

    @Test
    void testBatchProgressFactoryNullPartialResults() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProgress(
            "job-123", "batch-456", 3, 1, 10, null
        );
        
        assertEquals("batch.progress", event.type());
        assertFalse(event.data().containsKey("partialResults"));
    }

    @Test
    void testBatchCompletedFactory() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        BatchMetrics metrics = new BatchMetrics(start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33);
        
        BatchNotificationEvent event = BatchNotificationEvent.batchCompleted(
            "job-123", "batch-456", 8, 2, 10, metrics
        );
        
        assertEquals("batch.completed", event.type());
        assertEquals("COMPLETED", event.getStatus());
        assertEquals(8, event.getCompletedRequests());
        assertEquals(2, event.getFailedRequests());
        assertEquals(100.0, event.getProgressPercentage(), 0.001);
        assertEquals(80.0, event.data().get("successRate")); // 8/10 * 100
        assertTrue(event.data().containsKey("metrics"));
    }

    @Test
    void testBatchCompletedFactoryNullMetrics() {
        BatchNotificationEvent event = BatchNotificationEvent.batchCompleted(
            "job-123", "batch-456", 8, 2, 10, null
        );
        
        assertEquals("batch.completed", event.type());
        assertFalse(event.data().containsKey("metrics"));
    }

    @Test
    void testBatchFailedFactory() {
        BatchNotificationEvent event = BatchNotificationEvent.batchFailed(
            "job-123", "batch-456", 3, 7, 10, "API Error"
        );
        
        assertEquals("batch.failed", event.type());
        assertEquals("FAILED", event.getStatus());
        assertEquals(3, event.getCompletedRequests());
        assertEquals(7, event.getFailedRequests());
        assertEquals(100.0, event.getProgressPercentage(), 0.001); // All processed
        assertEquals("API Error", event.getErrorMessage());
    }

    @Test
    void testBatchFailedFactoryNullError() {
        BatchNotificationEvent event = BatchNotificationEvent.batchFailed(
            "job-123", "batch-456", 3, 7, 10, null
        );
        
        assertEquals("Unknown error", event.getErrorMessage());
    }

    @Test
    void testBatchCancelledFactory() {
        BatchNotificationEvent event = BatchNotificationEvent.batchCancelled(
            "job-123", "batch-456", 3, 2, 10
        );
        
        assertEquals("batch.cancelled", event.type());
        assertEquals("CANCELLED", event.getStatus());
        assertEquals(3, event.getCompletedRequests());
        assertEquals(2, event.getFailedRequests());
        assertEquals(50.0, event.getProgressPercentage(), 0.001); // (3+2)/10 * 100
    }

    @Test
    void testBatchTimeoutFactory() {
        BatchNotificationEvent event = BatchNotificationEvent.batchTimeout(
            "job-123", "batch-456", 4, 1, 10, 30
        );
        
        assertEquals("batch.timeout", event.type());
        assertEquals("TIMEOUT", event.getStatus());
        assertEquals(4, event.getCompletedRequests());
        assertEquals(1, event.getFailedRequests());
        assertEquals(50.0, event.getProgressPercentage(), 0.001); // (4+1)/10 * 100
        assertEquals(30, event.data().get("timeoutMinutes"));
    }

    @Test
    void testToNotificationEvent() {
        BatchNotificationEvent batchEvent = BatchNotificationEvent.batchSubmitted("job-123", "batch-456", 10);
        NotificationEvent notificationEvent = batchEvent.toNotificationEvent();
        
        assertEquals(batchEvent.type(), notificationEvent.type());
        assertEquals(batchEvent.jobId(), notificationEvent.jobId());
        assertEquals(batchEvent.timestamp(), notificationEvent.timestamp());
        assertEquals("batch-456", notificationEvent.getData("batchId"));
        assertEquals("SUBMITTED", notificationEvent.getData("status"));
    }

    @Test
    void testGetStatus() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        assertEquals("PROCESSING", event.getStatus());
    }

    @Test
    void testGetStatusUnknown() {
        Instant timestamp = Instant.now();
        BatchNotificationEvent event = new BatchNotificationEvent(
            "batch.custom", "job-123", "batch-456", timestamp, Map.of()
        );
        
        assertEquals("UNKNOWN", event.getStatus());
    }

    @Test
    void testGetProgressPercentage() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 3, 2, 10);
        
        assertEquals(50.0, event.getProgressPercentage(), 0.001); // (3+2)/10 * 100
    }

    @Test
    void testGetProgressPercentageZeroTotal() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 0, 0, 0);
        
        assertEquals(0.0, event.getProgressPercentage(), 0.001);
    }

    @Test
    void testGetCompletedRequests() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        assertEquals(5, event.getCompletedRequests());
    }

    @Test
    void testGetFailedRequests() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        assertEquals(2, event.getFailedRequests());
    }

    @Test
    void testGetTotalRequests() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        assertEquals(10, event.getTotalRequests());
    }

    @Test
    void testGetErrorMessage() {
        BatchNotificationEvent event = BatchNotificationEvent.batchFailed(
            "job-123", "batch-456", 3, 7, 10, "Network timeout"
        );
        
        assertEquals("Network timeout", event.getErrorMessage());
    }

    @Test
    void testGetErrorMessageNull() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        assertNull(event.getErrorMessage());
    }

    @Test
    void testIsTerminal() {
        assertTrue(BatchNotificationEvent.batchCompleted("job-123", "batch-456", 10, 0, 10, null).isTerminal());
        assertTrue(BatchNotificationEvent.batchFailed("job-123", "batch-456", 5, 5, 10, "Error").isTerminal());
        assertTrue(BatchNotificationEvent.batchCancelled("job-123", "batch-456", 3, 2, 10).isTerminal());
        assertTrue(BatchNotificationEvent.batchTimeout("job-123", "batch-456", 4, 1, 10, 30).isTerminal());
        
        assertFalse(BatchNotificationEvent.batchSubmitted("job-123", "batch-456", 10).isTerminal());
        assertFalse(BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10).isTerminal());
    }

    @Test
    void testIsSuccessful() {
        assertTrue(BatchNotificationEvent.batchCompleted("job-123", "batch-456", 10, 0, 10, null).isSuccessful());
        
        assertFalse(BatchNotificationEvent.batchFailed("job-123", "batch-456", 5, 5, 10, "Error").isSuccessful());
        assertFalse(BatchNotificationEvent.batchCancelled("job-123", "batch-456", 3, 2, 10).isSuccessful());
        assertFalse(BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10).isSuccessful());
    }

    @Test
    void testToMap() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        Map<String, Object> map = event.toMap();
        
        assertEquals("batch.processing", map.get("type"));
        assertEquals("job-123", map.get("jobId"));
        assertEquals("batch-456", map.get("batchId"));
        assertNotNull(map.get("timestamp"));
        assertTrue(map.containsKey("data"));
    }

    @Test
    void testToMapEmptyData() {
        Instant timestamp = Instant.now();
        BatchNotificationEvent event = new BatchNotificationEvent(
            "batch.custom", "job-123", "batch-456", timestamp, Map.of()
        );
        
        Map<String, Object> map = event.toMap();
        
        assertEquals("batch.custom", map.get("type"));
        assertEquals("job-123", map.get("jobId"));
        assertEquals("batch-456", map.get("batchId"));
        assertFalse(map.containsKey("data")); // Empty data not included
    }

    @Test
    void testGetSummary() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        String summary = event.getSummary();
        
        assertTrue(summary.contains("batch.processing"));
        assertTrue(summary.contains("job-123"));
        assertTrue(summary.contains("batch-456"));
        assertTrue(summary.contains("PROCESSING"));
        assertTrue(summary.contains("7/10")); // completed + failed / total
        assertTrue(summary.contains("70.0%"));
    }

    @Test
    void testFieldTrimming() {
        Instant timestamp = Instant.now();
        
        BatchNotificationEvent event = new BatchNotificationEvent(
            "  batch.processing  ", "  job-123  ", "  batch-456  ", timestamp, Map.of()
        );
        
        assertEquals("batch.processing", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals("batch-456", event.batchId());
    }

    @Test
    void testNullDataHandled() {
        Instant timestamp = Instant.now();
        
        BatchNotificationEvent event = new BatchNotificationEvent(
            "batch.processing", "job-123", "batch-456", timestamp, null
        );
        
        assertEquals(Map.of(), event.data());
    }
}