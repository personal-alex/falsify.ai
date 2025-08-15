package ai.falsify.prediction.service;

/**
 * Base exception for GenAI SDK operations.
 * Wraps GenAI SDK exceptions with additional context and error handling capabilities.
 */
public class GenAIException extends RuntimeException {

    private final String operation;
    private final String batchId;
    private final boolean retryable;

    public GenAIException(String message) {
        super(message);
        this.operation = null;
        this.batchId = null;
        this.retryable = false;
    }

    public GenAIException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
        this.batchId = null;
        this.retryable = isRetryableException(cause);
    }

    public GenAIException(String message, String operation, String batchId, boolean retryable) {
        super(message);
        this.operation = operation;
        this.batchId = batchId;
        this.retryable = retryable;
    }

    public GenAIException(String message, Throwable cause, String operation, String batchId, boolean retryable) {
        super(message, cause);
        this.operation = operation;
        this.batchId = batchId;
        this.retryable = retryable;
    }

    public String getOperation() {
        return operation;
    }

    public String getBatchId() {
        return batchId;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Determines if an exception is retryable based on its type and message.
     * 
     * @param cause the underlying exception
     * @return true if the exception is retryable
     */
    private static boolean isRetryableException(Throwable cause) {
        if (cause == null) {
            return false;
        }

        String message = cause.getMessage();
        if (message == null) {
            message = "";
        }
        message = message.toLowerCase();

        // Network-related errors are usually retryable
        if (cause instanceof java.net.SocketTimeoutException ||
            cause instanceof java.net.ConnectException ||
            cause instanceof java.io.IOException) {
            return true;
        }

        // HTTP status codes that are retryable
        if (message.contains("429") || // Rate limited
            message.contains("500") || // Internal server error
            message.contains("502") || // Bad gateway
            message.contains("503") || // Service unavailable
            message.contains("504")) { // Gateway timeout
            return true;
        }

        // Specific error messages that indicate retryable conditions
        if (message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("network") ||
            message.contains("temporary") ||
            message.contains("rate limit")) {
            return true;
        }

        return false;
    }

    /**
     * Creates a GenAI exception for batch submission failures.
     * 
     * @param cause the underlying exception
     * @param requestCount the number of requests in the batch
     * @return GenAIException
     */
    public static GenAIException batchSubmissionFailed(Throwable cause, int requestCount) {
        String message = String.format("Failed to submit batch with %d requests: %s", 
                                     requestCount, cause.getMessage());
        return new GenAIException(message, cause, "submitBatch", null, isRetryableException(cause));
    }

    /**
     * Creates a GenAI exception for batch status check failures.
     * 
     * @param cause the underlying exception
     * @param batchId the batch identifier
     * @return GenAIException
     */
    public static GenAIException batchStatusFailed(Throwable cause, String batchId) {
        String message = String.format("Failed to check status for batch %s: %s", 
                                     batchId, cause.getMessage());
        return new GenAIException(message, cause, "getBatchStatus", batchId, isRetryableException(cause));
    }

    /**
     * Creates a GenAI exception for batch result retrieval failures.
     * 
     * @param cause the underlying exception
     * @param batchId the batch identifier
     * @return GenAIException
     */
    public static GenAIException batchResultsFailed(Throwable cause, String batchId) {
        String message = String.format("Failed to retrieve results for batch %s: %s", 
                                     batchId, cause.getMessage());
        return new GenAIException(message, cause, "getBatchResults", batchId, isRetryableException(cause));
    }

    /**
     * Creates a GenAI exception for batch cancellation failures.
     * 
     * @param cause the underlying exception
     * @param batchId the batch identifier
     * @return GenAIException
     */
    public static GenAIException batchCancellationFailed(Throwable cause, String batchId) {
        String message = String.format("Failed to cancel batch %s: %s", 
                                     batchId, cause.getMessage());
        return new GenAIException(message, cause, "cancelBatch", batchId, isRetryableException(cause));
    }

    /**
     * Creates a GenAI exception for rate limiting.
     * 
     * @param retryAfterSeconds seconds to wait before retry
     * @param operation the operation that was rate limited
     * @return GenAIException
     */
    public static GenAIException rateLimited(int retryAfterSeconds, String operation) {
        String message = String.format("Rate limited for operation %s, retry after %d seconds", 
                                     operation, retryAfterSeconds);
        return new GenAIException(message, operation, null, true);
    }

    /**
     * Creates a GenAI exception for unsupported operations.
     * 
     * @param operation the unsupported operation
     * @param reason the reason why it's unsupported
     * @return GenAIException
     */
    public static GenAIException unsupportedOperation(String operation, String reason) {
        String message = String.format("Operation %s is not supported: %s", operation, reason);
        return new GenAIException(message, operation, null, false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GenAIException{");
        sb.append("message='").append(getMessage()).append("'");
        if (operation != null) {
            sb.append(", operation='").append(operation).append("'");
        }
        if (batchId != null) {
            sb.append(", batchId='").append(batchId).append("'");
        }
        sb.append(", retryable=").append(retryable);
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName());
        }
        sb.append("}");
        return sb.toString();
    }
}