package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BatchMetricsTest {

    @Test
    void testValidConstruction() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(start, metrics.startTime());
        assertEquals(end, metrics.endTime());
        assertEquals(10, metrics.totalRequests());
        assertEquals(8, metrics.successfulRequests());
        assertEquals(2, metrics.failedRequests());
        assertEquals(5000, metrics.totalTokensUsed());
        assertEquals(250, metrics.totalCostCents());
        assertEquals(3000.0, metrics.averageLatencyMs());
        assertEquals(0.33, metrics.requestsPerSecond());
    }

    @Test
    void testValidationStartTimeRequired() {
        Instant end = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(null, end, 10, 8, 2, 5000, 250, 3000.0, 0.33)
        );
    }

    @Test
    void testValidationEndTimeRequired() {
        Instant start = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, null, 10, 8, 2, 5000, 250, 3000.0, 0.33)
        );
    }

    @Test
    void testValidationEndTimeAfterStartTime() {
        Instant start = Instant.now();
        Instant earlier = start.minusSeconds(10);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, earlier, 10, 8, 2, 5000, 250, 3000.0, 0.33)
        );
    }

    @Test
    void testValidationNegativeValues() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, -1, 8, 2, 5000, 250, 3000.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, -1, 2, 5000, 250, 3000.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, -1, 5000, 250, 3000.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, 2, -1, 250, 3000.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, 2, 5000, -1, 3000.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, 2, 5000, 250, -1.0, 0.33)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, 2, 5000, 250, 3000.0, -0.1)
        );
    }

    @Test
    void testValidationRequestCounts() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        // Successful + failed cannot exceed total
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchMetrics(start, end, 10, 8, 5, 5000, 250, 3000.0, 0.33)
        );
    }

    @Test
    void testCompletedFactory() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = BatchMetrics.completed(start, end, 10, 8, 2, 5000, 250);
        
        assertEquals(10, metrics.totalRequests());
        assertEquals(8, metrics.successfulRequests());
        assertEquals(2, metrics.failedRequests());
        assertEquals(5000, metrics.totalTokensUsed());
        assertEquals(250, metrics.totalCostCents());
        
        // Check calculated values
        assertEquals(3000.0, metrics.averageLatencyMs(), 0.1); // 30000ms / 10 requests
        assertEquals(0.333, metrics.requestsPerSecond(), 0.01); // 10 requests / 30 seconds
    }

    @Test
    void testEmptyFactory() {
        Instant start = Instant.now();
        
        BatchMetrics metrics = BatchMetrics.empty(start);
        
        assertEquals(start, metrics.startTime());
        assertEquals(start, metrics.endTime());
        assertEquals(0, metrics.totalRequests());
        assertEquals(0, metrics.successfulRequests());
        assertEquals(0, metrics.failedRequests());
        assertEquals(0, metrics.totalTokensUsed());
        assertEquals(0, metrics.totalCostCents());
        assertEquals(0.0, metrics.averageLatencyMs());
        assertEquals(0.0, metrics.requestsPerSecond());
    }

    @Test
    void testGetDuration() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(45);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 4500.0, 0.22
        );
        
        assertEquals(45000, metrics.getDurationMillis());
        assertEquals(45.0, metrics.getDurationSeconds(), 0.1);
    }

    @Test
    void testGetSuccessRate() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(0.8, metrics.getSuccessRate(), 0.001);
    }

    @Test
    void testGetSuccessRateZeroRequests() {
        Instant start = Instant.now();
        
        BatchMetrics metrics = BatchMetrics.empty(start);
        
        assertEquals(0.0, metrics.getSuccessRate());
    }

    @Test
    void testGetFailureRate() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(0.2, metrics.getFailureRate(), 0.001);
    }

    @Test
    void testGetTotalCostDollars() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(2.50, metrics.getTotalCostDollars(), 0.001);
    }

    @Test
    void testGetAverageCostPerRequest() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(25.0, metrics.getAverageCostPerRequestCents(), 0.001);
        assertEquals(0.25, metrics.getAverageCostPerRequestDollars(), 0.001);
    }

    @Test
    void testGetAverageCostPerRequestZeroRequests() {
        Instant start = Instant.now();
        
        BatchMetrics metrics = BatchMetrics.empty(start);
        
        assertEquals(0.0, metrics.getAverageCostPerRequestCents());
        assertEquals(0.0, metrics.getAverageCostPerRequestDollars());
    }

    @Test
    void testGetAverageTokensPerRequest() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(500.0, metrics.getAverageTokensPerRequest(), 0.001);
    }

    @Test
    void testGetTokensPerSecond() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        assertEquals(166.67, metrics.getTokensPerSecond(), 0.1); // 5000 tokens / 30 seconds
    }

    @Test
    void testGetTokensPerSecondZeroDuration() {
        Instant start = Instant.now();
        
        BatchMetrics metrics = new BatchMetrics(
            start, start, 10, 8, 2, 5000, 250, 0.0, 10.0
        );
        
        assertEquals(0.0, metrics.getTokensPerSecond());
    }

    @Test
    void testHasGoodPerformance() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        // Good performance: 80% success, 1+ req/sec
        BatchMetrics good = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 1.5
        );
        assertTrue(good.hasGoodPerformance());
        
        // Poor success rate
        BatchMetrics poorSuccess = new BatchMetrics(
            start, end, 10, 5, 5, 5000, 250, 3000.0, 1.5
        );
        assertFalse(poorSuccess.hasGoodPerformance());
        
        // Poor throughput
        BatchMetrics poorThroughput = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.5
        );
        assertFalse(poorThroughput.hasGoodPerformance());
    }

    @Test
    void testIsCostEffective() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        // Cost effective: < $0.10 per request (50 cents / 10 requests = 5 cents = $0.05 per request)
        BatchMetrics costEffective = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 50, 3000.0, 0.33 // $0.05 per request
        );
        assertTrue(costEffective.isCostEffective());
        
        // Expensive (1500 cents / 10 requests = 150 cents = $1.50 per request)
        BatchMetrics expensive = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 1500, 3000.0, 0.33 // $1.50 per request
        );
        assertFalse(expensive.isCostEffective());
    }

    @Test
    void testHasLowLatency() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        // Low latency: < 5 seconds
        BatchMetrics lowLatency = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        assertTrue(lowLatency.hasLowLatency());
        
        // High latency
        BatchMetrics highLatency = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 8000.0, 0.33
        );
        assertFalse(highLatency.hasLowLatency());
    }

    @Test
    void testGetSummary() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        String summary = metrics.getSummary();
        
        assertTrue(summary.contains("30.0s"));
        assertTrue(summary.contains("8/10"));
        assertTrue(summary.contains("80.0%"));
        assertTrue(summary.contains("3000ms"));
        assertTrue(summary.contains("0.3 req/s"));
        assertTrue(summary.contains("$2.5000"));
    }

    @Test
    void testGetDetailedMetrics() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(30);
        
        BatchMetrics metrics = new BatchMetrics(
            start, end, 10, 8, 2, 5000, 250, 3000.0, 0.33
        );
        
        String detailed = metrics.getDetailedMetrics();
        
        assertTrue(detailed.contains("Batch Metrics:"));
        assertTrue(detailed.contains("Duration:"));
        assertTrue(detailed.contains("Requests:"));
        assertTrue(detailed.contains("Performance:"));
        assertTrue(detailed.contains("Tokens:"));
        assertTrue(detailed.contains("Cost:"));
    }
}