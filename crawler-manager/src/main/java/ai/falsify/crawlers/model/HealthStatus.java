package ai.falsify.crawlers.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Represents the health status of a crawler instance.
 * Contains information about the current health state, response time, and last check timestamp.
 */
public class HealthStatus {
    
    public enum Status {
        HEALTHY,
        UNHEALTHY,
        UNKNOWN
    }
    
    @NotNull
    public Status status;
    
    public String message;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant lastCheck;
    
    public Long responseTimeMs;
    
    public String crawlerId;
    
    public HealthStatus() {
        // Default constructor for Jackson
    }
    
    public HealthStatus(String crawlerId, Status status, String message, Instant lastCheck, Long responseTimeMs) {
        this.crawlerId = crawlerId;
        this.status = status;
        this.message = message;
        this.lastCheck = lastCheck;
        this.responseTimeMs = responseTimeMs;
    }
    
    /**
     * Creates a healthy status instance.
     */
    public static HealthStatus healthy(String crawlerId, Long responseTimeMs) {
        return new HealthStatus(crawlerId, Status.HEALTHY, "Crawler is responding normally", Instant.now(), responseTimeMs);
    }
    
    /**
     * Creates an unhealthy status instance.
     */
    public static HealthStatus unhealthy(String crawlerId, String message) {
        return new HealthStatus(crawlerId, Status.UNHEALTHY, message, Instant.now(), null);
    }
    
    /**
     * Creates an unknown status instance.
     */
    public static HealthStatus unknown(String crawlerId, String message) {
        return new HealthStatus(crawlerId, Status.UNKNOWN, message, Instant.now(), null);
    }
    
    /**
     * Checks if the status indicates the crawler is healthy.
     */
    @JsonIgnore
    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }
    
    /**
     * Checks if the status indicates the crawler is unhealthy.
     */
    @JsonIgnore
    public boolean isUnhealthy() {
        return status == Status.UNHEALTHY;
    }
    
    /**
     * Checks if the status is unknown.
     */
    @JsonIgnore
    public boolean isUnknown() {
        return status == Status.UNKNOWN;
    }
    
    @Override
    public String toString() {
        return "HealthStatus{" +
                "crawlerId='" + crawlerId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", lastCheck=" + lastCheck +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}