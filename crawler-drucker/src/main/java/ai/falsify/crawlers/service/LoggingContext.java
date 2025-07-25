package ai.falsify.crawlers.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for managing logging context and structured log formatting.
 * Provides consistent logging patterns across the crawler application.
 */
@ApplicationScoped
public class LoggingContext {

    private static final Logger LOG = Logger.getLogger(LoggingContext.class);
    
    // Thread-local storage for context information
    private final ThreadLocal<Map<String, Object>> contextData = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Set a context value for the current thread
     */
    public void setContext(String key, Object value) {
        contextData.get().put(key, value);
    }

    /**
     * Get a context value for the current thread
     */
    public Object getContext(String key) {
        return contextData.get().get(key);
    }

    /**
     * Clear all context data for the current thread
     */
    public void clearContext() {
        contextData.get().clear();
    }

    /**
     * Format a structured log message with context
     */
    public String formatMessage(String operation, String message, Object... params) {
        Map<String, Object> context = contextData.get();
        StringBuilder sb = new StringBuilder();
        
        sb.append(operation).append(": ").append(String.format(message, params));
        
        if (!context.isEmpty()) {
            sb.append(" [");
            context.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
            sb.append("]");
        }
        
        return sb.toString();
    }

    /**
     * Log operation start with timing
     */
    public OperationTimer startOperation(Logger logger, String operation, String description, Object... params) {
        String message = formatMessage(operation + "_START", description, params);
        logger.info(message);
        return new OperationTimer(logger, operation, Instant.now());
    }

    /**
     * Log operation completion with timing
     */
    public static class OperationTimer {
        private final Logger logger;
        private final String operation;
        private final Instant startTime;

        public OperationTimer(Logger logger, String operation, Instant startTime) {
            this.logger = logger;
            this.operation = operation;
            this.startTime = startTime;
        }

        public void complete(String description, Object... params) {
            Duration duration = Duration.between(startTime, Instant.now());
            String message = String.format("%s_COMPLETE: %s (duration=%dms)", 
                    operation, String.format(description, params), duration.toMillis());
            logger.info(message);
        }

        public void fail(String description, Throwable error, Object... params) {
            Duration duration = Duration.between(startTime, Instant.now());
            String message = String.format("%s_FAILED: %s (duration=%dms)", 
                    operation, String.format(description, params), duration.toMillis());
            logger.error(message, error);
        }
    }

    /**
     * Create a performance logger for tracking operation metrics
     */
    public PerformanceLogger createPerformanceLogger(Logger logger, String operation) {
        return new PerformanceLogger(logger, operation);
    }

    /**
     * Performance logging utility for tracking metrics
     */
    public static class PerformanceLogger {
        private final Logger logger;
        private final String operation;
        private final Instant startTime;
        private int successCount = 0;
        private int failureCount = 0;
        private long totalDuration = 0;

        public PerformanceLogger(Logger logger, String operation) {
            this.logger = logger;
            this.operation = operation;
            this.startTime = Instant.now();
        }

        public void recordSuccess(Duration duration) {
            successCount++;
            totalDuration += duration.toMillis();
            logger.debugf("PERF_%s_SUCCESS: Operation completed in %dms (total_success=%d)", 
                    operation, duration.toMillis(), successCount);
        }

        public void recordFailure(Duration duration) {
            failureCount++;
            totalDuration += duration.toMillis();
            logger.debugf("PERF_%s_FAILURE: Operation failed in %dms (total_failures=%d)", 
                    operation, duration.toMillis(), failureCount);
        }

        public void logSummary() {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            int totalOperations = successCount + failureCount;
            double avgDuration = totalOperations > 0 ? (double) totalDuration / totalOperations : 0;
            double successRate = totalOperations > 0 ? (double) successCount / totalOperations * 100 : 0;

            logger.infof("PERF_%s_SUMMARY: total_ops=%d, success=%d, failures=%d, success_rate=%.1f%%, avg_duration=%.1fms, total_elapsed=%dms",
                    operation, totalOperations, successCount, failureCount, successRate, avgDuration, totalElapsed.toMillis());
        }
    }
}