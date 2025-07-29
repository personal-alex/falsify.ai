package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import ai.falsify.crawlers.model.CrawlerMetrics;
import ai.falsify.crawlers.model.JobRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for collecting and aggregating crawler metrics.
 * Provides scheduled collection, Redis caching, and trend calculation.
 */
@ApplicationScoped
public class MetricsCollectorService {
    
    private static final String METRICS_KEY_PREFIX = "crawler:metrics:";
    private static final String TRENDS_KEY_PREFIX = "crawler:trends:";
    private static final Duration METRICS_TTL = Duration.ofHours(24);
    private static final Duration TRENDS_TTL = Duration.ofDays(7);
    
    @Inject
    CrawlerConfigurationService configurationService;
    
    @Inject
    WebSocketNotificationService webSocketService;
    
    @Inject
    RedisDataSource redisDataSource;
    
    @Inject
    ObjectMapper objectMapper;
    
    // In-memory cache for quick access
    private final ConcurrentHashMap<String, CrawlerMetrics> metricsCache = new ConcurrentHashMap<>();
    
    private ValueCommands<String, String> redisCommands;
    
    /**
     * Initialize Redis commands after injection.
     */
    public void init() {
        if (redisDataSource != null) {
            this.redisCommands = redisDataSource.value(String.class, String.class);
        }
    }
    
    /**
     * Scheduled method to collect metrics from all configured crawlers.
     * Runs every 30 seconds as configured in application.properties.
     */
    @Scheduled(every = "${crawler.manager.metrics-collection.interval:30s}")
    @Transactional
    public void collectMetrics() {
        Log.debug("Starting scheduled metrics collection");
        
        try {
            List<CrawlerConfiguration> crawlers = configurationService.getAllCrawlers();
            
            for (CrawlerConfiguration crawler : crawlers) {
                if (crawler.enabled) {
                    collectCrawlerMetrics(crawler.id);
                }
            }
            
            Log.debugf("Completed metrics collection for %d crawlers", crawlers.size());
            
        } catch (Exception e) {
            Log.errorf(e, "Error during scheduled metrics collection");
        }
    }
    
    /**
     * Collects metrics for a specific crawler.
     */
    public void collectCrawlerMetrics(String crawlerId) {
        try {
            Log.debugf("Collecting metrics for crawler: %s", crawlerId);
            
            // Get or create metrics object
            CrawlerMetrics metrics = metricsCache.computeIfAbsent(crawlerId, CrawlerMetrics::new);
            
            // Collect metrics from database
            updateMetricsFromDatabase(metrics, crawlerId);
            
            // Add trend point
            CrawlerMetrics.MetricPoint trendPoint = CrawlerMetrics.MetricPoint.fromMetrics(metrics);
            metrics.addTrendPoint(trendPoint);
            
            // Cache in Redis
            cacheMetrics(metrics);
            
            // Broadcast update via WebSocket
            webSocketService.broadcastMetricsUpdated(metrics);
            
            Log.debugf("Successfully collected metrics for crawler: %s", crawlerId);
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to collect metrics for crawler: %s", crawlerId);
        }
    }
    
