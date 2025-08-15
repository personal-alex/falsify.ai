package ai.falsify.prediction.model;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Record representing the complete results of a GenAI batch processing operation.
 * Contains all responses, metrics, and aggregated information about the batch.
 */
public record BatchResults(
    String batchId,
    Map<String, BatchResponse> responses,
    BatchMetrics metrics
) {
    
    /**
     * Creates BatchResults with validation.
     * 
     * @param batchId the GenAI batch identifier (required)
     * @param responses map of request ID to response (required)
     * @param metrics performance metrics for the batch (required)
     */
    public BatchResults {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }
        
        if (responses == null) {
            throw new IllegalArgumentException("Responses cannot be null");
        }
        
        if (metrics == null) {
            throw new IllegalArgumentException("Metrics cannot be null");
        }
        
        // Sanitize batch ID
        batchId = batchId.trim();
        
        // Validate that metrics match response counts
        int successCount = (int) responses.values().stream().filter(BatchResponse::success).count();
        int failureCount = responses.size() - successCount;
        
        if (metrics.successfulRequests() != successCount) {
            throw new IllegalArgumentException(
                "Metrics successful requests (" + metrics.successfulRequests() + 
                ") does not match actual successful responses (" + successCount + ")"
            );
        }
        
        if (metrics.failedRequests() != failureCount) {
            throw new IllegalArgumentException(
                "Metrics failed requests (" + metrics.failedRequests() + 
                ") does not match actual failed responses (" + failureCount + ")"
            );
        }
    }
    
    /**
     * Creates BatchResults for a completed batch.
     * 
     * @param batchId the batch identifier
     * @param responses the response map
     * @param metrics the performance metrics
     * @return new BatchResults
     */
    public static BatchResults completed(String batchId, Map<String, BatchResponse> responses, 
                                       BatchMetrics metrics) {
        return new BatchResults(batchId, responses, metrics);
    }
    
    /**
     * Creates empty BatchResults for initialization.
     * 
     * @param batchId the batch identifier
     * @param metrics the initial metrics
     * @return new empty BatchResults
     */
    public static BatchResults empty(String batchId, BatchMetrics metrics) {
        return new BatchResults(batchId, Map.of(), metrics);
    }
    
    /**
     * Gets the total number of responses.
     * 
     * @return total response count
     */
    public int getTotalResponses() {
        return responses.size();
    }
    
    /**
     * Gets the number of successful responses.
     * 
     * @return successful response count
     */
    public int getSuccessfulResponses() {
        return (int) responses.values().stream().filter(BatchResponse::success).count();
    }
    
    /**
     * Gets the number of failed responses.
     * 
     * @return failed response count
     */
    public int getFailedResponses() {
        return getTotalResponses() - getSuccessfulResponses();
    }
    
    /**
     * Gets the number of partial responses (failed but with content).
     * 
     * @return partial response count
     */
    public int getPartialResponses() {
        return (int) responses.values().stream().filter(BatchResponse::isPartialResponse).count();
    }
    
    /**
     * Gets all successful responses.
     * 
     * @return map of successful responses
     */
    public Map<String, BatchResponse> getSuccessfulResponsesMap() {
        return responses.entrySet().stream()
                .filter(entry -> entry.getValue().success())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Gets all failed responses.
     * 
     * @return map of failed responses
     */
    public Map<String, BatchResponse> getFailedResponsesMap() {
        return responses.entrySet().stream()
                .filter(entry -> !entry.getValue().success())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Gets all partial responses (failed but with content).
     * 
     * @return map of partial responses
     */
    public Map<String, BatchResponse> getPartialResponsesMap() {
        return responses.entrySet().stream()
                .filter(entry -> entry.getValue().isPartialResponse())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Gets a response by request ID.
     * 
     * @param requestId the request identifier
     * @return the response, or null if not found
     */
    public BatchResponse getResponse(String requestId) {
        return responses.get(requestId);
    }
    
    /**
     * Checks if a response exists for the given request ID.
     * 
     * @param requestId the request identifier
     * @return true if response exists
     */
    public boolean hasResponse(String requestId) {
        return responses.containsKey(requestId);
    }
    
    /**
     * Checks if the batch has any successful responses.
     * 
     * @return true if there are successful responses
     */
    public boolean hasSuccessfulResponses() {
        return responses.values().stream().anyMatch(BatchResponse::success);
    }
    
    /**
     * Checks if the batch has any failed responses.
     * 
     * @return true if there are failed responses
     */
    public boolean hasFailedResponses() {
        return responses.values().stream().anyMatch(response -> !response.success());
    }
    
    /**
     * Checks if the batch has any partial responses.
     * 
     * @return true if there are partial responses
     */
    public boolean hasPartialResponses() {
        return responses.values().stream().anyMatch(BatchResponse::isPartialResponse);
    }
    
    /**
     * Checks if all responses were successful.
     * 
     * @return true if all responses succeeded
     */
    public boolean isCompletelySuccessful() {
        return !responses.isEmpty() && responses.values().stream().allMatch(BatchResponse::success);
    }
    
    /**
     * Checks if all responses failed.
     * 
     * @return true if all responses failed
     */
    public boolean isCompletelyFailed() {
        return !responses.isEmpty() && responses.values().stream().noneMatch(BatchResponse::success);
    }
    
    /**
     * Checks if the batch is empty (no responses).
     * 
     * @return true if no responses
     */
    public boolean isEmpty() {
        return responses.isEmpty();
    }
    
    /**
     * Gets the success rate as a percentage (0.0 to 1.0).
     * 
     * @return success rate
     */
    public double getSuccessRate() {
        if (responses.isEmpty()) {
            return 0.0;
        }
        return (double) getSuccessfulResponses() / getTotalResponses();
    }
    
    /**
     * Gets the total content length of all responses.
     * 
     * @return total content length in characters
     */
    public int getTotalContentLength() {
        return responses.values().stream()
                .mapToInt(BatchResponse::getResponseLength)
                .sum();
    }
    
    /**
     * Gets the average content length per response.
     * 
     * @return average content length
     */
    public double getAverageContentLength() {
        if (responses.isEmpty()) {
            return 0.0;
        }
        return (double) getTotalContentLength() / getTotalResponses();
    }
    
    /**
     * Gets a summary of the batch results.
     * 
     * @return formatted summary string
     */
    public String getSummary() {
        return String.format(
            "Batch %s: %d responses (%d successful, %d failed, %d partial), %.1f%% success rate",
            batchId,
            getTotalResponses(),
            getSuccessfulResponses(),
            getFailedResponses(),
            getPartialResponses(),
            getSuccessRate() * 100
        );
    }
    
    /**
     * Gets detailed results information.
     * 
     * @return detailed results string
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Batch Results for ").append(batchId).append(":\n");
        sb.append("  Total Responses: ").append(getTotalResponses()).append("\n");
        sb.append("  Successful: ").append(getSuccessfulResponses()).append(" (").append(String.format("%.1f%%", getSuccessRate() * 100)).append(")\n");
        sb.append("  Failed: ").append(getFailedResponses()).append("\n");
        sb.append("  Partial: ").append(getPartialResponses()).append("\n");
        sb.append("  Total Content: ").append(getTotalContentLength()).append(" chars (").append(String.format("%.1f", getAverageContentLength())).append(" avg)\n");
        sb.append("  ").append(metrics.getSummary());
        
        return sb.toString();
    }
    
    /**
     * Gets error summary for failed responses.
     * 
     * @return error summary string
     */
    public String getErrorSummary() {
        Map<String, Long> errorCounts = responses.values().stream()
                .filter(response -> !response.success())
                .collect(Collectors.groupingBy(
                    response -> response.getErrorMessageOrEmpty().length() > 50 
                        ? response.getErrorMessageOrEmpty().substring(0, 50) + "..." 
                        : response.getErrorMessageOrEmpty(),
                    Collectors.counting()
                ));
        
        if (errorCounts.isEmpty()) {
            return "No errors";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Error Summary:\n");
        errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> sb.append("  ").append(entry.getValue()).append("x: ").append(entry.getKey()).append("\n"));
        
        return sb.toString();
    }
}