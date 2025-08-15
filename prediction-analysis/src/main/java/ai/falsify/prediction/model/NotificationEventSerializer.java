package ai.falsify.prediction.model;

import java.util.Map;

/**
 * Utility class for serializing notification events to JSON format.
 * Provides consistent serialization for WebSocket message transmission.
 */
public class NotificationEventSerializer {
    
    /**
     * Converts a NotificationEvent to JSON string.
     * 
     * @param event the notification event
     * @return JSON string representation
     */
    public static String toJson(NotificationEvent event) {
        if (event == null) {
            return "{}";
        }
        
        return mapToJson(event.toMap());
    }
    
    /**
     * Converts a BatchNotificationEvent to JSON string.
     * 
     * @param event the batch notification event
     * @return JSON string representation
     */
    public static String toJson(BatchNotificationEvent event) {
        if (event == null) {
            return "{}";
        }
        
        return mapToJson(event.toMap());
    }
    
    /**
     * Converts a map to JSON string.
     * Simple implementation for basic data types - in production, use Jackson or similar.
     * 
     * @param map the map to convert
     * @return JSON string
     */
    public static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            json.append(valueToJson(entry.getValue()));
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Converts a value to JSON representation.
     * 
     * @param value the value to convert
     * @return JSON representation of the value
     */
    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJsonString((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return mapToJson(mapValue);
        } else if (value instanceof Iterable) {
            StringBuilder array = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    array.append(",");
                }
                first = false;
                array.append(valueToJson(item));
            }
            array.append("]");
            return array.toString();
        } else {
            // For other types, convert to string and escape
            return "\"" + escapeJsonString(value.toString()) + "\"";
        }
    }
    
    /**
     * Escapes special characters in JSON strings.
     * 
     * @param str the string to escape
     * @return escaped string
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\u0000", "\\u0000")
                  .replace("\u0001", "\\u0001")
                  .replace("\u0002", "\\u0002")
                  .replace("\u0003", "\\u0003")
                  .replace("\u0004", "\\u0004")
                  .replace("\u0005", "\\u0005")
                  .replace("\u0006", "\\u0006")
                  .replace("\u0007", "\\u0007")
                  .replace("\u000B", "\\u000B")
                  .replace("\u000E", "\\u000E")
                  .replace("\u000F", "\\u000F")
                  .replace("\u0010", "\\u0010")
                  .replace("\u0011", "\\u0011")
                  .replace("\u0012", "\\u0012")
                  .replace("\u0013", "\\u0013")
                  .replace("\u0014", "\\u0014")
                  .replace("\u0015", "\\u0015")
                  .replace("\u0016", "\\u0016")
                  .replace("\u0017", "\\u0017")
                  .replace("\u0018", "\\u0018")
                  .replace("\u0019", "\\u0019")
                  .replace("\u001A", "\\u001A")
                  .replace("\u001B", "\\u001B")
                  .replace("\u001C", "\\u001C")
                  .replace("\u001D", "\\u001D")
                  .replace("\u001E", "\\u001E")
                  .replace("\u001F", "\\u001F");
    }
    
    /**
     * Creates a simple JSON message for basic notifications.
     * 
     * @param type message type
     * @param jobId job identifier
     * @param message message content
     * @return JSON string
     */
    public static String createSimpleMessage(String type, String jobId, String message) {
        Map<String, Object> data = Map.of(
            "type", type,
            "jobId", jobId,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        return mapToJson(data);
    }
    
    /**
     * Creates a JSON error message.
     * 
     * @param type message type
     * @param jobId job identifier
     * @param errorMessage error message
     * @return JSON string
     */
    public static String createErrorMessage(String type, String jobId, String errorMessage) {
        Map<String, Object> data = Map.of(
            "type", type,
            "jobId", jobId,
            "error", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        return mapToJson(data);
    }
    
    /**
     * Creates a JSON progress message.
     * 
     * @param type message type
     * @param jobId job identifier
     * @param current current progress value
     * @param total total progress value
     * @return JSON string
     */
    public static String createProgressMessage(String type, String jobId, int current, int total) {
        double percentage = total > 0 ? (double) current / total * 100 : 0.0;
        
        Map<String, Object> data = Map.of(
            "type", type,
            "jobId", jobId,
            "current", current,
            "total", total,
            "percentage", percentage,
            "timestamp", System.currentTimeMillis()
        );
        return mapToJson(data);
    }
    
    /**
     * Validates that a JSON string is properly formatted.
     * Basic validation - checks for balanced braces and quotes.
     * 
     * @param json the JSON string to validate
     * @return true if the JSON appears to be valid
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }
        
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : json.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
            }
        }
        
        return braceCount == 0 && !inString;
    }
}