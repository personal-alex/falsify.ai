package ai.falsify.crawlers.common.exception;

/**
 * Exception thrown when database persistence operations fail during crawling.
 * This includes save failures, connection issues, and constraint violations.
 */
public class PersistenceException extends CrawlingException {

    private final String operation;
    private final String entityType;
    private final Object entityId;

    public PersistenceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.operation = null;
        this.entityType = null;
        this.entityId = null;
    }

    public PersistenceException(ErrorCode errorCode, String message, String operation) {
        super(errorCode, message, operation);
        this.operation = operation;
        this.entityType = null;
        this.entityId = null;
    }

    public PersistenceException(ErrorCode errorCode, String message, String operation, 
                              String entityType, Object entityId) {
        super(errorCode, message, operation);
        this.operation = operation;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public PersistenceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.operation = null;
        this.entityType = null;
        this.entityId = null;
    }

    public PersistenceException(ErrorCode errorCode, String message, String operation, Throwable cause) {
        super(errorCode, message, operation, cause);
        this.operation = operation;
        this.entityType = null;
        this.entityId = null;
    }

    public PersistenceException(ErrorCode errorCode, String message, String operation, 
                              String entityType, Object entityId, Throwable cause) {
        super(errorCode, message, operation, cause);
        this.operation = operation;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PersistenceException[")
          .append("code=").append(getErrorCode());
        
        if (operation != null) {
            sb.append(", operation=").append(operation);
        }
        
        if (entityType != null) {
            sb.append(", entityType=").append(entityType);
        }
        
        if (entityId != null) {
            sb.append(", entityId=").append(entityId);
        }
        
        sb.append(", message=").append(getMessage())
          .append("]");
        
        return sb.toString();
    }

    // Convenience factory methods for common persistence errors
    public static PersistenceException saveFailed(String entityType, Object entityId, Throwable cause) {
        return new PersistenceException(
            ErrorCode.PERSISTENCE_SAVE_FAILED,
            String.format("Failed to save %s with id %s", entityType, entityId),
            "save",
            entityType,
            entityId,
            cause
        );
    }

    public static PersistenceException duplicateKey(String entityType, Object entityId, Throwable cause) {
        return new PersistenceException(
            ErrorCode.PERSISTENCE_DUPLICATE_KEY,
            String.format("Duplicate key constraint violation for %s with id %s", entityType, entityId),
            "save",
            entityType,
            entityId,
            cause
        );
    }

    public static PersistenceException connectionFailed(Throwable cause) {
        return new PersistenceException(
            ErrorCode.PERSISTENCE_CONNECTION_FAILED,
            "Database connection failed",
            "connect",
            cause
        );
    }
}