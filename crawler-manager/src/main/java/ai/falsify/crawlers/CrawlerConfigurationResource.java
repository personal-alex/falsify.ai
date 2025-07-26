package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.service.CrawlerConfigurationService;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST endpoint for managing crawler configurations.
 * Provides endpoints to list, retrieve, and validate crawler configurations.
 */
@Path("/api/crawlers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CrawlerConfigurationResource {
    
    private static final Logger LOG = Logger.getLogger(CrawlerConfigurationResource.class);
    
    @Inject
    CrawlerConfigurationService configurationService;
    
    /**
     * Gets all configured crawler instances.
     * @return List of all crawler configurations
     */
    @GET
    public Response getAllCrawlers() {
        try {
            List<CrawlerConfiguration> crawlers = configurationService.discoverCrawlers();
            LOG.info("Retrieved " + crawlers.size() + " crawler configurations");
            return Response.ok(crawlers).build();
        } catch (Exception e) {
            LOG.error("Failed to retrieve crawler configurations", e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to retrieve crawler configurations: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets only enabled crawler instances.
     * @return List of enabled crawler configurations
     */
    @GET
    @Path("/enabled")
    public Response getEnabledCrawlers() {
        try {
            List<CrawlerConfiguration> crawlers = configurationService.getEnabledCrawlers();
            LOG.info("Retrieved " + crawlers.size() + " enabled crawler configurations");
            return Response.ok(crawlers).build();
        } catch (Exception e) {
            LOG.error("Failed to retrieve enabled crawler configurations", e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to retrieve enabled crawler configurations: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets a specific crawler configuration by ID.
     * @param crawlerId The crawler ID to retrieve
     * @return The crawler configuration if found
     */
    @GET
    @Path("/{crawlerId}")
    public Response getCrawlerById(@PathParam("crawlerId") String crawlerId) {
        try {
            Optional<CrawlerConfiguration> crawler = configurationService.getCrawlerConfiguration(crawlerId);
            
            if (crawler.isPresent()) {
                LOG.info("Retrieved configuration for crawler: " + crawlerId);
                return Response.ok(crawler.get()).build();
            } else {
                LOG.warn("Crawler configuration not found: " + crawlerId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Crawler not found: " + crawlerId))
                        .build();
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve crawler configuration for: " + crawlerId, e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to retrieve crawler configuration: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Validates a crawler configuration.
     * @param configuration The configuration to validate
     * @return Validation result
     */
    @POST
    @Path("/validate")
    public Response validateConfiguration(CrawlerConfiguration configuration) {
        try {
            Set<ConstraintViolation<CrawlerConfiguration>> violations = 
                    configurationService.validateConfiguration(configuration);
            
            if (violations.isEmpty()) {
                LOG.info("Configuration validation passed for: " + configuration.id);
                return Response.ok(new ValidationResponse(true, "Configuration is valid", null)).build();
            } else {
                List<String> errors = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.toList());
                
                LOG.warn("Configuration validation failed for: " + configuration.id + ", errors: " + errors);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ValidationResponse(false, "Configuration validation failed", errors))
                        .build();
            }
        } catch (Exception e) {
            LOG.error("Failed to validate crawler configuration", e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to validate configuration: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Refreshes crawler configurations from properties.
     * @return Success response
     */
    @POST
    @Path("/refresh")
    public Response refreshConfigurations() {
        try {
            configurationService.refreshConfigurations();
            List<CrawlerConfiguration> crawlers = configurationService.discoverCrawlers();
            LOG.info("Successfully refreshed crawler configurations, found " + crawlers.size() + " crawlers");
            return Response.ok(new RefreshResponse(true, "Configurations refreshed successfully", crawlers.size()))
                    .build();
        } catch (Exception e) {
            LOG.error("Failed to refresh crawler configurations", e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to refresh configurations: " + e.getMessage()))
                    .build();
        }
    }
    
    // Response DTOs
    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
    
    public static class ValidationResponse {
        public boolean valid;
        public String message;
        public List<String> errors;
        
        public ValidationResponse(boolean valid, String message, List<String> errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
        }
    }
    
    public static class RefreshResponse {
        public boolean success;
        public String message;
        public int crawlerCount;
        
        public RefreshResponse(boolean success, String message, int crawlerCount) {
            this.success = success;
            this.message = message;
            this.crawlerCount = crawlerCount;
        }
    }
}