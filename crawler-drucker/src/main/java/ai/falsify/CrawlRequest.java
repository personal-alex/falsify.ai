package ai.falsify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for triggering crawler operations.
 * Contains parameters and options for crawl execution.
 */
public class CrawlRequest {
    
    @NotBlank(message = "Crawler ID cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Crawler ID must contain only alphanumeric characters, hyphens, and underscores")
    public String crawlerId;
    
    // Job tracking fields
    public String jobId;
    public String callbackUrl;
    
    // Optional parameters for future extensibility
    public String priority;
    public Integer maxArticles;
    public String dateRange;
    
    public CrawlRequest() {
        // Default constructor for Jackson
    }
    
    public CrawlRequest(String crawlerId) {
        this.crawlerId = crawlerId;
    }
    
    public CrawlRequest(String crawlerId, String priority, Integer maxArticles, String dateRange) {
        this.crawlerId = crawlerId;
        this.priority = priority;
        this.maxArticles = maxArticles;
        this.dateRange = dateRange;
    }
    
    public CrawlRequest(String crawlerId, String jobId, String callbackUrl) {
        this.crawlerId = crawlerId;
        this.jobId = jobId;
        this.callbackUrl = callbackUrl;
    }
    
    @Override
    public String toString() {
        return "CrawlRequest{" +
                "crawlerId='" + crawlerId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", priority='" + priority + '\'' +
                ", maxArticles=" + maxArticles +
                ", dateRange='" + dateRange + '\'' +
                '}';
    }
}