package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.CrawlRequest;
import ai.falsify.crawlers.model.CrawlResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for proxying crawl requests to individual crawler instances.
 * Handles HTTP communication, timeout management, retry logic, and error handling.
 */
@ApplicationScoped
public class CrawlerProxyService {
    
    private static final Logger LOG = Logger.getLogger(CrawlerProxyService.class);
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    
    @Inject
    CrawlerConfigurationService configurationService;
    
    @Inject
    CircuitBreaker circuitBreaker;
    
    /**
     * Triggers a crawl operation on the specified crawler.
     * 
     * @param request The crawl request containing crawler ID and parameters
     * @return CrawlResponse with operation result
     */
    public CrawlResponse triggerCrawl(CrawlRequest request) {
        String requestId = "crawl-proxy-" + System.currentTimeMillis();
        LOG.infof("Processing crawl request [%s] for crawler: %s", requestId, 
            request != null ? request.crawlerId : "null");
        
        try {
            // Validate request
            if (request == null) {
                LOG.warnf("Invalid crawl request [%s]: request is null", requestId);
                return CrawlResponse.error(
                    "unknown",
                    requestId,
                    "Request cannot be null",
                    "validation"
                );
            }
            
            if (request.crawlerId == null || request.crawlerId.trim().isEmpty()) {
                LOG.warnf("Invalid crawl request [%s]: missing crawler ID", requestId);
                return CrawlResponse.error(
                    "unknown",
                    requestId,
                    "Crawler ID is required",
                    "validation"
                );
            }
            
            // Get crawler configuration
            Optional<CrawlerConfiguration> configOpt = configurationService.getCrawlerConfiguration(request.crawlerId);
            if (configOpt.isEmpty()) {
                LOG.warnf("Crawler configuration not found [%s] for ID: %s", requestId, request.crawlerId);
                return CrawlResponse.error(
                    request.crawlerId,
                    requestId,
                    "Crawler configuration not found: " + request.crawlerId,
                    "configuration"
                );
            }
            
            CrawlerConfiguration config = configOpt.get();
            if (!config.enabled) {
                LOG.warnf("Crawler is disabled [%s] for ID: %s", requestId, request.crawlerId);
                return CrawlResponse.error(
                    request.crawlerId,
                    requestId,
                    "Crawler is disabled: " + request.crawlerId,
                    "configuration"
                );
            }
            
            // Check circuit breaker
            if (!circuitBreaker.allowRequest(request.crawlerId)) {
                LOG.warnf("Circuit breaker is open [%s] for crawler: %s", requestId, request.crawlerId);
                return CrawlResponse.serviceUnavailable(
                    request.crawlerId,
                    requestId,
                    "Crawler is temporarily unavailable due to previous failures"
                );
            }
            
            // Execute crawl with retry logic
            return executeWithRetry(config, requestId, MAX_RETRY_ATTEMPTS);
            
        } catch (Exception e) {
            LOG.errorf("Unexpected error processing crawl request [%s]: %s", requestId, e.getMessage(), e);
            return CrawlResponse.error(
                request.crawlerId,
                requestId,
                "Internal error: " + e.getMessage(),
                "internal"
            );
        }
    }
    
