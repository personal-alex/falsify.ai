package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.HealthStatus;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for monitoring the health of all configured crawlers.
 * Performs scheduled health checks, caches results in Redis, and broadcasts
 * status changes via WebSocket to connected clients.
 */
@ApplicationScoped
@ServerEndpoint("/ws/health")
public class HealthMonitorService {

    private static final Logger LOG = Logger.getLogger(HealthMonitorService.class);
    private static final String HEALTH_CACHE_PREFIX = "crawler:health:";
    private static final Duration HEALTH_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(10);

    @Inject
    CrawlerConfigurationService configurationService;

    @Inject
    CircuitBreaker circuitBreaker;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, HealthStatus> healthCache;
    private final Map<String, HealthStatus> localCache = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Session> webSocketSessions = new CopyOnWriteArraySet<>();

    @jakarta.annotation.PostConstruct
    void init() {
        if (redisDataSource != null) {
            this.healthCache = redisDataSource.value(HealthStatus.class);
        }
    }

    /**
     * Scheduled method that performs health checks on all configured crawlers.
     * Runs every 10 seconds as configured in application.properties.
     */
    @Scheduled(every = "10s")
    public void performHealthChecks() {
        LOG.debug("Starting scheduled health checks for all crawlers");

        List<CrawlerConfiguration> crawlers = configurationService.getAllCrawlers();

        // Perform health checks asynchronously for all crawlers
        List<CompletableFuture<Void>> healthCheckFutures = crawlers.stream()
                .filter(crawler -> crawler.enabled)
                .map(this::performHealthCheckAsync)
                .toList();

        // Wait for all health checks to complete (with timeout)
        CompletableFuture<Void> allHealthChecks = CompletableFuture.allOf(
                healthCheckFutures.toArray(new CompletableFuture[0]));

        try {
            allHealthChecks.get(30, TimeUnit.SECONDS);
            LOG.debug("Completed health checks for all crawlers");
        } catch (Exception e) {
            LOG.warn("Some health checks did not complete within timeout", e);
        }
    }

    /**
     * Performs an asynchronous health check for a single crawler.
     */
    private CompletableFuture<Void> performHealthCheckAsync(CrawlerConfiguration crawler) {
        return CompletableFuture.runAsync(() -> {
            try {
                HealthStatus status = performHealthCheck(crawler);
                updateHealthStatus(crawler.id, status);
            } catch (Exception e) {
                LOG.errorf(e, "Error performing health check for crawler %s", crawler.id);
                HealthStatus errorStatus = HealthStatus.unhealthy(crawler.id,
                        "Health check failed: " + e.getMessage());
                updateHealthStatus(crawler.id, errorStatus);
            }
        });
    }

