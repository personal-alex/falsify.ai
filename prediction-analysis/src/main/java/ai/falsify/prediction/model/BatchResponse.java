package ai.falsify.prediction.model;

/**
 * Record representing a single response from a GenAI batch request.
 * Contains the request ID, response content, success status, and error information.
 */
public record BatchResponse(
    String requestId,
    String response,
    boolean success,
    String errorMessage
) {
    
    /**
     * Creates a BatchResponse with validation.
     * 
     * @param requestId the request identifier this response corresponds to (required)
     * @param response the response content from the GenAI API (optional for failed requests)
     * @param success whether the request was successful
     * @param errorMessage error message if the request failed (optional)
     */
    public BatchResponse {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        
        // Sanitize text fields
        requestId = requestId.trim();
        response = sanitizeResponse(response);
        errorMessage = sanitizeErrorMessage(errorMessage);
        
        // Validation: successful responses should have content, failed ones should have error message
        if (success && (response == null || response.trim().isEmpty())) {
            throw new IllegalArgumentException("Successful responses must have response content");
        }
        
        if (!success && (errorMessage == null || errorMessage.trim().isEmpty())) {
            throw new IllegalArgumentException("Failed responses must have an error message");
        }
    }
    
    /**
     * Creates a successful BatchResponse.
     * 
     * @param requestId the request identifier
     * @param response the response content
     * @return new successful BatchResponse
     */
    public static BatchResponse success(String requestId, String response) {
        return new BatchResponse(requestId, response, true, null);
    }
    
    /**
     * Creates a failed BatchResponse.
     * 
     * @param requestId the request identifier
     * @param errorMessage the error message
     * @return new failed BatchResponse
     */
    public static BatchResponse failure(String requestId, String errorMessage) {
        return new BatchResponse(requestId, null, false, errorMessage);
    }
    
    /**
     * Creates a failed BatchResponse with partial response content.
     * 
     * @param requestId the request identifier
     * @param partialResponse partial response content that was received
     * @param errorMessage the error message
     * @return new failed BatchResponse with partial content
     */
    public static BatchResponse partialFailure(String requestId, String partialResponse, String errorMessage) {
        return new BatchResponse(requestId, partialResponse, false, errorMessage);
    }
    
    /**
     * Gets the response content, or empty string if null.
     * 
     * @return response content or empty string
     */
    public String getResponseOrEmpty() {
        return response != null ? response : "";
    }
    
    /**
     * Gets the error message, or empty string if null.
     * 
     * @return error message or empty string
     */
    public String getErrorMessageOrEmpty() {
        return errorMessage != null ? errorMessage : "";
    }
    
    /**
     * Gets the length of the response content.
     * 
     * @return response length, or 0 if no response
     */
    public int getResponseLength() {
        return response != null ? response.length() : 0;
    }
    
    /**
     * Checks if this response has content (successful or partial).
     * 
     * @return true if response content is available
     */
    public boolean hasContent() {
        return response != null && !response.trim().isEmpty();
    }
    
    /**
     * Checks if this is a partial response (has content but failed).
     * 
     * @return true if response has content but is marked as failed
     */
    public boolean isPartialResponse() {
        return !success && hasContent();
    }
    
    /**
     * Checks if this response is empty (no content and no error).
     * 
     * @return true if both response and error message are empty
     */
    public boolean isEmpty() {
        return !hasContent() && (errorMessage == null || errorMessage.trim().isEmpty());
    }
    
    /**
     * Gets a summary of this response for logging.
     * 
     * @return summary string
     */
    public String getSummary() {
        if (success) {
            return String.format("Success: %d chars", getResponseLength());
        } else if (isPartialResponse()) {
            return String.format("Partial: %d chars, Error: %s", getResponseLength(), 
                               truncateForSummary(errorMessage, 50));
        } else {
            return String.format("Failed: %s", truncateForSummary(errorMessage, 100));
        }
    }
    
    /**
     * Sanitizes response content to prevent security issues.
     * 
     * @param response the response to sanitize
     * @return sanitized response or null if input was null
     */
    private static String sanitizeResponse(String response) {
        if (response == null) {
            return null;
        }
        
        String sanitized = response.trim();
        
        // Remove null characters and other dangerous control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Limit maximum length to prevent memory issues
        if (sanitized.length() > 100000) {
            sanitized = sanitized.substring(0, 99997) + "...";
        }
        
        return sanitized.isEmpty() ? null : sanitized;
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
        if (sanitized.length() > 2000) {
            sanitized = sanitized.substring(0, 1997) + "...";
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }
    
    /**
     * Truncates text for summary display.
     * 
     * @param text the text to truncate
     * @param maxLength maximum length
     * @return truncated text
     */
    private static String truncateForSummary(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}