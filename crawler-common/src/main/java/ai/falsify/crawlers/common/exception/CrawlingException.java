package ai.falsify.crawlers.common.exception;

/**
 * Base exception for all crawler-related errors.
 * Provides error codes and structured error information for better error handling.
 */
public class CrawlingException extends Exception {

    private final ErrorCode errorCode;
    private final String context;

    public CrawlingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public CrawlingException(ErrorCode errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public CrawlingException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }

    public CrawlingException(ErrorCode errorCode, String message, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getContext() {
        return context;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName())
          .append("[")
          .append("code=").append(errorCode)
          .append(", message=").append(getMessage());
        
        if (context != null) {
            sb.append(", context=").append(context);
        }
        
        sb.append("]");
        return sb.toString();
    }

    /**
     * Error codes for different types of crawling failures
     */
    public enum ErrorCode {
        // Network related errors
        NETWORK_CONNECTION_FAILED("NET001", "Failed to establish network connection"),
        NETWORK_TIMEOUT("NET002", "Network operation timed out"),
        NETWORK_INVALID_RESPONSE("NET003", "Invalid or unexpected network response"),
        
        // Content related errors
        CONTENT_VALIDATION_FAILED("CNT001", "Content validation failed"),
        CONTENT_PARSING_FAILED("CNT002", "Failed to parse content"),
        CONTENT_TOO_SHORT("CNT003", "Content length below minimum threshold"),
        CONTENT_TOO_LONG("CNT004", "Content length exceeds maximum threshold"),
        
        // Persistence related errors
        PERSISTENCE_SAVE_FAILED("PER001", "Failed to save data to database"),
        PERSISTENCE_DUPLICATE_KEY("PER002", "Duplicate key constraint violation"),
        PERSISTENCE_CONNECTION_FAILED("PER003", "Database connection failed"),
        
        // Configuration related errors
        CONFIG_INVALID_VALUE("CFG001", "Invalid configuration value"),
        CONFIG_MISSING_REQUIRED("CFG002", "Required configuration missing"),
        
        // General errors
        UNKNOWN_ERROR("GEN001", "Unknown error occurred"),
        OPERATION_CANCELLED("GEN002", "Operation was cancelled"),
        RESOURCE_EXHAUSTED("GEN003", "System resources exhausted");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }
}