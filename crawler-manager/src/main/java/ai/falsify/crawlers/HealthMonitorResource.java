package ai.falsify.crawlers;

import ai.falsify.crawlers.model.HealthStatus;
import ai.falsify.crawlers.service.HealthMonitorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * REST resource for health monitoring operations.
 * Provides endpoints to check crawler health status and force health checks.
 */
@Path("/api/health")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HealthMonitorResource {

    private static final Logger LOG = Logger.getLogger(HealthMonitorResource.class);

    @Inject
    HealthMonitorService healthMonitorService;

    /**
     * Gets health status for all configured crawlers.
     */
    @GET
    public Response getAllCrawlerHealth() {
        LOG.debug("Received request to get all crawler health statuses");
        try {
            LOG.debug("Calling healthMonitorService.getAllCrawlerHealth()");
            Map<String, HealthStatus> healthStatuses = healthMonitorService.getAllCrawlerHealth();
            LOG.infof("Successfully retrieved health status for %d crawlers", healthStatuses.size());

            // Log individual crawler statuses for debugging
            healthStatuses.forEach((crawlerId, status) -> LOG.debugf("Crawler %s: status=%s, lastCheck=%s", crawlerId,
                    status.status, status.lastCheck));

            return Response.ok(healthStatuses).build();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve crawler health statuses. Exception type: %s, Message: %s",
                    e.getClass().getSimpleName(), e.getMessage());

            // Log stack trace for debugging
            LOG.debug("Full stack trace:", e);

            return Response.serverError()
                    .entity(Map.of(
                            "error", "Failed to retrieve health statuses: " + e.getMessage(),
                            "type", e.getClass().getSimpleName(),
                            "timestamp", java.time.Instant.now().toString()))
                    .build();
        }
    }

    /**
     * Gets health status for a specific crawler.
     */
    @GET
    @Path("/{crawlerId}")
    public Response getCrawlerHealth(@PathParam("crawlerId") String crawlerId) {
        LOG.debugf("Received request to get health status for crawler: %s", crawlerId);
        try {
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                LOG.warn("Received request with null or empty crawlerId");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID cannot be null or empty"))
                        .build();
            }

            LOG.debugf("Calling healthMonitorService.getCrawlerHealth(%s)", crawlerId);
            HealthStatus healthStatus = healthMonitorService.getCrawlerHealth(crawlerId);
            LOG.infof("Successfully retrieved health status for crawler %s: status=%s, lastCheck=%s",
                    crawlerId, healthStatus.status, healthStatus.lastCheck);
            return Response.ok(healthStatus).build();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve health status for crawler %s. Exception type: %s, Message: %s",
                    crawlerId, e.getClass().getSimpleName(), e.getMessage());

            // Log stack trace for debugging
            LOG.debug("Full stack trace:", e);

            return Response.serverError()
                    .entity(Map.of(
                            "error", "Failed to retrieve health status: " + e.getMessage(),
                            "crawlerId", crawlerId,
                            "type", e.getClass().getSimpleName(),
                            "timestamp", java.time.Instant.now().toString()))
                    .build();
        }
    }

    /**
     * Forces a health check for a specific crawler (bypasses circuit breaker).
     */
    @POST
    @Path("/{crawlerId}/check")
    public Response forceHealthCheck(@PathParam("crawlerId") String crawlerId) {
        LOG.debugf("Received request to force health check for crawler: %s", crawlerId);
        try {
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                LOG.warn("Received force health check request with null or empty crawlerId");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Crawler ID cannot be null or empty"))
                        .build();
            }

            LOG.debugf("Calling healthMonitorService.forceHealthCheck(%s)", crawlerId);
            HealthStatus healthStatus = healthMonitorService.forceHealthCheck(crawlerId);
            LOG.infof("Successfully forced health check for crawler %s: status=%s, responseTime=%s",
                    crawlerId, healthStatus.status, healthStatus.responseTimeMs);
            return Response.ok(healthStatus).build();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to force health check for crawler %s. Exception type: %s, Message: %s",
                    crawlerId, e.getClass().getSimpleName(), e.getMessage());

            // Log stack trace for debugging
            LOG.debug("Full stack trace:", e);

            return Response.serverError()
                    .entity(Map.of(
                            "error", "Failed to perform health check: " + e.getMessage(),
                            "crawlerId", crawlerId,
                            "type", e.getClass().getSimpleName(),
                            "timestamp", java.time.Instant.now().toString()))
                    .build();
        }
    }
}