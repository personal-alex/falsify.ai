package ai.falsify.crawlers.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Response model for crawl operation results.
 * Contains information about the crawl request outcome and execution details.
 */
public class CrawlResponse {
    
    public enum Status {
        ACCEPTED,
        CONFLICT,
        ERROR,
        SERVICE_UNAVAILABLE
    }
    
    @NotNull
    public Status status;
    
    public String message;
    
    public String requestId;
    
    public String crawlId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant timestamp;
    
    public String crawlerId;
    
    public String statusEndpoint;
    
    public String estimatedDuration;
    
    public String errorCategory;
    
    public String suggestion;
    
    public CrawlResponse() {
        // Default constructor for Jackson
    }
    
    public CrawlResponse(String crawlerId, Status status, String message, String requestId) {
        this.crawlerId = crawlerId;
        this.status = status;
        this.message = message;
        this.requestId = requestId;
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a successful crawl response.
     */
    public static CrawlResponse accepted(String crawlerId, String requestId, String crawlId, String statusEndpoint) {
        CrawlResponse response = new CrawlResponse(crawlerId, Status.ACCEPTED, "Crawl started successfully", requestId);
        response.crawlId = crawlId;
        response.statusEndpoint = statusEndpoint;
        response.estimatedDuration = "Variable (depends on article count and site responsiveness)";
        return response;
    }
    
    /**
     * Creates a conflict response when crawl is already in progress.
     */
    public static CrawlResponse conflict(String crawlerId, String requestId, String currentStatus) {
        CrawlResponse response = new CrawlResponse(crawlerId, Status.CONFLICT, "Crawl already in progress", requestId);
        response.suggestion = "Wait for current crawl to complete before starting a new one";
        return response;
    }
    
    /**
     * Creates an error response for failed crawl requests.
     */
    public static CrawlResponse error(String crawlerId, String requestId, String message, String errorCategory) {
        CrawlResponse response = new CrawlResponse(crawlerId, Status.ERROR, message, requestId);
        response.errorCategory = errorCategory;
        response.suggestion = "Check logs for detailed error information and retry after resolving the issue";
        return response;
    }
    
    /**
     * Creates a service unavailable response.
     */
    public static CrawlResponse serviceUnavailable(String crawlerId, String requestId, String message) {
        CrawlResponse response = new CrawlResponse(crawlerId, Status.SERVICE_UNAVAILABLE, message, requestId);
        response.suggestion = "Check crawler service availability and configuration";
        return response;
    }
    
    @Override
    public String toString() {
        return "CrawlResponse{" +
                "crawlerId='" + crawlerId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", crawlId='" + crawlId + '\'' +
                ", timestamp=" + timestamp +
                ", statusEndpoint='" + statusEndpoint + '\'' +
                ", errorCategory='" + errorCategory + '\'' +
                '}';
    }
}