package ai.falsify.crawlers.common.exception;

/**
 * Exception thrown when content validation fails during crawling.
 * This includes content that is too short, too long, malformed, or missing required fields.
 */
public class ContentValidationException extends CrawlingException {

    private final String fieldName;
    private final Object actualValue;
    private final Object expectedValue;

    public ContentValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.fieldName = null;
        this.actualValue = null;
        this.expectedValue = null;
    }

    public ContentValidationException(ErrorCode errorCode, String message, String fieldName) {
        super(errorCode, message, fieldName);
        this.fieldName = fieldName;
        this.actualValue = null;
        this.expectedValue = null;
    }

    public ContentValidationException(ErrorCode errorCode, String message, String fieldName, 
                                    Object actualValue, Object expectedValue) {
        super(errorCode, message, fieldName);
        this.fieldName = fieldName;
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
    }

    public ContentValidationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.fieldName = null;
        this.actualValue = null;
        this.expectedValue = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getActualValue() {
        return actualValue;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContentValidationException[")
          .append("code=").append(getErrorCode());
        
        if (fieldName != null) {
            sb.append(", field=").append(fieldName);
        }
        
        if (actualValue != null) {
            sb.append(", actual=").append(actualValue);
        }
        
        if (expectedValue != null) {
            sb.append(", expected=").append(expectedValue);
        }
        
        sb.append(", message=").append(getMessage())
          .append("]");
        
        return sb.toString();
    }

    // Convenience factory methods for common validation errors
    public static ContentValidationException contentTooShort(int actualLength, int minLength) {
        return new ContentValidationException(
            ErrorCode.CONTENT_TOO_SHORT,
            String.format("Content length %d is below minimum %d", actualLength, minLength),
            "content.length",
            actualLength,
            minLength
        );
    }

    public static ContentValidationException contentTooLong(int actualLength, int maxLength) {
        return new ContentValidationException(
            ErrorCode.CONTENT_TOO_LONG,
            String.format("Content length %d exceeds maximum %d", actualLength, maxLength),
            "content.length",
            actualLength,
            maxLength
        );
    }

    public static ContentValidationException missingRequiredField(String fieldName) {
        return new ContentValidationException(
            ErrorCode.CONTENT_VALIDATION_FAILED,
            "Required field is missing or empty: " + fieldName,
            fieldName
        );
    }

    public static ContentValidationException parsingFailed(String fieldName, Throwable cause) {
        return new ContentValidationException(
            ErrorCode.CONTENT_PARSING_FAILED,
            "Failed to parse field: " + fieldName,
            cause
        );
    }
}