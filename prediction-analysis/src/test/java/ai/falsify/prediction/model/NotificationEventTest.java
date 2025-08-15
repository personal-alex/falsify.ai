package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventTest {

    @Test
    void testValidConstruction() {
        Instant timestamp = Instant.now();
        Map<String, Object> data = Map.of("key", "value", "number", 42);
        
        NotificationEvent event = new NotificationEvent("job.started", "job-123", timestamp, data);
        
        assertEquals("job.started", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals(timestamp, event.timestamp());
        assertEquals(data, event.data());
    }

    @Test
    void testValidationTypeRequired() {
        Instant timestamp = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent(null, "job-123", timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("", "job-123", timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("   ", "job-123", timestamp, Map.of())
        );
    }

    @Test
    void testValidationJobIdRequired() {
        Instant timestamp = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("job.started", null, timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("job.started", "", timestamp, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("job.started", "   ", timestamp, Map.of())
        );
    }

    @Test
    void testValidationTimestampRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new NotificationEvent("job.started", "job-123", null, Map.of())
        );
    }

    @Test
    void testNullDataHandled() {
        Instant timestamp = Instant.now();
        
        NotificationEvent event = new NotificationEvent("job.started", "job-123", timestamp, null);
        
        assertEquals(Map.of(), event.data());
        assertFalse(event.hasData("any"));
    }

    @Test
    void testCreateFactory() {
        Map<String, Object> data = Map.of("status", "running");
        
        NotificationEvent event = NotificationEvent.create("job.started", "job-123", data);
        
        assertEquals("job.started", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals(data, event.data());
        assertNotNull(event.timestamp());
    }

    @Test
    void testSimpleFactory() {
        NotificationEvent event = NotificationEvent.simple("job.started", "job-123");
        
        assertEquals("job.started", event.type());
        assertEquals("job-123", event.jobId());
        assertEquals(Map.of(), event.data());
        assertNotNull(event.timestamp());
    }

    @Test
    void testGetData() {
        Map<String, Object> data = Map.of("status", "running", "progress", 50);
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        assertEquals("running", event.getData("status"));
        assertEquals(50, event.getData("progress"));
        assertNull(event.getData("nonexistent"));
    }

    @Test
    void testGetDataAsString() {
        Map<String, Object> data = Map.of("status", "running", "progress", 50);
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        assertEquals("running", event.getDataAsString("status", "default"));
        assertEquals("50", event.getDataAsString("progress", "default"));
        assertEquals("default", event.getDataAsString("nonexistent", "default"));
    }

    @Test
    void testGetDataAsInteger() {
        Map<String, Object> data = Map.of("progress", 50, "percentage", 75.5, "stringNumber", "100");
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        assertEquals(50, event.getDataAsInteger("progress", 0));
        assertEquals(75, event.getDataAsInteger("percentage", 0)); // Double converted to int
        assertEquals(100, event.getDataAsInteger("stringNumber", 0));
        assertEquals(0, event.getDataAsInteger("nonexistent", 0));
        
        // Test invalid string number
        Map<String, Object> invalidData = Map.of("invalid", "not-a-number");
        NotificationEvent invalidEvent = NotificationEvent.create("test", "job-123", invalidData);
        assertEquals(0, invalidEvent.getDataAsInteger("invalid", 0));
    }

    @Test
    void testGetDataAsDouble() {
        Map<String, Object> data = Map.of("percentage", 75.5, "progress", 50, "stringNumber", "100.25");
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        assertEquals(75.5, event.getDataAsDouble("percentage", 0.0));
        assertEquals(50.0, event.getDataAsDouble("progress", 0.0)); // Integer converted to double
        assertEquals(100.25, event.getDataAsDouble("stringNumber", 0.0));
        assertEquals(0.0, event.getDataAsDouble("nonexistent", 0.0));
        
        // Test invalid string number
        Map<String, Object> invalidData = Map.of("invalid", "not-a-number");
        NotificationEvent invalidEvent = NotificationEvent.create("test", "job-123", invalidData);
        assertEquals(0.0, invalidEvent.getDataAsDouble("invalid", 0.0));
    }

    @Test
    void testHasData() {
        Map<String, Object> data = Map.of("status", "running");
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        assertTrue(event.hasData("status"));
        assertFalse(event.hasData("nonexistent"));
    }

    @Test
    void testGetTimestampMillis() {
        Instant timestamp = Instant.now();
        NotificationEvent event = new NotificationEvent("job.started", "job-123", timestamp, Map.of());
        
        assertEquals(timestamp.toEpochMilli(), event.getTimestampMillis());
    }

    @Test
    void testIsBatchEvent() {
        NotificationEvent batchEvent = NotificationEvent.simple("batch.submitted", "job-123");
        NotificationEvent jobEvent = NotificationEvent.simple("job.started", "job-123");
        
        assertTrue(batchEvent.isBatchEvent());
        assertFalse(jobEvent.isBatchEvent());
    }

    @Test
    void testIsJobEvent() {
        NotificationEvent jobEvent = NotificationEvent.simple("job.started", "job-123");
        NotificationEvent batchEvent = NotificationEvent.simple("batch.submitted", "job-123");
        
        assertTrue(jobEvent.isJobEvent());
        assertFalse(batchEvent.isJobEvent());
    }

    @Test
    void testIsPredictionEvent() {
        NotificationEvent predictionEvent = NotificationEvent.simple("prediction.extracted", "job-123");
        NotificationEvent jobEvent = NotificationEvent.simple("job.started", "job-123");
        
        assertTrue(predictionEvent.isPredictionEvent());
        assertFalse(jobEvent.isPredictionEvent());
    }

    @Test
    void testWithAdditionalData() {
        Map<String, Object> originalData = Map.of("status", "running");
        NotificationEvent original = NotificationEvent.create("job.progress", "job-123", originalData);
        
        Map<String, Object> additionalData = Map.of("progress", 50, "total", 100);
        NotificationEvent updated = original.withAdditionalData(additionalData);
        
        assertEquals("running", updated.getData("status"));
        assertEquals(50, updated.getData("progress"));
        assertEquals(100, updated.getData("total"));
        
        // Original should be unchanged
        assertFalse(original.hasData("progress"));
    }

    @Test
    void testWithAdditionalDataOverwrite() {
        Map<String, Object> originalData = Map.of("status", "running", "progress", 25);
        NotificationEvent original = NotificationEvent.create("job.progress", "job-123", originalData);
        
        Map<String, Object> additionalData = Map.of("progress", 50); // Overwrites existing
        NotificationEvent updated = original.withAdditionalData(additionalData);
        
        assertEquals("running", updated.getData("status"));
        assertEquals(50, updated.getData("progress")); // Overwritten value
    }

    @Test
    void testToMap() {
        Instant timestamp = Instant.now();
        Map<String, Object> data = Map.of("status", "running", "progress", 50);
        NotificationEvent event = new NotificationEvent("job.progress", "job-123", timestamp, data);
        
        Map<String, Object> map = event.toMap();
        
        assertEquals("job.progress", map.get("type"));
        assertEquals("job-123", map.get("jobId"));
        assertEquals(timestamp.toEpochMilli(), map.get("timestamp"));
        assertEquals(data, map.get("data"));
    }

    @Test
    void testToMapEmptyData() {
        Instant timestamp = Instant.now();
        NotificationEvent event = new NotificationEvent("job.started", "job-123", timestamp, Map.of());
        
        Map<String, Object> map = event.toMap();
        
        assertEquals("job.started", map.get("type"));
        assertEquals("job-123", map.get("jobId"));
        assertEquals(timestamp.toEpochMilli(), map.get("timestamp"));
        assertFalse(map.containsKey("data")); // Empty data not included
    }

    @Test
    void testGetSummary() {
        Map<String, Object> data = Map.of("status", "running", "progress", 50);
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        String summary = event.getSummary();
        
        assertTrue(summary.contains("job.progress"));
        assertTrue(summary.contains("job-123"));
        assertTrue(summary.contains("2 data fields"));
    }

    @Test
    void testFieldTrimming() {
        Instant timestamp = Instant.now();
        
        NotificationEvent event = new NotificationEvent("  job.started  ", "  job-123  ", timestamp, Map.of());
        
        assertEquals("job.started", event.type());
        assertEquals("job-123", event.jobId());
    }
}