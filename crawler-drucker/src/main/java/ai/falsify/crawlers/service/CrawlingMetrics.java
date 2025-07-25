package ai.falsify.crawlers.service;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and tracking crawler performance metrics.
 * Provides comprehensive monitoring of processing times, success/failure rates,
 * network request latencies, and database operation times.
 */
@ApplicationScoped
public class CrawlingMetrics {

    private static final Logger LOG = Logger.getLogger(CrawlingMetrics.class);

    @Inject
    CrawlerConfiguration config;

    // Overall metrics
    private final AtomicInteger totalArticlesProcessed = new AtomicInteger(0);
    private final AtomicInteger successfulArticles = new AtomicInteger(0);
    private final AtomicInteger failedArticles = new AtomicInteger(0);

    // Timing metrics (in milliseconds)
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong totalNetworkTime = new AtomicLong(0);
    private final AtomicLong totalDatabaseTime = new AtomicLong(0);

    // Detailed timing tracking
    private final ConcurrentHashMap<String, TimingMetric> operationMetrics = new ConcurrentHashMap<>();

    // Session tracking
    private volatile Instant sessionStartTime = Instant.now();
    private final AtomicInteger currentSessionArticles = new AtomicInteger(0);

    /**
     * Records the start of an article processing operation.
     *
     * @param url the article URL being processed
     * @return a MetricsContext for tracking this operation
     */
    public MetricsContext startArticleProcessing(String url) {
        if (!config.performance().enableMetrics()) {
            return new NoOpMetricsContext();
        }

        LOG.debugf("Starting metrics tracking for article: %s", url);
        return new MetricsContextImpl(url, Instant.now());
    }

    /**
     * Records the completion of an article processing operation.
     *
     * @param context the metrics context from startArticleProcessing
     * @param success whether the operation was successful
     */
    public void recordArticleCompletion(MetricsContext context, boolean success) {
        if (!config.performance().enableMetrics() || context instanceof NoOpMetricsContext) {
            return;
        }

        MetricsContextImpl ctx = (MetricsContextImpl) context;
        long processingTime = Duration.between(ctx.startTime, Instant.now()).toMillis();

        totalArticlesProcessed.incrementAndGet();
        currentSessionArticles.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);

        if (success) {
            successfulArticles.incrementAndGet();
            LOG.debugf("Article processing completed successfully in %dms: %s", processingTime, ctx.url);
        } else {
            failedArticles.incrementAndGet();
            LOG.debugf("Article processing failed after %dms: %s", processingTime, ctx.url);
        }

