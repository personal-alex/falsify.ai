package ai.falsify;

import ai.falsify.crawlers.CaspitCrawler;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.control.ActivateRequestContext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST resource for managing Ben Caspit crawler operations.
 * Provides endpoints for triggering crawls and checking crawler status.
 */
@Path("/caspit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CaspitResource {

    private static final Logger LOG = Logger.getLogger(CaspitResource.class);

    private final CaspitCrawler crawler;

    // Simple in-memory status tracking (in production, this would be externalized)
    private final AtomicBoolean crawlInProgress = new AtomicBoolean(false);
    private final AtomicReference<String> lastCrawlStatus = new AtomicReference<>("IDLE");
    private final AtomicReference<String> lastCrawlTime = new AtomicReference<>("Never");
    private final AtomicReference<String> lastCrawlResult = new AtomicReference<>("No crawls executed yet");
    private final AtomicReference<Integer> lastArticleCount = new AtomicReference<>(0);

    @Inject
    public CaspitResource(CaspitCrawler crawler) {
        this.crawler = crawler;
    }

    /**
     * Trigger a crawl of Ben Caspit articles with comprehensive error handling and
     * monitoring.
     * Returns immediately with crawl initiation status.
     */
    @POST
    @Path("/crawl")
    public Response startCrawl() {
        String requestId = "crawl-" + System.currentTimeMillis();
        LOG.infof("Received crawl request [%s] to start Ben Caspit crawl", requestId);

        try {
            // Validate crawler dependency
            if (crawler == null) {
                LOG.errorf("Crawler dependency is null [%s] - service not properly initialized", requestId);
                Map<String, Object> errorResponse = Map.of(
                        "status", "error",
                        "message", "Crawler service not available",
                        "requestId", requestId,
                        "timestamp", getCurrentTimestamp());
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(errorResponse)
                        .build();
            }

            // Check if crawl is already in progress with detailed logging
            if (!crawlInProgress.compareAndSet(false, true)) {
                LOG.warnf("Crawl request [%s] rejected - crawl already in progress. Current status: %s",
                        requestId, lastCrawlStatus.get());

                Map<String, Object> errorResponse = Map.of(
                        "status", "conflict",
                        "message", "Crawl already in progress",
                        "currentStatus", lastCrawlStatus.get(),
                        "lastCrawlTime", lastCrawlTime.get(),
                        "requestId", requestId,
                        "timestamp", getCurrentTimestamp());
                return Response.status(Response.Status.CONFLICT)
                        .entity(errorResponse)
                        .build();
            }

            // Update status with detailed tracking
            String startTimestamp = getCurrentTimestamp();
            lastCrawlStatus.set("STARTING");
            lastCrawlTime.set(startTimestamp);
            lastCrawlResult.set("Crawl initialization in progress");

            LOG.infof("Crawl [%s] accepted and initializing at %s", requestId, startTimestamp);

            // Start crawl asynchronously using Mutiny which is better integrated with
            // Quarkus CDI
            Uni.createFrom().item(() -> {
                String crawlId = "async-" + requestId;
                long crawlStartTime = System.currentTimeMillis();

                try {
                    LOG.infof("Starting asynchronous crawl execution [%s]", crawlId);
                    lastCrawlStatus.set("RUNNING");
                    lastCrawlResult.set("Crawl in progress - collecting articles");

                    // Execute the crawl with timeout monitoring and proper CDI context
                    ai.falsify.crawlers.common.model.CrawlResult crawlResult = executeCrawlWithContext();

                    // Calculate execution time
                    long executionTime = System.currentTimeMillis() - crawlStartTime;

                    // Update status with detailed success information
                    lastCrawlStatus.set("COMPLETED");
                    lastCrawlResult.set(String.format("Successfully crawled %d articles in %d ms",
                            crawlResult.articles().size(), executionTime));
                    lastArticleCount.set(crawlResult.articles().size());

                    LOG.infof("Crawl [%s] completed successfully - processed %d articles in %d ms (%.2f articles/sec)",
                            crawlId, crawlResult.articles().size(), executionTime,
                            executionTime > 0 ? (crawlResult.articles().size() * 1000.0 / executionTime) : 0);

                    return crawlResult;

                } catch (IOException ioException) {
                    // Handle IO-specific errors (network, file system, etc.)
                    long executionTime = System.currentTimeMillis() - crawlStartTime;
                    String errorMessage = String.format("Crawl IO error after %d ms: %s",
                            executionTime, ioException.getMessage());

                    lastCrawlStatus.set("FAILED_IO");
                    lastCrawlResult.set(errorMessage);
                    lastArticleCount.set(0);

                    LOG.errorf("Crawl [%s] failed with IO error: %s", crawlId, ioException.getMessage(), ioException);
                    throw new RuntimeException(ioException);

                } catch (IllegalArgumentException argException) {
                    // Handle configuration or argument errors
                    long executionTime = System.currentTimeMillis() - crawlStartTime;
                    String errorMessage = String.format("Crawl configuration error after %d ms: %s",
                            executionTime, argException.getMessage());

                    lastCrawlStatus.set("FAILED_CONFIG");
                    lastCrawlResult.set(errorMessage);
                    lastArticleCount.set(0);

                    LOG.errorf("Crawl [%s] failed with configuration error: %s", crawlId, argException.getMessage(),
                            argException);
                    throw new RuntimeException(argException);

                } catch (RuntimeException runtimeException) {
                    // Handle runtime errors (WebDriver, database, etc.)
                    long executionTime = System.currentTimeMillis() - crawlStartTime;
                    String errorMessage = String.format("Crawl runtime error after %d ms: %s",
                            executionTime, runtimeException.getMessage());

                    lastCrawlStatus.set("FAILED_RUNTIME");
                    lastCrawlResult.set(errorMessage);
                    lastArticleCount.set(0);

                    LOG.errorf("Crawl [%s] failed with runtime error: %s", crawlId, runtimeException.getMessage(),
                            runtimeException);
                    throw runtimeException;

                } catch (Exception generalException) {
                    // Handle any other unexpected errors
                    long executionTime = System.currentTimeMillis() - crawlStartTime;
                    String errorMessage = String.format("Crawl unexpected error after %d ms: %s",
                            executionTime, generalException.getMessage());

                    lastCrawlStatus.set("FAILED_UNKNOWN");
                    lastCrawlResult.set(errorMessage);
                    lastArticleCount.set(0);

                    LOG.errorf("Crawl [%s] failed with unexpected error: %s", crawlId, generalException.getMessage(),
                            generalException);
                    throw new RuntimeException(generalException);

                } finally {
                    // Always reset the in-progress flag and log completion
                    crawlInProgress.set(false);
                    long totalTime = System.currentTimeMillis() - crawlStartTime;
                    LOG.infof("Crawl [%s] execution completed, total time: %d ms, final status: %s",
                            crawlId, totalTime, lastCrawlStatus.get());
                }
            })
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .subscribe().with(
                            result -> {
                                // Success handling is already done in the try block above
                                LOG.debugf("Crawl [%s] subscription completed successfully", requestId);
                            },
                            failure -> {
                                // Handle subscription failures
                                LOG.errorf("Asynchronous crawl execution failed [%s]: %s", requestId,
                                        failure.getMessage(), failure);

                                lastCrawlStatus.set("FAILED_ASYNC");
                                lastCrawlResult.set("Asynchronous execution failed: " + failure.getMessage());
                                lastArticleCount.set(0);
                                crawlInProgress.set(false);
                            });

            // Return immediate response with detailed information
            Map<String, Object> response = new HashMap<>();
            response.put("status", "accepted");
            response.put("message", "Crawl started successfully");
            response.put("requestId", requestId);
            response.put("crawlId", "caspit-" + System.currentTimeMillis());
            response.put("timestamp", startTimestamp);
            response.put("estimatedDuration", "Variable (depends on article count and site responsiveness)");
            response.put("statusEndpoint", "/caspit/status");

            LOG.infof("Crawl request [%s] accepted and started asynchronously", requestId);

            return Response.status(Response.Status.ACCEPTED)
                    .entity(response)
                    .build();

        } catch (Exception e) {
            LOG.errorf("Critical error starting crawl [%s]: %s", requestId, e.getMessage(), e);

            // Reset status on unexpected error with detailed error information
            crawlInProgress.set(false);
            lastCrawlStatus.set("ERROR_STARTUP");
            lastCrawlResult.set("Failed to start crawl: " + e.getMessage());

            // Determine appropriate HTTP status based on error type
            Response.Status httpStatus;
            String errorCategory;

            if (e instanceof IllegalArgumentException) {
                httpStatus = Response.Status.BAD_REQUEST;
                errorCategory = "configuration";
            } else if (e instanceof SecurityException) {
                httpStatus = Response.Status.FORBIDDEN;
                errorCategory = "security";
            } else {
                httpStatus = Response.Status.INTERNAL_SERVER_ERROR;
                errorCategory = "internal";
            }

            Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "category", errorCategory,
                    "message", "Failed to start crawl: " + e.getMessage(),
                    "requestId", requestId,
                    "timestamp", getCurrentTimestamp(),
                    "suggestion", "Check logs for detailed error information and retry after resolving the issue");

            return Response.status(httpStatus)
                    .entity(errorResponse)
                    .build();
        }
    }

    /**
     * Get the current status of the crawler with comprehensive error handling and
     * detailed information.
     * Returns information about current and last crawl operations.
     */
    @GET
    @Path("/status")
    public Response getCrawlStatus() {
        String requestId = "status-" + System.currentTimeMillis();
        LOG.debugf("Received status request [%s]", requestId);

        try {
            // Validate internal state consistency
            String currentStatus = lastCrawlStatus.get();
            boolean inProgress = crawlInProgress.get();

            // Check for inconsistent state and log warnings
            if (inProgress && !isActiveStatus(currentStatus)) {
                LOG.warnf("Inconsistent crawler state detected [%s]: inProgress=true but status=%s", requestId,
                        currentStatus);
            } else if (!inProgress && isActiveStatus(currentStatus)) {
                LOG.warnf("Inconsistent crawler state detected [%s]: inProgress=false but status=%s", requestId,
                        currentStatus);
            }

            Map<String, Object> statusResponse = new HashMap<>();
            statusResponse.put("currentStatus", currentStatus);
            statusResponse.put("crawlInProgress", inProgress);
            statusResponse.put("lastCrawlTime", lastCrawlTime.get());
            statusResponse.put("lastCrawlResult", lastCrawlResult.get());
            statusResponse.put("lastArticleCount", lastArticleCount.get());
            statusResponse.put("timestamp", getCurrentTimestamp());
            statusResponse.put("requestId", requestId);

            // Add detailed status information and recommendations
            String message;
            String recommendation = null;
            String severity = "info";

            switch (currentStatus) {
                case "STARTING":
                    message = "Crawl is initializing";
                    recommendation = "Wait for crawl to begin processing articles";
                    break;
                case "RUNNING":
                    message = "Crawl is currently in progress";
                    recommendation = "Monitor progress through periodic status checks";
                    break;
                case "COMPLETED":
                    message = "Last crawl completed successfully";
                    recommendation = "Ready for new crawl requests";
                    break;
                case "FAILED_IO":
                    message = "Last crawl failed due to network or I/O issues";
                    recommendation = "Check network connectivity and target site availability before retrying";
                    severity = "warning";
                    break;
                case "FAILED_CONFIG":
                    message = "Last crawl failed due to configuration issues";
                    recommendation = "Review crawler configuration settings and fix any invalid values";
                    severity = "error";
                    break;
                case "FAILED_RUNTIME":
                    message = "Last crawl failed due to runtime errors";
                    recommendation = "Check logs for WebDriver or database issues and resolve before retrying";
                    severity = "error";
                    break;
                case "FAILED_UNKNOWN":
                    message = "Last crawl failed due to unexpected errors";
                    recommendation = "Review detailed logs and contact support if issue persists";
                    severity = "error";
                    break;
                case "FAILED_ASYNC":
                    message = "Last crawl failed during asynchronous execution";
                    recommendation = "Check system resources and retry the crawl operation";
                    severity = "error";
                    break;
                case "ERROR_STARTUP":
                    message = "Crawler failed to start properly";
                    recommendation = "Check service configuration and dependencies";
                    severity = "error";
                    break;
                case "IDLE":
                default:
                    message = "Crawler is idle and ready for new requests";
                    recommendation = "Submit a POST request to /caspit/crawl to start crawling";
                    break;
            }

            statusResponse.put("message", message);
            statusResponse.put("severity", severity);
            if (recommendation != null) {
                statusResponse.put("recommendation", recommendation);
            }

            // Add operational metrics if available
            if (lastArticleCount.get() > 0) {
                statusResponse.put("metrics", Map.of(
                        "lastArticleCount", lastArticleCount.get(),
                        "hasRecentSuccess", "COMPLETED".equals(currentStatus)));
            }

            // Add service health indicators
            Map<String, Object> healthIndicators = new HashMap<>();
            healthIndicators.put("crawlerService", crawler != null ? "available" : "unavailable");
            healthIndicators.put("stateConsistency",
                    (inProgress == isActiveStatus(currentStatus)) ? "consistent" : "inconsistent");
            statusResponse.put("health", healthIndicators);

            LOG.debugf("Status request [%s] completed: status=%s, inProgress=%s, severity=%s",
                    requestId, currentStatus, inProgress, severity);

            return Response.ok(statusResponse).build();

        } catch (Exception e) {
            LOG.errorf("Critical error retrieving crawl status [%s]: %s", requestId, e.getMessage(), e);

            // Provide detailed error response with troubleshooting information
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve crawler status");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("requestId", requestId);
            errorResponse.put("timestamp", getCurrentTimestamp());
            errorResponse.put("severity", "critical");
            errorResponse.put("recommendation",
                    "Service may be experiencing internal issues - check logs and restart if necessary");

            // Try to provide partial status information if possible
            try {
                errorResponse.put("partialInfo", Map.of(
                        "crawlInProgress", crawlInProgress.get(),
                        "lastKnownStatus", lastCrawlStatus.get()));
            } catch (Exception partialException) {
                LOG.debugf("Could not retrieve partial status information: %s", partialException.getMessage());
            }

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponse)
                    .build();
        }
    }

    /**
     * Execute crawl with proper CDI context and transaction activation
     */
    @ActivateRequestContext
    public ai.falsify.crawlers.common.model.CrawlResult executeCrawlWithContext() throws IOException {
        return crawler.crawl();
    }

    /**
     * Helper method to determine if a status indicates active crawling
     */
    private boolean isActiveStatus(String status) {
        return "STARTING".equals(status) || "RUNNING".equals(status);
    }

    /**
     * Health check endpoint for the crawler service.
     * Provides basic service availability information.
     */
    @GET
    @Path("/health")
    public Response getHealth() {
        LOG.debug("Health check requested");

        try {
            Map<String, Object> healthResponse = Map.of(
                    "service", "caspit-crawler",
                    "status", "healthy",
                    "timestamp", getCurrentTimestamp(),
                    "version", "1.0.0");

            return Response.ok(healthResponse).build();

        } catch (Exception e) {
            LOG.errorf("Health check failed: %s", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                    "service", "caspit-crawler",
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", getCurrentTimestamp());

            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(errorResponse)
                    .build();
        }
    }

    /**
     * Get current timestamp in ISO format for consistent response formatting.
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}