    /**
     * Performs a health check for a specific crawler.
     */
    public HealthStatus performHealthCheck(CrawlerConfiguration crawler) {
        LOG.debugf("Performing health check for crawler %s", crawler.id);

        // Check circuit breaker
        if (!circuitBreaker.allowRequest(crawler.id)) {
            LOG.debugf("Circuit breaker is open for crawler %s, skipping health check", crawler.id);
            return HealthStatus.unhealthy(crawler.id,
                    "Circuit breaker is open - too many recent failures");
        }

        try {
            // Use the configured health endpoint URL directly
            String healthUrl = crawler.getHealthUrl();
            LOG.debugf("Checking health at URL: %s", healthUrl);
            
            // Create REST client for this crawler using the full health URL
            CrawlerHealthClient client = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(crawler.baseUrl))
                    .connectTimeout(HEALTH_CHECK_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .readTimeout(HEALTH_CHECK_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .build(CrawlerHealthClient.class);

            Instant startTime = Instant.now();
            Response response;
            
            // Call the appropriate method based on crawler ID
            switch (crawler.id) {
                case "caspit":
                    response = client.checkCaspitHealthSync();
                    break;
                case "drucker":
                    response = client.checkDruckerHealthSync();
                    break;
                case "test-crawler":
                    response = client.checkTestHealthSync();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown crawler ID: " + crawler.id);
            }
            
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();

            if (response.getStatus() == 200) {
                circuitBreaker.recordSuccess(crawler.id);
                LOG.debugf("Health check successful for crawler %s (response time: %dms)",
                        crawler.id, responseTime);
                return HealthStatus.healthy(crawler.id, responseTime);
            } else {
                circuitBreaker.recordFailure(crawler.id);
                String message = String.format("Health check returned status %d", response.getStatus());
                LOG.warnf("Health check failed for crawler %s: %s", crawler.id, message);
                return HealthStatus.unhealthy(crawler.id, message);
            }

        } catch (Exception e) {
            circuitBreaker.recordFailure(crawler.id);
            String message = "Health check failed: " + e.getMessage();
            LOG.warnf("Health check failed for crawler %s: %s", crawler.id, message);
            return HealthStatus.unhealthy(crawler.id, message);
        }
    }

    /**
     * Updates the health status for a crawler in both local and Redis cache.
     */
    private void updateHealthStatus(String crawlerId, HealthStatus newStatus) {
        HealthStatus previousStatus = localCache.get(crawlerId);

        // Update local cache
        localCache.put(crawlerId, newStatus);

        // Update Redis cache if available
        if (healthCache != null) {
            try {
                healthCache.setex(HEALTH_CACHE_PREFIX + crawlerId, HEALTH_CACHE_TTL.getSeconds(), newStatus);
            } catch (Exception e) {
                LOG.warnf(e, "Failed to update Redis cache for crawler %s health status", crawlerId);
            }
        }

        // Broadcast status change via WebSocket if status actually changed
        if (previousStatus == null || !previousStatus.status.equals(newStatus.status)) {
            broadcastHealthStatusChange(newStatus);
            LOG.infof("Health status changed for crawler %s: %s -> %s",
                    crawlerId,
                    previousStatus != null ? previousStatus.status : "UNKNOWN",
                    newStatus.status);
        }
    }

    /**
     * Gets the current health status for a specific crawler.
     */
    public HealthStatus getCrawlerHealth(String crawlerId) {
        LOG.debugf("Getting health status for crawler: %s", crawlerId);
        
        if (crawlerId == null || crawlerId.trim().isEmpty()) {
            LOG.warn("Received null or empty crawlerId");
            throw new IllegalArgumentException("Crawler ID cannot be null or empty");
        }
        
        try {
            // Try local cache first
            HealthStatus status = localCache.get(crawlerId);
            if (status != null) {
                LOG.debugf("Found health status in local cache for crawler %s: %s", crawlerId, status.status);
                return status;
            }

            // Try Redis cache
            if (healthCache != null) {
                try {
                    LOG.debugf("Checking Redis cache for crawler %s", crawlerId);
                    status = healthCache.get(HEALTH_CACHE_PREFIX + crawlerId);
                    if (status != null) {
                        LOG.debugf("Found health status in Redis cache for crawler %s: %s", crawlerId, status.status);
                        localCache.put(crawlerId, status); // Update local cache
                        return status;
                    } else {
                        LOG.debugf("No health status found in Redis cache for crawler %s", crawlerId);
                    }
                } catch (Exception e) {
                    LOG.warnf(e, "Failed to retrieve health status from Redis for crawler %s", crawlerId);
                }
            } else {
                LOG.debug("Redis health cache is not available");
            }

            // Return unknown status if not found
            LOG.debugf("No cached health data found for crawler %s, returning unknown status", crawlerId);
            return HealthStatus.unknown(crawlerId, "No health data available");
        } catch (Exception e) {
            LOG.errorf(e, "Error getting health status for crawler %s", crawlerId);
            throw new RuntimeException("Failed to get health status for crawler: " + crawlerId, e);
        }
    }

    /**
     * Gets health status for all configured crawlers.
     */
    public Map<String, HealthStatus> getAllCrawlerHealth() {
        LOG.debug("Getting health status for all configured crawlers");
        
        try {
            List<CrawlerConfiguration> crawlers = configurationService.getAllCrawlers();
            LOG.debugf("Found %d configured crawlers", crawlers.size());
            
            Map<String, HealthStatus> healthStatuses = new ConcurrentHashMap<>();

            for (CrawlerConfiguration crawler : crawlers) {
                try {
                    LOG.debugf("Getting health status for crawler: %s", crawler.id);
                    HealthStatus health = getCrawlerHealth(crawler.id);
                    healthStatuses.put(crawler.id, health);
                    LOG.debugf("Successfully retrieved health for crawler %s: %s", crawler.id, health.status);
                } catch (Exception e) {
                    LOG.errorf(e, "Failed to get health status for crawler %s", crawler.id);
                    // Add an error status instead of failing completely
                    healthStatuses.put(crawler.id, HealthStatus.unknown(crawler.id, 
                            "Error retrieving health status: " + e.getMessage()));
                }
            }

            LOG.debugf("Successfully retrieved health status for %d crawlers", healthStatuses.size());
            return healthStatuses;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to get all crawler health statuses");
            throw new RuntimeException("Failed to retrieve crawler health statuses", e);
        }
    }

    /**
     * Forces a health check for a specific crawler (bypasses circuit breaker).
     */
    public HealthStatus forceHealthCheck(String crawlerId) {
        CrawlerConfiguration crawler = configurationService.getCrawlerById(crawlerId);
        if (crawler == null) {
            return HealthStatus.unknown(crawlerId, "Crawler configuration not found");
        }

        // Temporarily reset circuit breaker for this check
        circuitBreaker.reset(crawlerId);

        HealthStatus status = performHealthCheck(crawler);
        updateHealthStatus(crawlerId, status);

        return status;
    }

    /**
     * Broadcasts health status changes to all connected WebSocket clients.
     */
    private void broadcastHealthStatusChange(HealthStatus healthStatus) {
        if (webSocketSessions.isEmpty()) {
            return;
        }

        String message = String.format(
                "{\"type\":\"health.updated\",\"crawlerId\":\"%s\",\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                healthStatus.crawlerId,
                healthStatus.status,
                healthStatus.message != null ? healthStatus.message.replace("\"", "\\\"") : "",
                healthStatus.lastCheck);

        webSocketSessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                    return false;
                } else {
                    return true; // Remove closed sessions
                }
            } catch (Exception e) {
                LOG.warnf(e, "Failed to send health status update to WebSocket client");
                return true; // Remove problematic sessions
            }
        });

        LOG.debugf("Broadcasted health status change to %d WebSocket clients", webSocketSessions.size());
    }

    // WebSocket event handlers

    @OnOpen
    public void onWebSocketOpen(Session session) {
        webSocketSessions.add(session);
        LOG.debugf("WebSocket client connected for health monitoring. Total clients: %d",
                webSocketSessions.size());

        // Send current health status to new client
        try {
            Map<String, HealthStatus> allHealth = getAllCrawlerHealth();
            for (HealthStatus status : allHealth.values()) {
                String message = String.format(
                        "{\"type\":\"health.current\",\"crawlerId\":\"%s\",\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                        status.crawlerId,
                        status.status,
                        status.message != null ? status.message.replace("\"", "\\\"") : "",
                        status.lastCheck);
                session.getAsyncRemote().sendText(message);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to send current health status to new WebSocket client");
        }
    }

    @OnClose
    public void onWebSocketClose(Session session) {
        webSocketSessions.remove(session);
        LOG.debugf("WebSocket client disconnected from health monitoring. Total clients: %d",
                webSocketSessions.size());
    }
}