    /**
     * Updates metrics based on job records from the database.
     */
    @Transactional
    protected void updateMetricsFromDatabase(CrawlerMetrics metrics, String crawlerId) {
        // Get recent completed jobs (last 24 hours)
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        
        List<JobRecord> recentJobs = JobRecord.find(
            "crawlerId = ?1 and startTime >= ?2 and status != ?3", 
            crawlerId, since, JobRecord.JobStatus.RUNNING
        ).list();
        
        // Reset metrics for recalculation
        int totalArticlesProcessed = 0;
        int totalErrors = 0;
        long totalExecutionTime = 0;
        int completedJobs = 0;
        Instant lastCrawlTime = null;
        
        // Count active crawls
        long activeCrawls = JobRecord.count("crawlerId = ?1 and status = ?2", 
            crawlerId, JobRecord.JobStatus.RUNNING);
        
        for (JobRecord job : recentJobs) {
            totalArticlesProcessed += job.articlesProcessed;
            totalErrors += job.articlesFailed;
            
            if (job.isCompleted() && job.getDurationMs() != null) {
                totalExecutionTime += job.getDurationMs();
                completedJobs++;
                
                if (lastCrawlTime == null || job.endTime.isAfter(lastCrawlTime)) {
                    lastCrawlTime = job.endTime;
                }
            }
        }
        
        // Update metrics
        metrics.articlesProcessed = totalArticlesProcessed;
        metrics.errorCount = totalErrors;
        metrics.totalCrawlsExecuted = completedJobs;
        metrics.totalExecutionTimeMs = totalExecutionTime;
        metrics.activeCrawls = (int) activeCrawls;
        metrics.lastCrawlTime = lastCrawlTime;
        
        // Calculate derived metrics
        if (completedJobs > 0) {
            metrics.averageProcessingTimeMs = totalExecutionTime / completedJobs;
        }
        
        int totalArticles = totalArticlesProcessed + totalErrors;
        if (totalArticles > 0) {
            metrics.successRate = (double) totalArticlesProcessed / totalArticles * 100.0;
        }
        
        metrics.lastUpdated = Instant.now();
        
        Log.debugf("Updated metrics for %s: processed=%d, errors=%d, success_rate=%.2f%%, active=%d", 
            crawlerId, totalArticlesProcessed, totalErrors, metrics.successRate, metrics.activeCrawls);
    }
    
    /**
     * Caches metrics in Redis.
     */
    protected void cacheMetrics(CrawlerMetrics metrics) {
        if (redisCommands == null) {
            init();
        }
        
        if (redisCommands != null) {
            try {
                String key = METRICS_KEY_PREFIX + metrics.crawlerId;
                String json = objectMapper.writeValueAsString(metrics);
                redisCommands.setex(key, METRICS_TTL.getSeconds(), json);
                
                Log.debugf("Cached metrics for crawler %s in Redis", metrics.crawlerId);
                
            } catch (JsonProcessingException e) {
                Log.warnf(e, "Failed to serialize metrics for Redis cache: %s", metrics.crawlerId);
            } catch (Exception e) {
                Log.warnf(e, "Failed to cache metrics in Redis for crawler: %s", metrics.crawlerId);
            }
        }
    }
    
    /**
     * Retrieves metrics for a specific crawler.
     */
    public CrawlerMetrics getMetrics(String crawlerId) {
        // Try in-memory cache first
        CrawlerMetrics metrics = metricsCache.get(crawlerId);
        if (metrics != null) {
            return metrics;
        }
        
        // Try Redis cache
        metrics = getMetricsFromRedis(crawlerId);
        if (metrics != null) {
            metricsCache.put(crawlerId, metrics);
            return metrics;
        }
        
        // Generate fresh metrics if not cached
        metrics = new CrawlerMetrics(crawlerId);
        collectCrawlerMetrics(crawlerId);
        
        return metricsCache.getOrDefault(crawlerId, metrics);
    }
    
    /**
     * Retrieves metrics from Redis cache.
     */
    protected CrawlerMetrics getMetricsFromRedis(String crawlerId) {
        if (redisCommands == null) {
            init();
        }
        
        if (redisCommands != null) {
            try {
                String key = METRICS_KEY_PREFIX + crawlerId;
                String json = redisCommands.get(key);
                
                if (json != null) {
                    return objectMapper.readValue(json, CrawlerMetrics.class);
                }
                
            } catch (Exception e) {
                Log.warnf(e, "Failed to retrieve metrics from Redis for crawler: %s", crawlerId);
            }
        }
        
        return null;
    }
    
    /**
     * Gets metrics for all configured crawlers.
     */
    public Map<String, CrawlerMetrics> getAllMetrics() {
        Map<String, CrawlerMetrics> allMetrics = new HashMap<>();
        
        List<CrawlerConfiguration> crawlers = configurationService.getAllCrawlers();
        for (CrawlerConfiguration crawler : crawlers) {
            if (crawler.enabled) {
                CrawlerMetrics metrics = getMetrics(crawler.id);
                allMetrics.put(crawler.id, metrics);
            }
        }
        
        return allMetrics;
    }
    
