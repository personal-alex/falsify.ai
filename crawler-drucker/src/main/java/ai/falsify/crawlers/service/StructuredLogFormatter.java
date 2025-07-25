package ai.falsify.crawlers.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility class for creating structured log messages with consistent formatting.
 * Provides methods to create structured log entries with key-value pairs.
 */
@ApplicationScoped
public class StructuredLogFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;

    /**
     * Create a structured log message builder
     */
    public LogMessageBuilder createMessage(String operation) {
        return new LogMessageBuilder(operation);
    }

    /**
     * Builder class for creating structured log messages
     */
    public static class LogMessageBuilder {
        private final String operation;
        private final Map<String, Object> fields = new HashMap<>();
        private String message;
        private Instant timestamp;

        public LogMessageBuilder(String operation) {
            this.operation = operation;
            this.timestamp = Instant.now();
        }

        public LogMessageBuilder message(String message) {
            this.message = message;
            return this;
        }

        public LogMessageBuilder field(String key, Object value) {
            if (value != null) {
                fields.put(key, value);
            }
            return this;
        }

        public LogMessageBuilder url(String url) {
            return field("url", url);
        }

        public LogMessageBuilder duration(long durationMs) {
            return field("duration_ms", durationMs);
        }

        public LogMessageBuilder count(String type, int count) {
            return field(type + "_count", count);
        }

        public LogMessageBuilder success(boolean success) {
            return field("success", success);
        }

        public LogMessageBuilder error(String error) {
            return field("error", error);
        }

        public LogMessageBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            
            // Add operation prefix
            sb.append(operation.toUpperCase()).append(": ");
            
            // Add main message if provided
            if (message != null) {
                sb.append(message);
            }
            
            // Add structured fields
            if (!fields.isEmpty()) {
                sb.append(" [");
                StringJoiner joiner = new StringJoiner(", ");
                fields.forEach((key, value) -> joiner.add(key + "=" + value));
                sb.append(joiner.toString());
                sb.append("]");
            }
            
            return sb.toString();
        }

        @Override
        public String toString() {
            return build();
        }
    }

    /**
     * Create a crawl session log message
     */
    public String crawlSession(String phase, String url, int processed, int skipped, int failed, long durationMs) {
        return createMessage("crawl_session")
                .message(phase)
                .url(url)
                .count("processed", processed)
                .count("skipped", skipped)
                .count("failed", failed)
                .duration(durationMs)
                .build();
    }

    /**
     * Create an article processing log message
     */
    public String articleProcessing(String phase, String url, String title, int contentLength, boolean success, long durationMs) {
        return createMessage("article_processing")
                .message(phase)
                .url(url)
                .field("title", title)
                .field("content_length", contentLength)
                .success(success)
                .duration(durationMs)
                .build();
    }

    /**
     * Create a network operation log message
     */
    public String networkOperation(String operation, String url, boolean success, long durationMs, String error) {
        LogMessageBuilder builder = createMessage("network_operation")
                .message(operation)
                .url(url)
                .success(success)
                .duration(durationMs);
        
        if (error != null) {
            builder.error(error);
        }
        
        return builder.build();
    }

    /**
     * Create a database operation log message
     */
    public String databaseOperation(String operation, String entity, boolean success, long durationMs, String error) {
        LogMessageBuilder builder = createMessage("database_operation")
                .message(operation)
                .field("entity", entity)
                .success(success)
                .duration(durationMs);
        
        if (error != null) {
            builder.error(error);
        }
        
        return builder.build();
    }

    /**
     * Create a Redis operation log message
     */
    public String redisOperation(String operation, String key, boolean success, long durationMs, String error) {
        LogMessageBuilder builder = createMessage("redis_operation")
                .message(operation)
                .field("key", key)
                .success(success)
                .duration(durationMs);
        
        if (error != null) {
            builder.error(error);
        }
        
        return builder.build();
    }

    /**
     * Create a performance summary log message
     */
    public String performanceSummary(String operation, int totalOps, int successes, int failures, 
                                   double avgDurationMs, double successRate, long totalElapsedMs) {
        return createMessage("performance_summary")
                .message(operation + " performance summary")
                .count("total_operations", totalOps)
                .count("successes", successes)
                .count("failures", failures)
                .field("avg_duration_ms", String.format("%.1f", avgDurationMs))
                .field("success_rate_pct", String.format("%.1f", successRate))
                .field("total_elapsed_ms", totalElapsedMs)
                .build();
    }
}