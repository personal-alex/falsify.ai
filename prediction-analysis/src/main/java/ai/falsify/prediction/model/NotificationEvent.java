package ai.falsify.prediction.model;

import java.time.Instant;
import java.util.Map;

/**
 * Base record for all notification events sent via WebSocket.
 * Provides common structure and serialization support for notification payloads.
 */
public record NotificationEvent(
    String type,
    String jobId,
    Instant timestamp,
    Map<String, Object> data
) {
    
    /**
     * Creates a NotificationEvent with validation.
     * 
     * @param type the event type (required)
     * @param jobId the job identifier (required)
     * @param timestamp when the event occurred (required)
     * @param data additional event data (optional)
     */
    public NotificationEvent {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("Job ID cannot be null or empty");
        }
        
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        
        // Sanitize and ensure immutability
        type = type.trim();
        jobId = jobId.trim();
        
        if (data == null) {
            data = Map.of();
        }
    }
    
    /**
     * Creates a NotificationEvent with current timestamp.
     * 
     * @param type the event type
     * @param jobId the job identifier
     * @param data additional event data
     * @return new NotificationEvent
     */
    public static NotificationEvent create(String type, String jobId, Map<String, Object> data) {
        return new NotificationEvent(type, jobId, Instant.now(), data);
    }
    
    /**
     * Creates a NotificationEvent with no additional data.
     * 
     * @param type the event type
     * @param jobId the job identifier
     * @return new NotificationEvent
     */
    public static NotificationEvent simple(String type, String jobId) {
        return new NotificationEvent(type, jobId, Instant.now(), Map.of());
    }
    
    /**
     * Gets a data value by key.
     * 
     * @param key the data key
     * @return the data value, or null if not found
     */
    public Object getData(String key) {
        return data.get(key);
    }
    
    /**
     * Gets a data value as a string.
     * 
     * @param key the data key
     * @param defaultValue the default value if not found
     * @return the data value as string, or default value
     */
    public String getDataAsString(String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Gets a data value as an integer.
     * 
     * @param key the data key
     * @param defaultValue the default value if not found or not a number
     * @return the data value as integer, or default value
     */
    public Integer getDataAsInteger(String key, Integer defaultValue) {
        Object value = data.get(key);
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
     * Gets a data value as a double.
     * 
     * @param key the data key
     * @param defaultValue the default value if not found or not a number
     * @return the data value as double, or default value
     */
    public Double getDataAsDouble(String key, Double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Checks if the event has specific data.
     * 
     * @param key the data key
     * @return true if the data exists
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Gets the timestamp as epoch milliseconds.
     * 
     * @return timestamp in milliseconds
     */
    public long getTimestampMillis() {
        return timestamp.toEpochMilli();
    }
    
    /**
     * Checks if this is a batch-related event.
     * 
     * @return true if event type starts with "batch."
     */
    public boolean isBatchEvent() {
        return type.startsWith("batch.");
    }
    
    /**
     * Checks if this is a job-related event.
     * 
     * @return true if event type starts with "job."
     */
    public boolean isJobEvent() {
        return type.startsWith("job.");
    }
    
    /**
     * Checks if this is a prediction-related event.
     * 
     * @return true if event type starts with "prediction."
     */
    public boolean isPredictionEvent() {
        return type.startsWith("prediction.");
    }
    
    /**
     * Creates a copy of this event with additional data.
     * 
     * @param additionalData additional data to merge
     * @return new NotificationEvent with merged data
     */
    public NotificationEvent withAdditionalData(Map<String, Object> additionalData) {
        Map<String, Object> mergedData = Map.of();
        if (!data.isEmpty() || !additionalData.isEmpty()) {
            mergedData = new java.util.HashMap<>(data);
            mergedData.putAll(additionalData);
        }
        return new NotificationEvent(type, jobId, timestamp, mergedData);
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
        map.put("timestamp", timestamp.toEpochMilli());
        
        if (!data.isEmpty()) {
            map.put("data", data);
        }
        
        return map;
    }
    
    /**
     * Gets a summary of this event for logging.
     * 
     * @return summary string
     */
    public String getSummary() {
        return String.format("%s[%s] at %s with %d data fields", 
                           type, jobId, timestamp, data.size());
    }
}