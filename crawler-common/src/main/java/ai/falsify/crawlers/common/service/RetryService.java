package ai.falsify.crawlers.common.service;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.exception.CrawlingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Service for handling retry logic with exponential backoff and circuit breaker pattern.
 * Provides configurable retry mechanisms for network operations and other fallible operations.
 */
@ApplicationScoped
public class RetryService {

    private static final Logger LOG = Logger.getLogger(RetryService.class);

    @Inject
    CrawlerConfiguration config;

    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Executes an operation with retry logic and exponential backoff.
     *
     * @param operation the operation to execute
     * @param operationName descriptive name for logging
     * @param <T> the return type
     * @return the result of the operation
     * @throws CrawlingException if all retry attempts fail
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) throws CrawlingException {
        return executeWithRetry(operation, operationName, Exception.class);
    }

    /**
     * Executes an operation with retry logic, only retrying on specific exception types.
     *
     * @param operation the operation to execute
     * @param operationName descriptive name for logging
     * @param retryableExceptionType the type of exception that should trigger a retry
     * @param <T> the return type
     * @param <E> the exception type to retry on
     * @return the result of the operation
     * @throws CrawlingException if all retry attempts fail
     */
    public <T, E extends Exception> T executeWithRetry(
            Supplier<T> operation, 
            String operationName, 
            Class<E> retryableExceptionType) throws CrawlingException {

        CrawlerConfiguration.RetryConfig retryConfig = config.retry();
        
        // Check circuit breaker first
        if (retryConfig.enableCircuitBreaker() && circuitBreaker.isOpen()) {
            throw new CrawlingException(
                CrawlingException.ErrorCode.OPERATION_CANCELLED,
                "Circuit breaker is open, operation cancelled: " + operationName
            );
        }

        int maxAttempts = retryConfig.maxAttempts();
        Duration initialDelay = retryConfig.initialDelay();
        Duration maxDelay = retryConfig.maxDelay();
        double backoffMultiplier = retryConfig.backoffMultiplier();

        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOG.debugf("Executing %s (attempt %d/%d)", operationName, attempt, maxAttempts);
                
                T result = operation.get();
                
                // Success - reset circuit breaker if enabled
                if (retryConfig.enableCircuitBreaker()) {
                    circuitBreaker.recordSuccess();
                }
                
                if (attempt > 1) {
                    LOG.infof("Operation %s succeeded on attempt %d/%d", operationName, attempt, maxAttempts);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                // Check if this exception type should trigger a retry
                if (!retryableExceptionType.isAssignableFrom(e.getClass())) {
                    LOG.debugf("Exception %s is not retryable for operation %s", e.getClass().getSimpleName(), operationName);
                    break;
                }
                
                // Record failure for circuit breaker
                if (retryConfig.enableCircuitBreaker()) {
                    circuitBreaker.recordFailure();
                    
                    // Check if circuit breaker should open
                    if (circuitBreaker.shouldOpen(retryConfig.circuitBreakerFailureThreshold())) {
                        LOG.warnf("Circuit breaker opened after %d failures", retryConfig.circuitBreakerFailureThreshold());
                        break;
                    }
                }
                
                if (attempt == maxAttempts) {
                    LOG.errorf("Operation %s failed after %d attempts", operationName, maxAttempts);
                    break;
                }
                
                // Calculate delay for next attempt
                Duration delay = calculateDelay(attempt, initialDelay, maxDelay, backoffMultiplier);
                
                LOG.warnf("Operation %s failed on attempt %d/%d, retrying in %s. Error: %s", 
                    operationName, attempt, maxAttempts, delay, e.getMessage());
                
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CrawlingException(
                        CrawlingException.ErrorCode.OPERATION_CANCELLED,
                        "Operation interrupted during retry delay: " + operationName,
                        ie
                    );
                }
            }
        }
        
        // All attempts failed
        CrawlingException.ErrorCode errorCode = (lastException instanceof CrawlingException) 
            ? ((CrawlingException) lastException).getErrorCode()
            : CrawlingException.ErrorCode.UNKNOWN_ERROR;
            
        throw new CrawlingException(
            errorCode,
            String.format("Operation %s failed after %d attempts", operationName, maxAttempts),
            operationName,
            lastException
        );
    }

    /**
     * Calculates the delay for the next retry attempt using exponential backoff with jitter.
     */
    private Duration calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, double backoffMultiplier) {
        // Calculate exponential backoff
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        
        // Apply maximum delay limit
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());
        
        // Add jitter (±25% random variation) to avoid thundering herd
        double jitterFactor = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.5); // 0.75 to 1.25
        delayMillis = (long) (delayMillis * jitterFactor);
        
        return Duration.ofMillis(Math.max(delayMillis, 0));
    }

    /**
     * Gets the current circuit breaker status for monitoring.
     */
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return new CircuitBreakerStatus(
            circuitBreaker.isOpen(),
            circuitBreaker.getFailureCount(),
            circuitBreaker.getLastFailureTime()
        );
    }

    /**
     * Manually resets the circuit breaker (for administrative purposes).
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
        LOG.info("Circuit breaker manually reset");
    }

    /**
     * Simple circuit breaker implementation
     */
    private static class CircuitBreaker {
        private volatile boolean isOpen = false;
        private volatile int failureCount = 0;
        private volatile Instant lastFailureTime = null;

        public boolean isOpen() {
            return isOpen;
        }

        public void recordSuccess() {
            failureCount = 0;
            isOpen = false;
            lastFailureTime = null;
        }

        public void recordFailure() {
            failureCount++;
            lastFailureTime = Instant.now();
        }

        public boolean shouldOpen(int threshold) {
            if (failureCount >= threshold) {
                isOpen = true;
                return true;
            }
            return false;
        }

        public void reset() {
            isOpen = false;
            failureCount = 0;
            lastFailureTime = null;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public Instant getLastFailureTime() {
            return lastFailureTime;
        }
    }

    /**
     * Status information for circuit breaker monitoring
     */
    public static class CircuitBreakerStatus {
        private final boolean isOpen;
        private final int failureCount;
        private final Instant lastFailureTime;

        public CircuitBreakerStatus(boolean isOpen, int failureCount, Instant lastFailureTime) {
            this.isOpen = isOpen;
            this.failureCount = failureCount;
            this.lastFailureTime = lastFailureTime;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public Instant getLastFailureTime() {
            return lastFailureTime;
        }

        @Override
        public String toString() {
            return String.format("CircuitBreakerStatus[open=%s, failures=%d, lastFailure=%s]", 
                isOpen, failureCount, lastFailureTime);
        }
    }
}