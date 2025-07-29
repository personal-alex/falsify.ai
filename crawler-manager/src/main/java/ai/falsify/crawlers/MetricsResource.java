package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlerMetrics;
import ai.falsify.crawlers.service.MetricsCollectorService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Map;

/**
 * REST resource for crawler metrics operations.
 * Provides endpoints for retrieving metrics data and triggering collection.
 */
@Path("/api/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource {
    
    @Inject
    MetricsCollectorService metricsCollectorService;
    
    /**
     * Gets metrics for all configured crawlers.
     * 
     * @return Map of crawler ID to metrics data
     */
    @GET
    public Response getAllMetrics() {
        try {
            Log.debug("Getting metrics for all crawlers");
            
            Map<String, CrawlerMetrics> allMetrics = metricsCollectorService.getAllMetrics();
            
            Log.debugf("Retrieved metrics for %d crawlers", allMetrics.size());
            return Response.ok(allMetrics).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve all metrics");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve metrics", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets metrics for a specific crawler.
     * 
     * @param crawlerId The crawler identifier
     * @return Metrics data for the specified crawler
     */
    @GET
    @Path("/{crawlerId}")
    public Response getCrawlerMetrics(@PathParam("crawlerId") String crawlerId) {
        try {
            Log.debugf("Getting metrics for crawler: %s", crawlerId);
            
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID is required"))
                        .build();
            }
            
            CrawlerMetrics metrics = metricsCollectorService.getMetrics(crawlerId);
            
            if (metrics == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Metrics not found for crawler: " + crawlerId))
                        .build();
            }
            
            Log.debugf("Retrieved metrics for crawler %s: processed=%d, success_rate=%.2f%%", 
                      crawlerId, metrics.articlesProcessed, metrics.successRate);
            
            return Response.ok(metrics).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve metrics for crawler: %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve metrics", 
                                  "message", e.getMessage(),
                                  "crawlerId", crawlerId))
                    .build();
        }
    }
    
    /**
     * Gets historical metrics for a specific crawler within a time range.
     * 
     * @param crawlerId The crawler identifier
     * @param hours Number of hours to look back (default: 24)
     * @return Historical metrics data
     */
    @GET
    @Path("/{crawlerId}/history")
    public Response getCrawlerMetricsHistory(@PathParam("crawlerId") String crawlerId,
                                           @QueryParam("hours") @DefaultValue("24") int hours) {
        try {
            Log.debugf("Getting historical metrics for crawler %s (last %d hours)", crawlerId, hours);
            
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID is required"))
                        .build();
            }
            
            if (hours <= 0 || hours > 168) { // Max 1 week
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Hours must be between 1 and 168 (1 week)"))
                        .build();
            }
            
            Duration timeRange = Duration.ofHours(hours);
            CrawlerMetrics metrics = metricsCollectorService.getMetrics(crawlerId, timeRange);
            
            Log.debugf("Retrieved historical metrics for crawler %s: %d trend points", 
                      crawlerId, metrics.trendsData != null ? metrics.trendsData.size() : 0);
            
            return Response.ok(metrics).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve historical metrics for crawler: %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve historical metrics", 
                                  "message", e.getMessage(),
                                  "crawlerId", crawlerId))
                    .build();
        }
    }
    
    /**
     * Triggers immediate metrics collection for a specific crawler.
     * 
     * @param crawlerId The crawler identifier
     * @return Success response
     */
    @POST
    @Path("/{crawlerId}/collect")
    public Response triggerMetricsCollection(@PathParam("crawlerId") String crawlerId) {
        try {
            Log.debugf("Triggering metrics collection for crawler: %s", crawlerId);
            
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID is required"))
                        .build();
            }
            
            metricsCollectorService.collectCrawlerMetrics(crawlerId);
            
            Log.debugf("Successfully triggered metrics collection for crawler: %s", crawlerId);
            
            return Response.ok(Map.of(
                "message", "Metrics collection triggered successfully",
                "crawlerId", crawlerId,
                "timestamp", System.currentTimeMillis()
            )).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to trigger metrics collection for crawler: %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to trigger metrics collection", 
                                  "message", e.getMessage(),
                                  "crawlerId", crawlerId))
                    .build();
        }
    }
    
    /**
     * Triggers immediate metrics collection for all crawlers.
     * 
     * @return Success response
     */
    @POST
    @Path("/collect")
    public Response triggerAllMetricsCollection() {
        try {
            Log.debug("Triggering metrics collection for all crawlers");
            
            metricsCollectorService.collectMetrics();
            
            Log.debug("Successfully triggered metrics collection for all crawlers");
            
            return Response.ok(Map.of(
                "message", "Metrics collection triggered for all crawlers",
                "timestamp", System.currentTimeMillis()
            )).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to trigger metrics collection for all crawlers");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to trigger metrics collection", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Clears cached metrics for a specific crawler.
     * 
     * @param crawlerId The crawler identifier
     * @return Success response
     */
    @DELETE
    @Path("/{crawlerId}/cache")
    public Response clearMetricsCache(@PathParam("crawlerId") String crawlerId) {
        try {
            Log.debugf("Clearing metrics cache for crawler: %s", crawlerId);
            
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID is required"))
                        .build();
            }
            
            metricsCollectorService.clearMetricsCache(crawlerId);
            
            Log.debugf("Successfully cleared metrics cache for crawler: %s", crawlerId);
            
            return Response.ok(Map.of(
                "message", "Metrics cache cleared successfully",
                "crawlerId", crawlerId,
                "timestamp", System.currentTimeMillis()
            )).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to clear metrics cache for crawler: %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to clear metrics cache", 
                                  "message", e.getMessage(),
                                  "crawlerId", crawlerId))
                    .build();
        }
    }
    
    /**
     * Gets metrics collection status and statistics.
     * 
     * @return Status information about metrics collection
     */
    @GET
    @Path("/status")
    public Response getMetricsStatus() {
        try {
            Log.debug("Getting metrics collection status");
            
            int cachedMetricsCount = metricsCollectorService.getCachedMetricsCount();
            
            Map<String, Object> status = Map.of(
                "cachedMetricsCount", cachedMetricsCount,
                "collectionEnabled", true,
                "lastUpdate", System.currentTimeMillis()
            );
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to get metrics status");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get metrics status", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
}