    /**
     * Gets the current status of a crawler.
     * 
     * @param crawlerId The ID of the crawler
     * @return Response containing status information
     */
    public Response getCrawlerStatus(String crawlerId) {
        String requestId = "status-proxy-" + System.currentTimeMillis();
        LOG.debugf("Getting status [%s] for crawler: %s", requestId, crawlerId);
        
        try {
            Optional<CrawlerConfiguration> configOpt = configurationService.getCrawlerConfiguration(crawlerId);
            if (configOpt.isEmpty()) {
                LOG.warnf("Crawler configuration not found [%s] for ID: %s", requestId, crawlerId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "error", "Crawler not found",
                        "crawlerId", crawlerId,
                        "requestId", requestId
                    ))
                    .build();
            }
            
            CrawlerConfiguration config = configOpt.get();
            CrawlerClient client = createClient(config);
            
            // Call the appropriate method based on crawler ID
            switch (crawlerId) {
                case "caspit":
                    return client.getCaspitStatusSync();
                case "drucker":
                    return client.getDruckerStatusSync();
                case "test-crawler":
                    return client.getTestStatusSync();
                default:
                    LOG.warnf("Unknown crawler ID [%s]: %s", requestId, crawlerId);
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                            "error", "Unknown crawler ID",
                            "crawlerId", crawlerId,
                            "requestId", requestId
                        ))
                        .build();
            }
            
        } catch (Exception e) {
            LOG.errorf("Error getting crawler status [%s]: %s", requestId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                    "error", "Failed to get crawler status",
                    "message", e.getMessage(),
                    "crawlerId", crawlerId,
                    "requestId", requestId
                ))
                .build();
        }
    }
    
    /**
     * Executes a crawl request with retry logic.
     */
    private CrawlResponse executeWithRetry(CrawlerConfiguration config, String requestId, int maxAttempts) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOG.debugf("Attempting crawl [%s] for %s (attempt %d/%d)", 
                    requestId, config.id, attempt, maxAttempts);
                
                CrawlerClient client = createClient(config);
                
                // Execute with timeout - call the appropriate method based on crawler ID
                CompletableFuture<Response> future;
                switch (config.id) {
                    case "caspit":
                        future = client.triggerCaspitCrawl().toCompletableFuture();
                        break;
                    case "drucker":
                        future = client.triggerDruckerCrawl().toCompletableFuture();
                        break;
                    case "test-crawler":
                        future = client.triggerTestCrawl().toCompletableFuture();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown crawler ID: " + config.id);
                }
                Response response = future.get(DEFAULT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                
                // Process successful response
                CrawlResponse result = processResponse(config, requestId, response);
                
                // Record success in circuit breaker
                circuitBreaker.recordSuccess(config.id);
                
                LOG.infof("Crawl request [%s] successful for %s on attempt %d", 
                    requestId, config.id, attempt);
                
                return result;
                
            } catch (TimeoutException e) {
                lastException = e;
                LOG.warnf("Crawl request [%s] timed out for %s on attempt %d/%d", 
                    requestId, config.id, attempt, maxAttempts);
                
                if (attempt < maxAttempts) {
                    sleep(RETRY_DELAY);
                }
                
            } catch (Exception e) {
                lastException = e;
                LOG.warnf("Crawl request [%s] failed for %s on attempt %d/%d: %s", 
                    requestId, config.id, attempt, maxAttempts, e.getMessage());
                
                // For certain errors, don't retry and return appropriate response immediately
                if (e instanceof WebApplicationException) {
                    WebApplicationException webEx = (WebApplicationException) e;
                    if (webEx.getResponse() != null && webEx.getResponse().getStatus() == 409) {
                        // Conflict - crawl already in progress, don't retry
                        LOG.infof("Crawl conflict detected [%s] for %s, returning conflict response", 
                            requestId, config.id);
                        return CrawlResponse.conflict(config.id, requestId, "RUNNING");
                    }
                } else if (e.getMessage() != null && e.getMessage().contains("status code 409")) {
                    // Handle ClientWebApplicationException which doesn't have getResponse()
                    LOG.infof("Crawl conflict detected [%s] for %s, returning conflict response", 
                        requestId, config.id);
                    return CrawlResponse.conflict(config.id, requestId, "RUNNING");
                }
                
                if (attempt < maxAttempts) {
                    sleep(RETRY_DELAY);
                }
            }
        }
        
        // All attempts failed - record failure and return error
        circuitBreaker.recordFailure(config.id);
        
        LOG.errorf("All crawl attempts failed [%s] for %s after %d attempts", 
            requestId, config.id, maxAttempts);
        
        String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
        String errorCategory = determineErrorCategory(lastException);
        
        return CrawlResponse.error(config.id, requestId, 
            "Failed after " + maxAttempts + " attempts: " + errorMessage, errorCategory);
    }
    
    /**
     * Processes the HTTP response from a crawler and converts it to CrawlResponse.
     */
    private CrawlResponse processResponse(CrawlerConfiguration config, String requestId, Response response) {
        try {
            int status = response.getStatus();
            
            if (status == 202) { // Accepted
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = response.readEntity(Map.class);
                
                String crawlId = (String) responseData.get("crawlId");
                String statusEndpoint = config.getStatusUrl();
                
                return CrawlResponse.accepted(config.id, requestId, crawlId, statusEndpoint);
                
            } else if (status == 409) { // Conflict
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = response.readEntity(Map.class);
                
                String currentStatus = (String) responseData.get("currentStatus");
                return CrawlResponse.conflict(config.id, requestId, currentStatus);
                
            } else if (status == 503) { // Service Unavailable
                return CrawlResponse.serviceUnavailable(config.id, requestId, 
                    "Crawler service is temporarily unavailable");
                
            } else {
                // Other error status codes
                String errorMessage = "HTTP " + status;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorData = response.readEntity(Map.class);
                    errorMessage = (String) errorData.getOrDefault("message", errorMessage);
                } catch (Exception e) {
                    LOG.debugf("Could not parse error response body: %s", e.getMessage());
                }
                
                return CrawlResponse.error(config.id, requestId, errorMessage, "http");
            }
            
        } catch (Exception e) {
            LOG.errorf("Error processing crawler response [%s]: %s", requestId, e.getMessage(), e);
            return CrawlResponse.error(config.id, requestId, 
                "Failed to process crawler response: " + e.getMessage(), "processing");
        }
    }
    
    /**
     * Creates a REST client for the specified crawler configuration.
     */
    private CrawlerClient createClient(CrawlerConfiguration config) {
        try {
            URI baseUri = URI.create(config.baseUrl);
            
            return RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build(CrawlerClient.class);
                
        } catch (Exception e) {
            LOG.errorf("Failed to create REST client for %s: %s", config.id, e.getMessage(), e);
            throw new RuntimeException("Failed to create REST client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines the error category based on the exception type.
     */
    private String determineErrorCategory(Exception exception) {
        if (exception instanceof TimeoutException) {
            return "timeout";
        } else if (exception instanceof ProcessingException) {
            return "network";
        } else if (exception instanceof WebApplicationException) {
            return "http";
        } else if (exception instanceof CompletionException) {
            return "async";
        } else {
            return "unknown";
        }
    }
    
    /**
     * Sleep for the specified duration, handling interruption.
     */
    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warnf("Sleep interrupted: %s", e.getMessage());
        }
    }
}