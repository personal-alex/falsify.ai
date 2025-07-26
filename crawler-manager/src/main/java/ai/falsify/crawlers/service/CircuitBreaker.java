package ai.falsify.crawlers.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit breaker implementation for handling failed health checks.
 * Prevents continuous attempts to check unhealthy crawlers by implementing
 * a circuit breaker pattern with configurable failure thresholds and timeouts.
 */
@ApplicationScoped
public class CircuitBreaker {
    
    private static final Logger LOG = Logger.getLogger(CircuitBreaker.class);
    
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, requests are failing fast
        HALF_OPEN  // Testing if service has recovered
    }
    
    private static class CircuitState {
        volatile State state = State.CLOSED;
        final AtomicInteger failureCount = new AtomicInteger(0);
        volatile Instant lastFailureTime;
        volatile Instant lastSuccessTime;
        
        CircuitState() {
            this.lastSuccessTime = Instant.now();
        }
    }
    
    private final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();
    
    // Configuration
    private final int failureThreshold = 3;
    private final Duration timeout = Duration.ofMinutes(1);
    private final Duration halfOpenTimeout = Duration.ofSeconds(30);
    
    /**
     * Checks if a request should be allowed for the given crawler.
     */
    public boolean allowRequest(String crawlerId) {
        CircuitState circuit = circuits.computeIfAbsent(crawlerId, k -> new CircuitState());
        
        switch (circuit.state) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (shouldAttemptReset(circuit)) {
                    circuit.state = State.HALF_OPEN;
                    LOG.infof("Circuit breaker for crawler %s moved to HALF_OPEN state", crawlerId);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                // Allow one request to test if service has recovered
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Records a successful operation.
     */
    public void recordSuccess(String crawlerId) {
        CircuitState circuit = circuits.get(crawlerId);
        if (circuit != null) {
            circuit.failureCount.set(0);
            circuit.lastSuccessTime = Instant.now();
            
            if (circuit.state != State.CLOSED) {
                circuit.state = State.CLOSED;
                LOG.infof("Circuit breaker for crawler %s reset to CLOSED state", crawlerId);
            }
        }
    }
    
    /**
     * Records a failed operation.
     */
    public void recordFailure(String crawlerId) {
        CircuitState circuit = circuits.computeIfAbsent(crawlerId, k -> new CircuitState());
        
        circuit.lastFailureTime = Instant.now();
        int failures = circuit.failureCount.incrementAndGet();
        
        if (circuit.state == State.HALF_OPEN) {
            // Failed during half-open, go back to open
            circuit.state = State.OPEN;
            LOG.infof("Circuit breaker for crawler %s failed during HALF_OPEN, returning to OPEN state", crawlerId);
        } else if (circuit.state == State.CLOSED && failures >= failureThreshold) {
            // Too many failures, open the circuit
            circuit.state = State.OPEN;
            LOG.warnf("Circuit breaker for crawler %s opened after %d failures", crawlerId, failures);
        }
    }
    
    /**
     * Gets the current state of the circuit for a crawler.
     */
    public State getState(String crawlerId) {
        CircuitState circuit = circuits.get(crawlerId);
        return circuit != null ? circuit.state : State.CLOSED;
    }
    
    /**
     * Gets the current failure count for a crawler.
     */
    public int getFailureCount(String crawlerId) {
        CircuitState circuit = circuits.get(crawlerId);
        return circuit != null ? circuit.failureCount.get() : 0;
    }
    
    /**
     * Manually resets the circuit breaker for a crawler.
     */
    public void reset(String crawlerId) {
        CircuitState circuit = circuits.get(crawlerId);
        if (circuit != null) {
            circuit.state = State.CLOSED;
            circuit.failureCount.set(0);
            circuit.lastSuccessTime = Instant.now();
            LOG.infof("Circuit breaker for crawler %s manually reset", crawlerId);
        }
    }
    
    private boolean shouldAttemptReset(CircuitState circuit) {
        if (circuit.lastFailureTime == null) {
            return true;
        }
        
        Duration timeSinceLastFailure = Duration.between(circuit.lastFailureTime, Instant.now());
        return timeSinceLastFailure.compareTo(timeout) >= 0;
    }
}