    /**
     * Gets historical metrics for a specific time range.
     */
    public CrawlerMetrics getMetrics(String crawlerId, Duration timeRange) {
        CrawlerMetrics metrics = new CrawlerMetrics(crawlerId);
        
        try {
            // Calculate time range
            Instant since = Instant.now().minus(timeRange);
            
            // Get jobs within time range
            List<JobRecord> jobs = JobRecord.find(
                "crawlerId = ?1 and startTime >= ?2", 
                crawlerId, since
            ).list();
            
            // Calculate metrics for the time range
            calculateMetricsFromJobs(metrics, jobs);
            
            // Ensure trends data is initialized
            if (metrics.trendsData == null) {
                metrics.trendsData = new ArrayList<>();
            }
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to get historical metrics for crawler %s", crawlerId);
        }
        
        return metrics;
    }
    
    /**
     * Calculates metrics from a list of job records.
     */
    protected void calculateMetricsFromJobs(CrawlerMetrics metrics, List<JobRecord> jobs) {
        int totalArticlesProcessed = 0;
        int totalErrors = 0;
        long totalExecutionTime = 0;
        int completedJobs = 0;
        Instant lastCrawlTime = null;
        int activeCrawls = 0;
        
        List<CrawlerMetrics.MetricPoint> trendPoints = new ArrayList<>();
        
        for (JobRecord job : jobs) {
            totalArticlesProcessed += job.articlesProcessed;
            totalErrors += job.articlesFailed;
            
            if (job.status == JobRecord.JobStatus.RUNNING) {
                activeCrawls++;
            }
            
            if (job.isCompleted() && job.getDurationMs() != null) {
                totalExecutionTime += job.getDurationMs();
                completedJobs++;
                
                if (lastCrawlTime == null || job.endTime.isAfter(lastCrawlTime)) {
                    lastCrawlTime = job.endTime;
                }
                
                // Create trend point for completed job
                double jobSuccessRate = job.getSuccessRate();
                CrawlerMetrics.MetricPoint point = new CrawlerMetrics.MetricPoint(
                    job.endTime, job.articlesProcessed, jobSuccessRate, 
                    job.getDurationMs(), job.articlesFailed
                );
                trendPoints.add(point);
            }
        }
        
        // Update metrics
        metrics.articlesProcessed = totalArticlesProcessed;
        metrics.errorCount = totalErrors;
        metrics.totalCrawlsExecuted = completedJobs;
        metrics.totalExecutionTimeMs = totalExecutionTime;
        metrics.activeCrawls = activeCrawls;
        metrics.lastCrawlTime = lastCrawlTime;
        metrics.trendsData = trendPoints;
        
        // Calculate derived metrics
        if (completedJobs > 0) {
            metrics.averageProcessingTimeMs = totalExecutionTime / completedJobs;
        }
        
        int totalArticles = totalArticlesProcessed + totalErrors;
        if (totalArticles > 0) {
            metrics.successRate = (double) totalArticlesProcessed / totalArticles * 100.0;
        }
        
        metrics.lastUpdated = Instant.now();
    }
    
    /**
     * Clears cached metrics for a specific crawler.
     */
    public void clearMetricsCache(String crawlerId) {
        metricsCache.remove(crawlerId);
        
        if (redisDataSource != null && redisDataSource.key() != null) {
            try {
                String key = METRICS_KEY_PREFIX + crawlerId;
                redisDataSource.key().del(key);
                Log.debugf("Cleared metrics cache for crawler: %s", crawlerId);
            } catch (Exception e) {
                Log.warnf(e, "Failed to clear Redis cache for crawler: %s", crawlerId);
            }
        }
    }
    
    /**
     * Clears all cached metrics.
     */
    public void clearAllMetricsCache() {
        metricsCache.clear();
        Log.debug("Cleared all in-memory metrics cache");
    }
    
    /**
     * Gets the number of cached metrics entries.
     */
    public int getCachedMetricsCount() {
        return metricsCache.size();
    }
}