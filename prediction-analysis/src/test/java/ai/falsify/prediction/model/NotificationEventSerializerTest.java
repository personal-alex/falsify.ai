package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventSerializerTest {

    @Test
    void testToJsonNotificationEvent() {
        Map<String, Object> data = Map.of("status", "running", "progress", 50);
        NotificationEvent event = NotificationEvent.create("job.progress", "job-123", data);
        
        String json = NotificationEventSerializer.toJson(event);
        
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"job.progress\""));
        assertTrue(json.contains("\"jobId\":\"job-123\""));
        assertTrue(json.contains("\"status\":\"running\""));
        assertTrue(json.contains("\"progress\":50"));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void testToJsonBatchNotificationEvent() {
        BatchNotificationEvent event = BatchNotificationEvent.batchProcessing("job-123", "batch-456", 5, 2, 10);
        
        String json = NotificationEventSerializer.toJson(event);
        
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"batch.processing\""));
        assertTrue(json.contains("\"jobId\":\"job-123\""));
        assertTrue(json.contains("\"batchId\":\"batch-456\""));
        assertTrue(json.contains("\"completedRequests\":5"));
        assertTrue(json.contains("\"failedRequests\":2"));
    }

    @Test
    void testToJsonNullEvent() {
        String json = NotificationEventSerializer.toJson((NotificationEvent) null);
        assertEquals("{}", json);
        
        String batchJson = NotificationEventSerializer.toJson((BatchNotificationEvent) null);
        assertEquals("{}", batchJson);
    }

    @Test
    void testMapToJsonBasicTypes() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("string", "hello");
        map.put("number", 42);
        map.put("boolean", true);
        map.put("nullValue", null);
        
        String json = NotificationEventSerializer.mapToJson(map);
        
        assertTrue(json.contains("\"string\":\"hello\""));
        assertTrue(json.contains("\"number\":42"));
        assertTrue(json.contains("\"boolean\":true"));
        assertTrue(json.contains("\"nullValue\":null"));
    }

    @Test
    void testMapToJsonNestedMap() {
        Map<String, Object> nested = Map.of("inner", "value");
        Map<String, Object> map = Map.of("outer", nested);
        
        String json = NotificationEventSerializer.mapToJson(map);
        
        assertTrue(json.contains("\"outer\":{\"inner\":\"value\"}"));
    }

    @Test
    void testMapToJsonArray() {
        Map<String, Object> map = Map.of("array", List.of("item1", "item2", 42));
        
        String json = NotificationEventSerializer.mapToJson(map);
        
        assertTrue(json.contains("\"array\":[\"item1\",\"item2\",42]"));
    }

    @Test
    void testMapToJsonEmptyMap() {
        String json = NotificationEventSerializer.mapToJson(Map.of());
        assertEquals("{}", json);
    }

    @Test
    void testMapToJsonNullMap() {
        String json = NotificationEventSerializer.mapToJson(null);
        assertEquals("{}", json);
    }

    @Test
    void testJsonStringEscaping() {
        Map<String, Object> map = Map.of(
            "quotes", "He said \"Hello\"",
            "backslash", "C:\\path\\to\\file",
            "newline", "Line 1\nLine 2",
            "tab", "Column1\tColumn2"
        );
        
        String json = NotificationEventSerializer.mapToJson(map);
        
        assertTrue(json.contains("\"He said \\\"Hello\\\"\""));
        assertTrue(json.contains("\"C:\\\\path\\\\to\\\\file\""));
        assertTrue(json.contains("\"Line 1\\nLine 2\""));
        assertTrue(json.contains("\"Column1\\tColumn2\""));
    }

    @Test
    void testJsonControlCharacterEscaping() {
        Map<String, Object> map = Map.of(
            "control", "Text\u0000with\u0001control\u0002chars"
        );
        
        String json = NotificationEventSerializer.mapToJson(map);
        
        assertTrue(json.contains("\\u0000"));
        assertTrue(json.contains("\\u0001"));
        assertTrue(json.contains("\\u0002"));
    }

    @Test
    void testCreateSimpleMessage() {
        String json = NotificationEventSerializer.createSimpleMessage("job.started", "job-123", "Job has started");
        
        assertTrue(json.contains("\"type\":\"job.started\""));
        assertTrue(json.contains("\"jobId\":\"job-123\""));
        assertTrue(json.contains("\"message\":\"Job has started\""));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void testCreateErrorMessage() {
        String json = NotificationEventSerializer.createErrorMessage("job.failed", "job-123", "Network timeout");
        
        assertTrue(json.contains("\"type\":\"job.failed\""));
        assertTrue(json.contains("\"jobId\":\"job-123\""));
        assertTrue(json.contains("\"error\":\"Network timeout\""));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void testCreateProgressMessage() {
        String json = NotificationEventSerializer.createProgressMessage("job.progress", "job-123", 7, 10);
        
        assertTrue(json.contains("\"type\":\"job.progress\""));
        assertTrue(json.contains("\"jobId\":\"job-123\""));
        assertTrue(json.contains("\"current\":7"));
        assertTrue(json.contains("\"total\":10"));
        assertTrue(json.contains("\"percentage\":70.0"));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void testCreateProgressMessageZeroTotal() {
        String json = NotificationEventSerializer.createProgressMessage("job.progress", "job-123", 0, 0);
        
        assertTrue(json.contains("\"percentage\":0.0"));
    }

    @Test
    void testIsValidJsonValid() {
        assertTrue(NotificationEventSerializer.isValidJson("{}"));
        assertTrue(NotificationEventSerializer.isValidJson("{\"key\":\"value\"}"));
        assertTrue(NotificationEventSerializer.isValidJson("{\"nested\":{\"inner\":\"value\"}}"));
        assertTrue(NotificationEventSerializer.isValidJson("{\"array\":[1,2,3]}"));
        assertTrue(NotificationEventSerializer.isValidJson("{\"escaped\":\"He said \\\"Hello\\\"\"}"));
    }

    @Test
    void testIsValidJsonInvalid() {
        assertFalse(NotificationEventSerializer.isValidJson(null));
        assertFalse(NotificationEventSerializer.isValidJson(""));
        assertFalse(NotificationEventSerializer.isValidJson("   "));
        assertFalse(NotificationEventSerializer.isValidJson("{"));
        assertFalse(NotificationEventSerializer.isValidJson("}"));
        assertFalse(NotificationEventSerializer.isValidJson("[]"));
        assertFalse(NotificationEventSerializer.isValidJson("not json"));
        assertFalse(NotificationEventSerializer.isValidJson("{\"unclosed\":\"string}"));
        assertFalse(NotificationEventSerializer.isValidJson("{\"unbalanced\":{\"braces\"}"));
    }

    @Test
    void testComplexJsonSerialization() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        BatchMetrics metrics = new BatchMetrics(start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33);
        
        BatchNotificationEvent event = BatchNotificationEvent.batchCompleted(
            "job-123", "batch-456", 8, 2, 10, metrics
        );
        
        String json = NotificationEventSerializer.toJson(event);
        
        // Verify the JSON is valid
        assertTrue(NotificationEventSerializer.isValidJson(json));
        
        // Verify key content is present
        assertTrue(json.contains("\"type\":\"batch.completed\""));
        assertTrue(json.contains("\"batchId\":\"batch-456\""));
        assertTrue(json.contains("\"metrics\":{"));
        assertTrue(json.contains("\"durationSeconds\":"));
        assertTrue(json.contains("\"totalCostDollars\":"));
    }

    @Test
    void testSerializationWithSpecialCharacters() {
        Map<String, Object> data = Map.of(
            "message", "Error: \"Connection failed\" at line 42\nStack trace:\n\tFunction()",
            "path", "C:\\Users\\Test\\file.txt"
        );
        
        NotificationEvent event = NotificationEvent.create("error.occurred", "job-123", data);
        String json = NotificationEventSerializer.toJson(event);
        
        // Verify the JSON is valid despite special characters
        assertTrue(NotificationEventSerializer.isValidJson(json));
        
        // Verify escaping worked
        assertTrue(json.contains("\\\"Connection failed\\\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\\"));
    }

    @Test
    void testSerializationPerformance() {
        // Create a large event with nested data
        Map<String, Object> largeData = new java.util.HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeData.put("key" + i, "value" + i);
        }
        
        NotificationEvent event = NotificationEvent.create("large.event", "job-123", largeData);
        
        // This should complete quickly without issues
        long startTime = System.currentTimeMillis();
        String json = NotificationEventSerializer.toJson(event);
        long endTime = System.currentTimeMillis();
        
        assertTrue(endTime - startTime < 1000); // Should take less than 1 second
        assertTrue(NotificationEventSerializer.isValidJson(json));
        assertTrue(json.length() > 1000); // Should be a substantial JSON string
    }
}