package ai.falsify.crawlers.common.service.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;

/**
 * Health check service for Redis connectivity and performance monitoring.
 */
@ApplicationScoped
public class RedisHealthCheck {

    private static final Logger LOG = Logger.getLogger(RedisHealthCheck.class);
    private static final String HEALTH_CHECK_KEY = "health:check";

    @Inject
    RedisService redisService;

    /**
     * Performs a basic health check by testing Redis connectivity.
     * 
     * @return HealthStatus indicating the health of Redis
     */
    public HealthStatus checkHealth() {
        Instant start = Instant.now();
        
        try {
            // Test basic connectivity with a simple SET/GET operation
            String testValue = "health-check-" + System.currentTimeMillis();
            redisService.set(HEALTH_CHECK_KEY, testValue, Duration.ofSeconds(10));
            
            String retrievedValue = redisService.get(HEALTH_CHECK_KEY).orElse(null);
            
            Duration responseTime = Duration.between(start, Instant.now());
            
            if (testValue.equals(retrievedValue)) {
                // Clean up test key
                redisService.delete(HEALTH_CHECK_KEY);
                
                return HealthStatus.healthy(responseTime);
            } else {
                return HealthStatus.unhealthy("Redis GET/SET test failed", responseTime);
            }
            
        } catch (Exception e) {
            Duration responseTime = Duration.between(start, Instant.now());
            LOG.errorf(e, "Redis health check failed");
            return HealthStatus.unhealthy("Redis connection failed: " + e.getMessage(), responseTime);
        }
    }

    /**
     * Performs an extended health check including performance metrics.
     * 
     * @return ExtendedHealthStatus with detailed metrics
     */
    public ExtendedHealthStatus checkExtendedHealth() {
        Instant start = Instant.now();
        
        try {
            HealthStatus basicHealth = checkHealth();
            
            if (!basicHealth.isHealthy()) {
                return ExtendedHealthStatus.fromBasic(basicHealth);
            }
            
            // Test various operations for performance
            Instant opStart = Instant.now();
            
            // Test SET operation
            redisService.set("perf:test:set", "test-value", Duration.ofSeconds(10));
            Duration setTime = Duration.between(opStart, Instant.now());
            
            // Test GET operation
            opStart = Instant.now();
            redisService.get("perf:test:set");
            Duration getTime = Duration.between(opStart, Instant.now());
            
            // Test SETNX operation
            opStart = Instant.now();
            redisService.setnx("perf:test:setnx", "test-value", Duration.ofSeconds(10));
            Duration setnxTime = Duration.between(opStart, Instant.now());
            
            // Test EXISTS operation
            opStart = Instant.now();
            redisService.exists("perf:test:set");
            Duration existsTime = Duration.between(opStart, Instant.now());
            
            // Clean up test keys
            redisService.delete("perf:test:set");
            redisService.delete("perf:test:setnx");
            
            Duration totalTime = Duration.between(start, Instant.now());
            
            return ExtendedHealthStatus.healthy(totalTime, setTime, getTime, setnxTime, existsTime);
            
        } catch (Exception e) {
            Duration totalTime = Duration.between(start, Instant.now());
            LOG.errorf(e, "Redis extended health check failed");
            return ExtendedHealthStatus.unhealthy("Extended health check failed: " + e.getMessage(), totalTime);
        }
    }

    /**
     * Basic health status for Redis.
     */
    public static class HealthStatus {
        private final boolean healthy;
        private final String message;
        private final Duration responseTime;
        private final Instant timestamp;

        private HealthStatus(boolean healthy, String message, Duration responseTime) {
            this.healthy = healthy;
            this.message = message;
            this.responseTime = responseTime;
            this.timestamp = Instant.now();
        }

        public static HealthStatus healthy(Duration responseTime) {
            return new HealthStatus(true, "Redis is healthy", responseTime);
        }

        public static HealthStatus unhealthy(String message, Duration responseTime) {
            return new HealthStatus(false, message, responseTime);
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Duration getResponseTime() {
            return responseTime;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("HealthStatus[healthy=%s, message=%s, responseTime=%dms, timestamp=%s]",
                    healthy, message, responseTime.toMillis(), timestamp);
        }
    }

    /**
     * Extended health status with performance metrics.
     */
    public static class ExtendedHealthStatus extends HealthStatus {
        private final Duration setOperationTime;
        private final Duration getOperationTime;
        private final Duration setnxOperationTime;
        private final Duration existsOperationTime;

        private ExtendedHealthStatus(boolean healthy, String message, Duration responseTime,
                                   Duration setTime, Duration getTime, Duration setnxTime, Duration existsTime) {
            super(healthy, message, responseTime);
            this.setOperationTime = setTime;
            this.getOperationTime = getTime;
            this.setnxOperationTime = setnxTime;
            this.existsOperationTime = existsTime;
        }

        public static ExtendedHealthStatus healthy(Duration totalTime, Duration setTime, 
                                                 Duration getTime, Duration setnxTime, Duration existsTime) {
            return new ExtendedHealthStatus(true, "Redis is healthy with performance metrics", 
                    totalTime, setTime, getTime, setnxTime, existsTime);
        }

        public static ExtendedHealthStatus unhealthy(String message, Duration totalTime) {
            return new ExtendedHealthStatus(false, message, totalTime, null, null, null, null);
        }

        public static ExtendedHealthStatus fromBasic(HealthStatus basic) {
            return new ExtendedHealthStatus(basic.isHealthy(), basic.getMessage(), 
                    basic.getResponseTime(), null, null, null, null);
        }

        public Duration getSetOperationTime() {
            return setOperationTime;
        }

        public Duration getGetOperationTime() {
            return getOperationTime;
        }

        public Duration getSetnxOperationTime() {
            return setnxOperationTime;
        }

        public Duration getExistsOperationTime() {
            return existsOperationTime;
        }

        @Override
        public String toString() {
            if (!isHealthy()) {
                return super.toString();
            }
            
            return String.format("ExtendedHealthStatus[healthy=%s, totalTime=%dms, " +
                    "setTime=%dms, getTime=%dms, setnxTime=%dms, existsTime=%dms]",
                    isHealthy(), getResponseTime().toMillis(),
                    setOperationTime != null ? setOperationTime.toMillis() : -1,
                    getOperationTime != null ? getOperationTime.toMillis() : -1,
                    setnxOperationTime != null ? setnxOperationTime.toMillis() : -1,
                    existsOperationTime != null ? existsOperationTime.toMillis() : -1);
        }
    }
}