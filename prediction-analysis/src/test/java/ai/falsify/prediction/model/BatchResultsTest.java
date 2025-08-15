package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatchResultsTest {

    @Test
    void testValidConstruction() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.success("req-2", "Result 2"),
            "req-3", BatchResponse.failure("req-3", "Error")
        );
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 3, 2, 1, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals("batch-123", results.batchId());
        assertEquals(responses, results.responses());
        assertEquals(metrics, results.metrics());
    }

    @Test
    void testValidationBatchIdRequired() {
        Map<String, BatchResponse> responses = Map.of();
        BatchMetrics metrics = BatchMetrics.empty(Instant.now());
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults(null, responses, metrics)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults("", responses, metrics)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults("   ", responses, metrics)
        );
    }

    @Test
    void testValidationResponsesRequired() {
        BatchMetrics metrics = BatchMetrics.empty(Instant.now());
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults("batch-123", null, metrics)
        );
    }

    @Test
    void testValidationMetricsRequired() {
        Map<String, BatchResponse> responses = Map.of();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults("batch-123", responses, null)
        );
    }

    @Test
    void testValidationMetricsMatchResponses() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        // Metrics don't match actual response counts
        Instant start = Instant.now();
        BatchMetrics wrongMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 2, 0, 1000, 50, 15000.0, 0.067 // Says 2 successful, 0 failed
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResults("batch-123", responses, wrongMetrics)
        );
    }

    @Test
    void testCompletedFactory() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults results = BatchResults.completed("batch-123", responses, metrics);
        
        assertEquals("batch-123", results.batchId());
        assertEquals(responses, results.responses());
        assertEquals(metrics, results.metrics());
    }

    @Test
    void testEmptyFactory() {
        BatchMetrics metrics = BatchMetrics.empty(Instant.now());
        
        BatchResults results = BatchResults.empty("batch-123", metrics);
        
        assertEquals("batch-123", results.batchId());
        assertTrue(results.responses().isEmpty());
        assertEquals(metrics, results.metrics());
    }

    @Test
    void testGetTotalResponses() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 1, 1, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(2, results.getTotalResponses());
    }

    @Test
    void testGetSuccessfulResponses() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.success("req-2", "Result 2"),
            "req-3", BatchResponse.failure("req-3", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 2, 1, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(2, results.getSuccessfulResponses());
    }

    @Test
    void testGetFailedResponses() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 1, 1, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(1, results.getFailedResponses());
    }

    @Test
    void testGetPartialResponses() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error"),
            "req-3", BatchResponse.partialFailure("req-3", "Partial", "Timeout")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 1, 2, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(1, results.getPartialResponses());
    }

    @Test
    void testGetSuccessfulResponsesMap() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.success("req-2", "Result 2"),
            "req-3", BatchResponse.failure("req-3", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 2, 1, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        Map<String, BatchResponse> successful = results.getSuccessfulResponsesMap();
        
        assertEquals(2, successful.size());
        assertTrue(successful.containsKey("req-1"));
        assertTrue(successful.containsKey("req-2"));
        assertFalse(successful.containsKey("req-3"));
    }

    @Test
    void testGetFailedResponsesMap() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 1, 1, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        Map<String, BatchResponse> failed = results.getFailedResponsesMap();
        
        assertEquals(1, failed.size());
        assertTrue(failed.containsKey("req-2"));
    }

    @Test
    void testGetResponse() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertNotNull(results.getResponse("req-1"));
        assertNull(results.getResponse("nonexistent"));
    }

    @Test
    void testHasResponse() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertTrue(results.hasResponse("req-1"));
        assertFalse(results.hasResponse("nonexistent"));
    }

    @Test
    void testHasSuccessfulResponses() {
        Map<String, BatchResponse> withSuccess = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Map<String, BatchResponse> withoutSuccess = Map.of(
            "req-1", BatchResponse.failure("req-1", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics successMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        BatchMetrics failureMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 0, 1, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults withSuccessResults = new BatchResults("batch-123", withSuccess, successMetrics);
        BatchResults withoutSuccessResults = new BatchResults("batch-123", withoutSuccess, failureMetrics);
        
        assertTrue(withSuccessResults.hasSuccessfulResponses());
        assertFalse(withoutSuccessResults.hasSuccessfulResponses());
    }

    @Test
    void testIsCompletelySuccessful() {
        Map<String, BatchResponse> allSuccess = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.success("req-2", "Result 2")
        );
        
        Map<String, BatchResponse> mixed = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics allSuccessMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 2, 0, 1000, 50, 15000.0, 0.067
        );
        BatchMetrics mixedMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 1, 1, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults allSuccessResults = new BatchResults("batch-123", allSuccess, allSuccessMetrics);
        BatchResults mixedResults = new BatchResults("batch-123", mixed, mixedMetrics);
        
        assertTrue(allSuccessResults.isCompletelySuccessful());
        assertFalse(mixedResults.isCompletelySuccessful());
    }

    @Test
    void testIsCompletelyFailed() {
        Map<String, BatchResponse> allFailed = Map.of(
            "req-1", BatchResponse.failure("req-1", "Error 1"),
            "req-2", BatchResponse.failure("req-2", "Error 2")
        );
        
        Map<String, BatchResponse> mixed = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics allFailedMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 0, 2, 1000, 50, 15000.0, 0.067
        );
        BatchMetrics mixedMetrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 1, 1, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults allFailedResults = new BatchResults("batch-123", allFailed, allFailedMetrics);
        BatchResults mixedResults = new BatchResults("batch-123", mixed, mixedMetrics);
        
        assertTrue(allFailedResults.isCompletelyFailed());
        assertFalse(mixedResults.isCompletelyFailed());
    }

    @Test
    void testIsEmpty() {
        BatchMetrics metrics = BatchMetrics.empty(Instant.now());
        
        BatchResults empty = BatchResults.empty("batch-123", metrics);
        
        assertTrue(empty.isEmpty());
    }

    @Test
    void testGetSuccessRate() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.success("req-2", "Result 2"),
            "req-3", BatchResponse.failure("req-3", "Error")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 2, 1, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(0.667, results.getSuccessRate(), 0.001);
    }

    @Test
    void testGetTotalContentLength() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Hello"), // 5 chars
            "req-2", BatchResponse.success("req-2", "World!"), // 6 chars
            "req-3", BatchResponse.failure("req-3", "Error") // 0 chars (no content)
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 2, 1, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(11, results.getTotalContentLength());
    }

    @Test
    void testGetAverageContentLength() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Hello"), // 5 chars
            "req-2", BatchResponse.success("req-2", "World!") // 6 chars
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 2, 2, 0, 1000, 50, 15000.0, 0.067
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals(5.5, results.getAverageContentLength(), 0.001);
    }

    @Test
    void testGetSummary() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Error"),
            "req-3", BatchResponse.partialFailure("req-3", "Partial", "Timeout")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 3, 1, 2, 1000, 50, 10000.0, 0.1
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        String summary = results.getSummary();
        
        assertTrue(summary.contains("batch-123"));
        assertTrue(summary.contains("3 responses"));
        assertTrue(summary.contains("1 successful"));
        assertTrue(summary.contains("2 failed"));
        assertTrue(summary.contains("1 partial"));
        assertTrue(summary.contains("33.3% success"));
    }

    @Test
    void testGetDetailedSummary() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        String detailed = results.getDetailedSummary();
        
        assertTrue(detailed.contains("Batch Results for batch-123"));
        assertTrue(detailed.contains("Total Responses:"));
        assertTrue(detailed.contains("Successful:"));
        assertTrue(detailed.contains("Failed:"));
        assertTrue(detailed.contains("Partial:"));
        assertTrue(detailed.contains("Total Content:"));
    }

    @Test
    void testGetErrorSummary() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1"),
            "req-2", BatchResponse.failure("req-2", "Network timeout"),
            "req-3", BatchResponse.failure("req-3", "Network timeout"),
            "req-4", BatchResponse.failure("req-4", "API rate limit")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 4, 1, 3, 1000, 50, 7500.0, 0.133
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        String errorSummary = results.getErrorSummary();
        
        assertTrue(errorSummary.contains("Error Summary:"));
        assertTrue(errorSummary.contains("2x: Network timeout"));
        assertTrue(errorSummary.contains("1x: API rate limit"));
    }

    @Test
    void testGetErrorSummaryNoErrors() {
        Map<String, BatchResponse> responses = Map.of(
            "req-1", BatchResponse.success("req-1", "Result 1")
        );
        
        Instant start = Instant.now();
        BatchMetrics metrics = new BatchMetrics(
            start, start.plusSeconds(30), 1, 1, 0, 1000, 50, 30000.0, 0.033
        );
        
        BatchResults results = new BatchResults("batch-123", responses, metrics);
        
        assertEquals("No errors", results.getErrorSummary());
    }

    @Test
    void testBatchIdTrimming() {
        BatchMetrics metrics = BatchMetrics.empty(Instant.now());
        
        BatchResults results = new BatchResults("  batch-123  ", Map.of(), metrics);
        
        assertEquals("batch-123", results.batchId());
    }
}