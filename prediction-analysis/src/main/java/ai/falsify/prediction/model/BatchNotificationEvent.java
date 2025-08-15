package ai.falsify.prediction.model;

import java.time.Instant;
import java.util.Map;

/**
 * Specialized notification event for batch processing operations.
 * Extends NotificationEvent with batch-specific data and convenience methods.
 */
public record BatchNotificationEvent(
    String type,
    String jobId,
    String batchId,
    Instant timestamp,
    Map<String, Object> data
) {
    
    /**
     * Creates a BatchNotificationEvent with validation.
     * 
     * @param type the event type (required, should start with "batch.")
     * @param jobId the job identifier (required)
     * @param batchId the batch identifier (required)
     * @param timestamp when the event occurred (required)
     * @param data additional event data (optional)
     */
    public BatchNotificationEvent {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("Job ID cannot be null or empty");
        }
        
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }
        
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        
        // Sanitize and ensure immutability
        type = type.trim();
        jobId = jobId.trim();
        batchId = batchId.trim();
        
        if (data == null) {
            data = Map.of();
        }
    }
    
    /**
     * Creates a batch submitted event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param totalRequests total number of requests in the batch
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchSubmitted(String jobId, String batchId, int totalRequests) {
        Map<String, Object> data = Map.of(
            "totalRequests", totalRequests,
            "status", "SUBMITTED"
        );
        return new BatchNotificationEvent("batch.submitted", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch processing event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchProcessing(String jobId, String batchId, 
                                                       int completedRequests, int failedRequests, int totalRequests) {
        Map<String, Object> data = Map.of(
            "completedRequests", completedRequests,
            "failedRequests", failedRequests,
            "totalRequests", totalRequests,
            "status", "PROCESSING",
            "progressPercentage", totalRequests > 0 ? (double)(completedRequests + failedRequests) / totalRequests * 100 : 0.0
        );
        return new BatchNotificationEvent("batch.processing", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch progress event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @param partialResults any partial results available
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchProgress(String jobId, String batchId, 
                                                     int completedRequests, int failedRequests, int totalRequests,
                                                     Map<String, Object> partialResults) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("completedRequests", completedRequests);
        data.put("failedRequests", failedRequests);
        data.put("totalRequests", totalRequests);
        data.put("status", "PROCESSING");
        data.put("progressPercentage", totalRequests > 0 ? (double)(completedRequests + failedRequests) / totalRequests * 100 : 0.0);
        
        if (partialResults != null && !partialResults.isEmpty()) {
            data.put("partialResults", partialResults);
        }
        
        return new BatchNotificationEvent("batch.progress", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch completed event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @param metrics batch processing metrics
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchCompleted(String jobId, String batchId, 
                                                      int completedRequests, int failedRequests, int totalRequests,
                                                      BatchMetrics metrics) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("completedRequests", completedRequests);
        data.put("failedRequests", failedRequests);
        data.put("totalRequests", totalRequests);
        data.put("status", "COMPLETED");
        data.put("progressPercentage", 100.0);
        data.put("successRate", totalRequests > 0 ? (double)completedRequests / totalRequests * 100 : 0.0);
        
        if (metrics != null) {
            data.put("metrics", Map.of(
                "durationSeconds", metrics.getDurationSeconds(),
                "averageLatencyMs", metrics.averageLatencyMs(),
                "requestsPerSecond", metrics.requestsPerSecond(),
                "totalCostDollars", metrics.getTotalCostDollars(),
                "totalTokensUsed", metrics.totalTokensUsed()
            ));
        }
        
        return new BatchNotificationEvent("batch.completed", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch failed event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests before failure
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @param errorMessage error message
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchFailed(String jobId, String batchId, 
                                                   int completedRequests, int failedRequests, int totalRequests,
                                                   String errorMessage) {
        Map<String, Object> data = Map.of(
            "completedRequests", completedRequests,
            "failedRequests", failedRequests,
            "totalRequests", totalRequests,
            "status", "FAILED",
            "progressPercentage", totalRequests > 0 ? (double)(completedRequests + failedRequests) / totalRequests * 100 : 0.0,
            "errorMessage", errorMessage != null ? errorMessage : "Unknown error"
        );
        return new BatchNotificationEvent("batch.failed", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch cancelled event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests before cancellation
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchCancelled(String jobId, String batchId, 
                                                      int completedRequests, int failedRequests, int totalRequests) {
        Map<String, Object> data = Map.of(
            "completedRequests", completedRequests,
            "failedRequests", failedRequests,
            "totalRequests", totalRequests,
            "status", "CANCELLED",
            "progressPercentage", totalRequests > 0 ? (double)(completedRequests + failedRequests) / totalRequests * 100 : 0.0
        );
        return new BatchNotificationEvent("batch.cancelled", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Creates a batch timeout event.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param completedRequests number of completed requests before timeout
     * @param failedRequests number of failed requests
     * @param totalRequests total number of requests
     * @param timeoutMinutes timeout duration in minutes
     * @return new BatchNotificationEvent
     */
    public static BatchNotificationEvent batchTimeout(String jobId, String batchId, 
                                                    int completedRequests, int failedRequests, int totalRequests,
                                                    int timeoutMinutes) {
        Map<String, Object> data = Map.of(
            "completedRequests", completedRequests,
            "failedRequests", failedRequests,
            "totalRequests", totalRequests,
            "status", "TIMEOUT",
            "progressPercentage", totalRequests > 0 ? (double)(completedRequests + failedRequests) / totalRequests * 100 : 0.0,
            "timeoutMinutes", timeoutMinutes
        );
        return new BatchNotificationEvent("batch.timeout", jobId, batchId, Instant.now(), data);
    }
    
    /**
     * Converts this batch event to a general NotificationEvent.
     * 
     * @return equivalent NotificationEvent
     */
    public NotificationEvent toNotificationEvent() {
        Map<String, Object> mergedData = new java.util.HashMap<>(data);
        mergedData.put("batchId", batchId);
        return new NotificationEvent(type, jobId, timestamp, mergedData);
    }
    
    /**
     * Gets the batch status from the event data.
     * 
     * @return batch status string, or "UNKNOWN" if not found
     */
    public String getStatus() {
        Object status = data.get("status");
        return status != null ? status.toString() : "UNKNOWN";
    }
    
    /**
     * Gets the progress percentage from the event data.
     * 
     * @return progress percentage (0.0 to 100.0), or 0.0 if not found
     */
    public double getProgressPercentage() {
        Object progress = data.get("progressPercentage");
        if (progress instanceof Number) {
            return ((Number) progress).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Gets the number of completed requests from the event data.
     * 
     * @return completed requests count, or 0 if not found
     */
    public int getCompletedRequests() {
        Object completed = data.get("completedRequests");
        if (completed instanceof Number) {
            return ((Number) completed).intValue();
        }
        return 0;
    }
    
    /**
     * Gets the number of failed requests from the event data.
     * 
     * @return failed requests count, or 0 if not found
     */
    public int getFailedRequests() {
        Object failed = data.get("failedRequests");
        if (failed instanceof Number) {
            return ((Number) failed).intValue();
        }
        return 0;
    }
    
    /**
     * Gets the total number of requests from the event data.
     * 
     * @return total requests count, or 0 if not found
     */
    public int getTotalRequests() {
        Object total = data.get("totalRequests");
        if (total instanceof Number) {
            return ((Number) total).intValue();
        }
        return 0;
    }
    
    /**
     * Gets the error message from the event data.
     * 
     * @return error message, or null if not found
     */
    public String getErrorMessage() {
        Object error = data.get("errorMessage");
        return error != null ? error.toString() : null;
    }
    
    /**
     * Checks if this event indicates a terminal state.
     * 
     * @return true if the batch is in a terminal state
     */
    public boolean isTerminal() {
        String status = getStatus();
        return "COMPLETED".equals(status) || "FAILED".equals(status) || 
               "CANCELLED".equals(status) || "TIMEOUT".equals(status);
    }
    
    /**
     * Checks if this event indicates a successful completion.
     * 
     * @return true if the batch completed successfully
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(getStatus());
    }
    
    /**
     * Converts this event to a JSON-like map for serialization.
     * 
     * @return map representation suitable for JSON serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("type", type);
        map.put("jobId", jobId);
        map.put("batchId", batchId);
        map.put("timestamp", timestamp.toEpochMilli());
        
        if (!data.isEmpty()) {
            map.put("data", data);
        }
        
        return map;
    }
    
    /**
     * Gets a summary of this batch event for logging.
     * 
     * @return summary string
     */
    public String getSummary() {
        return String.format("%s[%s/%s] %s at %s - %d/%d requests (%.1f%%)", 
                           type, jobId, batchId, getStatus(), timestamp,
                           getCompletedRequests() + getFailedRequests(), getTotalRequests(),
                           getProgressPercentage());
    }
}