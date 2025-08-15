package ai.falsify.prediction.model;

import java.time.Instant;

/**
 * Record representing the status of a GenAI batch job.
 * Contains all information needed to track and monitor batch processing progress.
 */
public record BatchJobStatus(
    String jobId,
    String batchId,
    BatchState state,
    int totalRequests,
    int completedRequests,
    int failedRequests,
    Instant createdAt,
    Instant updatedAt,
    String errorMessage
) {
    
    /**
     * Creates a BatchJobStatus with validation.
     * 
     * @param jobId the unique job identifier (required)
     * @param batchId the GenAI batch identifier (required)
     * @param state the current batch state (required)
     * @param totalRequests total number of requests in the batch (must be >= 0)
     * @param completedRequests number of completed requests (must be >= 0)
     * @param failedRequests number of failed requests (must be >= 0)
     * @param createdAt when the job was created (required)
     * @param updatedAt when the job was last updated (required)
     * @param errorMessage error message if job failed (optional)
     */
    public BatchJobStatus {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("Job ID cannot be null or empty");
        }
        
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }
        
        if (state == null) {
            throw new IllegalArgumentException("Batch state cannot be null");
        }
        
        if (totalRequests < 0) {
            throw new IllegalArgumentException("Total requests cannot be negative: " + totalRequests);
        }
        
        if (completedRequests < 0) {
            throw new IllegalArgumentException("Completed requests cannot be negative: " + completedRequests);
        }
        
        if (failedRequests < 0) {
            throw new IllegalArgumentException("Failed requests cannot be negative: " + failedRequests);
        }
        
        if (completedRequests + failedRequests > totalRequests) {
            throw new IllegalArgumentException(
                "Completed + failed requests (" + (completedRequests + failedRequests) + 
                ") cannot exceed total requests (" + totalRequests + ")"
            );
        }
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp cannot be null");
        }
        
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated timestamp cannot be before created timestamp");
        }
        
        // Sanitize text fields
        jobId = jobId.trim();
        batchId = batchId.trim();
        errorMessage = sanitizeErrorMessage(errorMessage);
    }
    
    /**
     * Creates a new BatchJobStatus for a submitted job.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param totalRequests total number of requests
     * @return new BatchJobStatus in SUBMITTED state
     */
    public static BatchJobStatus submitted(String jobId, String batchId, int totalRequests) {
        Instant now = Instant.now();
        return new BatchJobStatus(jobId, batchId, BatchState.SUBMITTED, totalRequests, 0, 0, now, now, null);
    }
    
    /**
     * Creates a new BatchJobStatus for a processing job.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param totalRequests total number of requests
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @return new BatchJobStatus in PROCESSING state
     */
    public static BatchJobStatus processing(String jobId, String batchId, int totalRequests, 
                                          int completedRequests, int failedRequests) {
        Instant now = Instant.now();
        return new BatchJobStatus(jobId, batchId, BatchState.PROCESSING, totalRequests, 
                                completedRequests, failedRequests, now, now, null);
    }
    
    /**
     * Creates a new BatchJobStatus for a completed job.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param totalRequests total number of requests
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @return new BatchJobStatus in COMPLETED state
     */
    public static BatchJobStatus completed(String jobId, String batchId, int totalRequests, 
                                         int completedRequests, int failedRequests) {
        Instant now = Instant.now();
        return new BatchJobStatus(jobId, batchId, BatchState.COMPLETED, totalRequests, 
                                completedRequests, failedRequests, now, now, null);
    }
    
    /**
     * Creates a new BatchJobStatus for a failed job.
     * 
     * @param jobId the job identifier
     * @param batchId the batch identifier
     * @param totalRequests total number of requests
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @param errorMessage the error message
     * @return new BatchJobStatus in FAILED state
     */
    public static BatchJobStatus failed(String jobId, String batchId, int totalRequests, 
                                      int completedRequests, int failedRequests, String errorMessage) {
        Instant now = Instant.now();
        return new BatchJobStatus(jobId, batchId, BatchState.FAILED, totalRequests, 
                                completedRequests, failedRequests, now, now, errorMessage);
    }
    
    /**
     * Creates a copy of this status with updated progress.
     * 
     * @param completedRequests new completed request count
     * @param failedRequests new failed request count
     * @return new BatchJobStatus with updated progress
     */
    public BatchJobStatus withProgress(int completedRequests, int failedRequests) {
        BatchState newState = determineStateFromProgress(completedRequests, failedRequests);
        return new BatchJobStatus(jobId, batchId, newState, totalRequests, 
                                completedRequests, failedRequests, createdAt, Instant.now(), errorMessage);
    }
    
    /**
     * Creates a copy of this status with a new state.
     * 
     * @param newState the new batch state
     * @return new BatchJobStatus with updated state
     */
    public BatchJobStatus withState(BatchState newState) {
        return new BatchJobStatus(jobId, batchId, newState, totalRequests, 
                                completedRequests, failedRequests, createdAt, Instant.now(), errorMessage);
    }
    
    /**
     * Creates a copy of this status with an error message.
     * 
     * @param errorMessage the error message
     * @return new BatchJobStatus with error message and FAILED state
     */
    public BatchJobStatus withError(String errorMessage) {
        return new BatchJobStatus(jobId, batchId, BatchState.FAILED, totalRequests, 
                                completedRequests, failedRequests, createdAt, Instant.now(), errorMessage);
    }
    
    /**
     * Gets the number of pending requests (not yet processed).
     * 
     * @return number of pending requests
     */
    public int getPendingRequests() {
        return totalRequests - completedRequests - failedRequests;
    }
    
    /**
     * Gets the completion percentage (0.0 to 1.0).
     * 
     * @return completion percentage
     */
    public double getCompletionPercentage() {
        if (totalRequests == 0) {
            return 1.0;
        }
        return (double) (completedRequests + failedRequests) / totalRequests;
    }
    
    /**
     * Gets the success rate (0.0 to 1.0).
     * 
     * @return success rate based on completed vs total processed requests
     */
    public double getSuccessRate() {
        int processedRequests = completedRequests + failedRequests;
        if (processedRequests == 0) {
            return 0.0;
        }
        return (double) completedRequests / processedRequests;
    }
    
    /**
     * Checks if the job is complete (all requests processed).
     * 
     * @return true if all requests have been processed
     */
    public boolean isComplete() {
        return completedRequests + failedRequests >= totalRequests;
    }
    
    /**
     * Checks if the job has any failures.
     * 
     * @return true if there are failed requests
     */
    public boolean hasFailures() {
        return failedRequests > 0;
    }
    
    /**
     * Gets the duration of the job so far.
     * 
     * @return duration in milliseconds
     */
    public long getDurationMillis() {
        return updatedAt.toEpochMilli() - createdAt.toEpochMilli();
    }
    
    /**
     * Determines the appropriate state based on progress.
     * 
     * @param completedRequests number of completed requests
     * @param failedRequests number of failed requests
     * @return appropriate BatchState
     */
    private BatchState determineStateFromProgress(int completedRequests, int failedRequests) {
        if (completedRequests + failedRequests >= totalRequests) {
            return failedRequests == totalRequests ? BatchState.FAILED : BatchState.COMPLETED;
        } else if (completedRequests + failedRequests > 0) {
            return BatchState.PROCESSING;
        } else {
            return BatchState.SUBMITTED;
        }
    }
    
    /**
     * Sanitizes error message to prevent security issues.
     * 
     * @param errorMessage the error message to sanitize
     * @return sanitized error message or null if input was null
     */
    private static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        
        String sanitized = errorMessage.trim();
        
        // Basic HTML escaping to prevent XSS
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        // Limit length to prevent excessive memory usage
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 997) + "...";
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }
}