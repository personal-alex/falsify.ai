package ai.falsify.crawlers.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Configuration model for individual crawler instances.
 * Represents the configuration of a single crawler that can be managed by the crawler manager.
 */
public class CrawlerConfiguration {
    
    @NotBlank(message = "Crawler ID cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Crawler ID must contain only alphanumeric characters, hyphens, and underscores")
    public String id;
    
    @NotBlank(message = "Crawler name cannot be blank")
    public String name;
    
    @NotBlank(message = "Base URL cannot be blank")
    @Pattern(regexp = "^https?://.*", message = "Base URL must be a valid HTTP or HTTPS URL")
    public String baseUrl;
    
    @NotNull(message = "Port cannot be null")
    @Positive(message = "Port must be a positive number")
    public Integer port;
    
    @NotBlank(message = "Health endpoint cannot be blank")
    @Pattern(regexp = "^/.*", message = "Health endpoint must start with /")
    public String healthEndpoint;
    
    @NotBlank(message = "Crawl endpoint cannot be blank")
    @Pattern(regexp = "^/.*", message = "Crawl endpoint must start with /")
    public String crawlEndpoint;
    
    @NotBlank(message = "Status endpoint cannot be blank")
    @Pattern(regexp = "^/.*", message = "Status endpoint must start with /")
    public String statusEndpoint;
    
    @NotNull(message = "Enabled flag cannot be null")
    public Boolean enabled;
    
    // Author information
    public String authorName;
    public String authorAvatarUrl;
    
    public CrawlerConfiguration() {
        // Default constructor for Jackson
    }
    
    public CrawlerConfiguration(String id, String name, String baseUrl, Integer port, 
                              String healthEndpoint, String crawlEndpoint, String statusEndpoint, 
                              Boolean enabled) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.port = port;
        this.healthEndpoint = healthEndpoint;
        this.crawlEndpoint = crawlEndpoint;
        this.statusEndpoint = statusEndpoint;
        this.enabled = enabled;
    }
    
    public CrawlerConfiguration(String id, String name, String baseUrl, Integer port, 
                              String healthEndpoint, String crawlEndpoint, String statusEndpoint, 
                              Boolean enabled, String authorName, String authorAvatarUrl) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.port = port;
        this.healthEndpoint = healthEndpoint;
        this.crawlEndpoint = crawlEndpoint;
        this.statusEndpoint = statusEndpoint;
        this.enabled = enabled;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
    }
    
    /**
     * Gets the full URL for the health endpoint.
     * @return Complete URL for health checks
     */
    public String getHealthUrl() {
        return baseUrl + healthEndpoint;
    }
    
    /**
     * Gets the full URL for the crawl endpoint.
     * @return Complete URL for triggering crawls
     */
    public String getCrawlUrl() {
        return baseUrl + crawlEndpoint;
    }
    
    /**
     * Gets the full URL for the status endpoint.
     * @return Complete URL for status checks
     */
    public String getStatusUrl() {
        return baseUrl + statusEndpoint;
    }
    
    @Override
    public String toString() {
        return "CrawlerConfiguration{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", port=" + port +
                ", healthEndpoint='" + healthEndpoint + '\'' +
                ", crawlEndpoint='" + crawlEndpoint + '\'' +
                ", statusEndpoint='" + statusEndpoint + '\'' +
                ", enabled=" + enabled +
                ", authorName='" + authorName + '\'' +
                ", authorAvatarUrl='" + authorAvatarUrl + '\'' +
                '}';
    }
}