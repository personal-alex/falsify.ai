package ai.falsify.prediction.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Record representing performance metrics for a batch processing operation.
 * Contains timing, throughput, and resource usage information.
 */
public record BatchMetrics(
    Instant startTime,
    Instant endTime,
    int totalRequests,
    int successfulRequests,
    int failedRequests,
    long totalTokensUsed,
    long totalCostCents,
    double averageLatencyMs,
    double requestsPerSecond
) {
    
    /**
     * Creates BatchMetrics with validation.
     * 
     * @param startTime when batch processing started (required)
     * @param endTime when batch processing ended (required)
     * @param totalRequests total number of requests processed (must be >= 0)
     * @param successfulRequests number of successful requests (must be >= 0)
     * @param failedRequests number of failed requests (must be >= 0)
     * @param totalTokensUsed total tokens consumed by the API (must be >= 0)
     * @param totalCostCents total cost in cents (must be >= 0)
     * @param averageLatencyMs average latency per request in milliseconds (must be >= 0)
     * @param requestsPerSecond throughput in requests per second (must be >= 0)
     */
    public BatchMetrics {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        
        if (totalRequests < 0) {
            throw new IllegalArgumentException("Total requests cannot be negative: " + totalRequests);
        }
        
        if (successfulRequests < 0) {
            throw new IllegalArgumentException("Successful requests cannot be negative: " + successfulRequests);
        }
        
        if (failedRequests < 0) {
            throw new IllegalArgumentException("Failed requests cannot be negative: " + failedRequests);
        }
        
        if (successfulRequests + failedRequests > totalRequests) {
            throw new IllegalArgumentException(
                "Successful + failed requests (" + (successfulRequests + failedRequests) + 
                ") cannot exceed total requests (" + totalRequests + ")"
            );
        }
        
        if (totalTokensUsed < 0) {
            throw new IllegalArgumentException("Total tokens used cannot be negative: " + totalTokensUsed);
        }
        
        if (totalCostCents < 0) {
            throw new IllegalArgumentException("Total cost cannot be negative: " + totalCostCents);
        }
        
        if (averageLatencyMs < 0) {
            throw new IllegalArgumentException("Average latency cannot be negative: " + averageLatencyMs);
        }
        
        if (requestsPerSecond < 0) {
            throw new IllegalArgumentException("Requests per second cannot be negative: " + requestsPerSecond);
        }
    }
    
    /**
     * Creates BatchMetrics for a completed batch with calculated values.
     * 
     * @param startTime when processing started
     * @param endTime when processing ended
     * @param totalRequests total requests processed
     * @param successfulRequests successful requests
     * @param failedRequests failed requests
     * @param totalTokensUsed tokens consumed
     * @param totalCostCents cost in cents
     * @return new BatchMetrics with calculated throughput and latency
     */
    public static BatchMetrics completed(Instant startTime, Instant endTime, int totalRequests,
                                       int successfulRequests, int failedRequests,
                                       long totalTokensUsed, long totalCostCents) {
        Duration duration = Duration.between(startTime, endTime);
        double durationMs = duration.toMillis();
        
        double averageLatencyMs = totalRequests > 0 ? durationMs / totalRequests : 0.0;
        double requestsPerSecond = durationMs > 0 ? (totalRequests * 1000.0) / durationMs : 0.0;
        
        return new BatchMetrics(startTime, endTime, totalRequests, successfulRequests, failedRequests,
                              totalTokensUsed, totalCostCents, averageLatencyMs, requestsPerSecond);
    }
    
    /**
     * Creates empty BatchMetrics for initialization.
     * 
     * @param startTime when processing started
     * @return new empty BatchMetrics
     */
    public static BatchMetrics empty(Instant startTime) {
        return new BatchMetrics(startTime, startTime, 0, 0, 0, 0, 0, 0.0, 0.0);
    }
    
    /**
     * Gets the total duration of the batch processing.
     * 
     * @return duration between start and end time
     */
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Gets the duration in milliseconds.
     * 
     * @return duration in milliseconds
     */
    public long getDurationMillis() {
        return getDuration().toMillis();
    }
    
    /**
     * Gets the duration in seconds.
     * 
     * @return duration in seconds
     */
    public double getDurationSeconds() {
        return getDurationMillis() / 1000.0;
    }
    
    /**
     * Gets the success rate as a percentage (0.0 to 1.0).
     * 
     * @return success rate
     */
    public double getSuccessRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) successfulRequests / totalRequests;
    }
    
    /**
     * Gets the failure rate as a percentage (0.0 to 1.0).
     * 
     * @return failure rate
     */
    public double getFailureRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) failedRequests / totalRequests;
    }
    
    /**
     * Gets the total cost in dollars.
     * 
     * @return cost in dollars
     */
    public double getTotalCostDollars() {
        return totalCostCents / 100.0;
    }
    
    /**
     * Gets the average cost per request in cents.
     * 
     * @return average cost per request in cents
     */
    public double getAverageCostPerRequestCents() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) totalCostCents / totalRequests;
    }
    
    /**
     * Gets the average cost per request in dollars.
     * 
     * @return average cost per request in dollars
     */
    public double getAverageCostPerRequestDollars() {
        return getAverageCostPerRequestCents() / 100.0;
    }
    
    /**
     * Gets the average tokens per request.
     * 
     * @return average tokens per request
     */
    public double getAverageTokensPerRequest() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) totalTokensUsed / totalRequests;
    }
    
    /**
     * Gets the tokens per second throughput.
     * 
     * @return tokens processed per second
     */
    public double getTokensPerSecond() {
        double durationSeconds = getDurationSeconds();
        if (durationSeconds == 0) {
            return 0.0;
        }
        return totalTokensUsed / durationSeconds;
    }
    
    /**
     * Checks if this batch had good performance (>= 80% success rate, >= 1 req/sec).
     * 
     * @return true if performance was good
     */
    public boolean hasGoodPerformance() {
        return getSuccessRate() >= 0.8 && requestsPerSecond >= 1.0;
    }
    
    /**
     * Checks if this batch was cost-effective (< $0.10 per request).
     * 
     * @return true if cost was reasonable
     */
    public boolean isCostEffective() {
        return getAverageCostPerRequestDollars() < 0.10;
    }
    
    /**
     * Checks if this batch had low latency (< 5 seconds average).
     * 
     * @return true if latency was low
     */
    public boolean hasLowLatency() {
        return averageLatencyMs < 5000;
    }
    
    /**
     * Gets a summary string of the key metrics.
     * 
     * @return formatted summary string
     */
    public String getSummary() {
        return String.format(
            "Duration: %.1fs, Success: %d/%d (%.1f%%), Avg Latency: %.0fms, Throughput: %.1f req/s, Cost: $%.4f",
            getDurationSeconds(),
            successfulRequests, totalRequests, getSuccessRate() * 100,
            averageLatencyMs,
            requestsPerSecond,
            getTotalCostDollars()
        );
    }
    
    /**
     * Gets detailed metrics as a formatted string.
     * 
     * @return detailed metrics string
     */
    public String getDetailedMetrics() {
        return String.format(
            "Batch Metrics:\n" +
            "  Duration: %s (%.1fs)\n" +
            "  Requests: %d total, %d successful (%.1f%%), %d failed (%.1f%%)\n" +
            "  Performance: %.1f req/s, %.0fms avg latency\n" +
            "  Tokens: %d total (%.1f avg/req, %.1f tokens/s)\n" +
            "  Cost: $%.4f total ($%.6f avg/req)",
            getDuration(),
            getDurationSeconds(),
            totalRequests, successfulRequests, getSuccessRate() * 100, failedRequests, getFailureRate() * 100,
            requestsPerSecond, averageLatencyMs,
            totalTokensUsed, getAverageTokensPerRequest(), getTokensPerSecond(),
            getTotalCostDollars(), getAverageCostPerRequestDollars()
        );
    }
}