        // Update operation-specific metrics
        updateOperationMetrics("article_processing", processingTime);
    }

    /**
     * Records network operation timing.
     *
     * @param operationType the type of network operation (e.g., "http_request", "page_fetch")
     * @param duration the duration of the operation
     */
    public void recordNetworkOperation(String operationType, Duration duration) {
        if (!config.performance().enableMetrics()) {
            return;
        }

        long durationMs = duration.toMillis();
        totalNetworkTime.addAndGet(durationMs);
        updateOperationMetrics("network_" + operationType, durationMs);

        LOG.debugf("Network operation %s completed in %dms", operationType, durationMs);
    }

    /**
     * Records database operation timing.
     *
     * @param operationType the type of database operation (e.g., "save", "query", "transaction")
     * @param duration the duration of the operation
     */
    public void recordDatabaseOperation(String operationType, Duration duration) {
        if (!config.performance().enableMetrics()) {
            return;
        }

        long durationMs = duration.toMillis();
        totalDatabaseTime.addAndGet(durationMs);
        updateOperationMetrics("database_" + operationType, durationMs);

        LOG.debugf("Database operation %s completed in %dms", operationType, durationMs);
    }

    /**
     * Gets comprehensive metrics summary.
     *
     * @return MetricsSummary containing all collected metrics
     */
    public MetricsSummary getSummary() {
        Duration sessionDuration = Duration.between(sessionStartTime, Instant.now());
        
        return new MetricsSummary(
            totalArticlesProcessed.get(),
            successfulArticles.get(),
            failedArticles.get(),
            calculateSuccessRate(),
            totalProcessingTime.get(),
            calculateAverageProcessingTime(),
            totalNetworkTime.get(),
            calculateAverageNetworkTime(),
            totalDatabaseTime.get(),
            calculateAverageDatabaseTime(),
            sessionDuration,
            calculateArticlesPerMinute(sessionDuration),
            new ConcurrentHashMap<>(operationMetrics)
        );
    }

    /**
     * Resets all metrics (useful for testing or new crawling sessions).
     */
    public void reset() {
        totalArticlesProcessed.set(0);
        successfulArticles.set(0);
        failedArticles.set(0);
        totalProcessingTime.set(0);
        totalNetworkTime.set(0);
        totalDatabaseTime.set(0);
        currentSessionArticles.set(0);
        operationMetrics.clear();
        sessionStartTime = Instant.now();
        
        LOG.info("Crawling metrics reset");
    }

    /**
     * Logs current metrics summary.
     */
    public void logSummary() {
        if (!config.performance().enableMetrics()) {
            LOG.debug("Metrics collection is disabled");
            return;
        }

        MetricsSummary summary = getSummary();
        LOG.infof("=== Crawling Metrics Summary ===");
        LOG.infof("Total Articles: %d (Success: %d, Failed: %d)", 
            summary.totalArticles(), summary.successfulArticles(), summary.failedArticles());
        LOG.infof("Success Rate: %.2f%%", summary.successRate());
        LOG.infof("Average Processing Time: %.2fms", summary.averageProcessingTime());
        LOG.infof("Average Network Time: %.2fms", summary.averageNetworkTime());
        LOG.infof("Average Database Time: %.2fms", summary.averageDatabaseTime());
        LOG.infof("Articles per Minute: %.2f", summary.articlesPerMinute());
        LOG.infof("Session Duration: %s", formatDuration(summary.sessionDuration()));
    }

    // Private helper methods

    private void updateOperationMetrics(String operation, long durationMs) {
        operationMetrics.compute(operation, (key, existing) -> {
            if (existing == null) {
                return new TimingMetric(1, durationMs, durationMs, durationMs);
            } else {
                return new TimingMetric(
                    existing.count() + 1,
                    existing.totalTime() + durationMs,
                    Math.min(existing.minTime(), durationMs),
                    Math.max(existing.maxTime(), durationMs)
                );
            }
        });
    }

    private double calculateSuccessRate() {
        int total = totalArticlesProcessed.get();
        return total > 0 ? (double) successfulArticles.get() / total * 100.0 : 0.0;
    }

    private double calculateAverageProcessingTime() {
        int total = totalArticlesProcessed.get();
        return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
    }

    private double calculateAverageNetworkTime() {
        int total = totalArticlesProcessed.get();
        return total > 0 ? (double) totalNetworkTime.get() / total : 0.0;
    }

    private double calculateAverageDatabaseTime() {
        int total = totalArticlesProcessed.get();
        return total > 0 ? (double) totalDatabaseTime.get() / total : 0.0;
    }

    private double calculateArticlesPerMinute(Duration sessionDuration) {
        long minutes = sessionDuration.toMinutes();
        return minutes > 0 ? (double) currentSessionArticles.get() / minutes : 0.0;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Inner classes and interfaces

    /**
     * Context for tracking metrics of a single operation.
     */
    public interface MetricsContext {
        String getUrl();
        Instant getStartTime();
    }

    /**
     * Implementation of MetricsContext for active tracking.
     */
    private static class MetricsContextImpl implements MetricsContext {
        private final String url;
        private final Instant startTime;

        public MetricsContextImpl(String url, Instant startTime) {
            this.url = url;
            this.startTime = startTime;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public Instant getStartTime() {
            return startTime;
        }
    }

    /**
     * No-op implementation when metrics are disabled.
     */
    private static class NoOpMetricsContext implements MetricsContext {
        @Override
        public String getUrl() {
            return "";
        }

        @Override
        public Instant getStartTime() {
            return Instant.now();
        }
    }

    /**
     * Record for timing metrics of specific operations.
     */
    public record TimingMetric(int count, long totalTime, long minTime, long maxTime) {
        public double averageTime() {
            return count > 0 ? (double) totalTime / count : 0.0;
        }
    }

    /**
     * Comprehensive metrics summary record.
     */
    public record MetricsSummary(
        int totalArticles,
        int successfulArticles,
        int failedArticles,
        double successRate,
        long totalProcessingTime,
        double averageProcessingTime,
        long totalNetworkTime,
        double averageNetworkTime,
        long totalDatabaseTime,
        double averageDatabaseTime,
        Duration sessionDuration,
        double articlesPerMinute,
        ConcurrentHashMap<String, TimingMetric> operationMetrics
    ) {}
}