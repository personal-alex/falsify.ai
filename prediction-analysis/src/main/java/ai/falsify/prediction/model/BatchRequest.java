package ai.falsify.prediction.model;

import java.util.Map;

/**
 * Record representing a single request within a GenAI batch.
 * Contains the request ID, article ID, prompt, and parameters for processing.
 */
public record BatchRequest(
    String requestId,
    String articleId,
    String prompt,
    Map<String, Object> parameters
) {
    
    /**
     * Creates a BatchRequest with validation.
     * 
     * @param requestId unique identifier for this request within the batch (required)
     * @param articleId the article identifier being processed (required)
     * @param prompt the prompt text to send to the GenAI API (required)
     * @param parameters additional parameters for the API call (optional)
     */
    public BatchRequest {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        
        if (articleId == null || articleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Article ID cannot be null or empty");
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        // Sanitize text fields
        requestId = requestId.trim();
        articleId = articleId.trim();
        prompt = sanitizePrompt(prompt);
        
        // Ensure parameters is not null
        if (parameters == null) {
            parameters = Map.of();
        }
    }
    
    /**
     * Creates a simple BatchRequest with just the required fields.
     * 
     * @param requestId the request identifier
     * @param articleId the article identifier
     * @param prompt the prompt text
     * @return new BatchRequest
     */
    public static BatchRequest simple(String requestId, String articleId, String prompt) {
        return new BatchRequest(requestId, articleId, prompt, Map.of());
    }
    
    /**
     * Creates a BatchRequest with parameters.
     * 
     * @param requestId the request identifier
     * @param articleId the article identifier
     * @param prompt the prompt text
     * @param parameters the API parameters
     * @return new BatchRequest
     */
    public static BatchRequest withParameters(String requestId, String articleId, String prompt, 
                                            Map<String, Object> parameters) {
        return new BatchRequest(requestId, articleId, prompt, parameters);
    }
    
    /**
     * Gets a parameter value by key.
     * 
     * @param key the parameter key
     * @return the parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Gets a parameter value as a string.
     * 
     * @param key the parameter key
     * @param defaultValue the default value if not found
     * @return the parameter value as string, or default value
     */
    public String getParameterAsString(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Gets a parameter value as an integer.
     * 
     * @param key the parameter key
     * @param defaultValue the default value if not found or not a number
     * @return the parameter value as integer, or default value
     */
    public Integer getParameterAsInteger(String key, Integer defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Checks if the request has a specific parameter.
     * 
     * @param key the parameter key
     * @return true if the parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    /**
     * Gets the size of the prompt in characters.
     * 
     * @return prompt length
     */
    public int getPromptLength() {
        return prompt.length();
    }
    
    /**
     * Checks if this is a large prompt (over 10,000 characters).
     * 
     * @return true if prompt is considered large
     */
    public boolean isLargePrompt() {
        return prompt.length() > 10000;
    }
    
    /**
     * Sanitizes the prompt to prevent security issues and ensure valid format.
     * 
     * @param prompt the prompt to sanitize
     * @return sanitized prompt
     */
    private static String sanitizePrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        
        String sanitized = prompt.trim();
        
        // Remove null characters and other control characters except newlines and tabs
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Limit maximum length to prevent excessive API costs
        if (sanitized.length() > 50000) {
            sanitized = sanitized.substring(0, 49997) + "...";
        }
        
        return sanitized;
    }
}