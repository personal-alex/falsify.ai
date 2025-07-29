package ai.falsify.crawlers.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing crawler metrics data.
 * Contains performance and operational metrics for a specific crawler.
 */
public class CrawlerMetrics {
    
    @NotNull
    public String crawlerId;
    
    public int articlesProcessed;
    
    public double successRate;
    
    public long averageProcessingTimeMs;
    
    public int errorCount;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant lastCrawlTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant lastUpdated;
    
    public List<MetricPoint> trendsData;
    
    public long totalCrawlsExecuted;
    
    public long totalExecutionTimeMs;
    
    public int activeCrawls;
    
    public CrawlerMetrics() {
        // Default constructor for Jackson
    }
    
    public CrawlerMetrics(String crawlerId) {
        this.crawlerId = crawlerId;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Updates metrics with data from a completed job.
     */
    public void updateFromJob(JobRecord job) {
        if (job.isCompleted() && job.endTime != null) {
            this.articlesProcessed += job.articlesProcessed;
            this.errorCount += job.articlesFailed;
            this.totalCrawlsExecuted++;
            
            Long jobDuration = job.getDurationMs();
            if (jobDuration != null) {
                this.totalExecutionTimeMs += jobDuration;
                this.averageProcessingTimeMs = this.totalExecutionTimeMs / this.totalCrawlsExecuted;
            }
            
            this.lastCrawlTime = job.endTime;
            this.lastUpdated = Instant.now();
            
            // Calculate success rate
            int totalArticles = job.getTotalArticlesAttempted();
            if (totalArticles > 0) {
                this.successRate = (double) this.articlesProcessed / (this.articlesProcessed + this.errorCount) * 100.0;
            }
        }
    }
    
    /**
     * Adds a metric point for trend tracking.
     */
    public void addTrendPoint(MetricPoint point) {
        if (this.trendsData == null) {
            this.trendsData = new ArrayList<>();
        }
        
        this.trendsData.add(point);
        
        // Keep only last 100 points to prevent memory issues
        if (this.trendsData.size() > 100) {
            this.trendsData.remove(0);
        }
    }
    
    /**
     * Resets metrics to initial state.
     */
    public void reset() {
        this.articlesProcessed = 0;
        this.successRate = 0.0;
        this.averageProcessingTimeMs = 0;
        this.errorCount = 0;
        this.totalCrawlsExecuted = 0;
        this.totalExecutionTimeMs = 0;
        this.activeCrawls = 0;
        this.lastUpdated = Instant.now();
        if (this.trendsData != null) {
            this.trendsData.clear();
        }
    }
    
    @Override
    public String toString() {
        return "CrawlerMetrics{" +
                "crawlerId='" + crawlerId + '\'' +
                ", articlesProcessed=" + articlesProcessed +
                ", successRate=" + successRate +
                ", averageProcessingTimeMs=" + averageProcessingTimeMs +
                ", errorCount=" + errorCount +
                ", lastCrawlTime=" + lastCrawlTime +
                ", lastUpdated=" + lastUpdated +
                ", totalCrawlsExecuted=" + totalCrawlsExecuted +
                ", activeCrawls=" + activeCrawls +
                '}';
    }
    
    /**
     * Represents a single metric data point for trend analysis.
     */
    public static class MetricPoint {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Instant timestamp;
        
        public int articlesProcessed;
        
        public double successRate;
        
        public long processingTimeMs;
        
        public int errorCount;
        
        public MetricPoint() {
            // Default constructor for Jackson
        }
        
        public MetricPoint(Instant timestamp, int articlesProcessed, double successRate, 
                          long processingTimeMs, int errorCount) {
            this.timestamp = timestamp;
            this.articlesProcessed = articlesProcessed;
            this.successRate = successRate;
            this.processingTimeMs = processingTimeMs;
            this.errorCount = errorCount;
        }
        
        public static MetricPoint fromMetrics(CrawlerMetrics metrics) {
            return new MetricPoint(
                Instant.now(),
                metrics.articlesProcessed,
                metrics.successRate,
                metrics.averageProcessingTimeMs,
                metrics.errorCount
            );
        }
        
        @Override
        public String toString() {
            return "MetricPoint{" +
                    "timestamp=" + timestamp +
                    ", articlesProcessed=" + articlesProcessed +
                    ", successRate=" + successRate +
                    ", processingTimeMs=" + processingTimeMs +
                    ", errorCount=" + errorCount +
                    '}';
        }
    }
}