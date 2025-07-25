package ai.falsify.crawlers.common.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Result of a crawling operation containing summary statistics and processed articles.
 */
public record CrawlResult(
    int totalArticlesFound,
    int articlesProcessed,
    int articlesSkipped,
    int articlesFailed,
    long processingTimeMs,
    List<String> errors,
    List<Article> articles,
    Instant startTime,
    Instant endTime,
    String crawlerSource
) {
    
    /**
     * Gets the success rate as a percentage.
     * 
     * @return success rate (0-100)
     */
    public double getSuccessRate() {
        if (totalArticlesFound == 0) return 0.0;
        return (double) articlesProcessed / totalArticlesFound * 100.0;
    }
    
    /**
     * Gets the processing duration.
     * 
     * @return Duration of the crawling operation
     */
    public Duration getProcessingDuration() {
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Gets articles processed per minute.
     * 
     * @return articles per minute rate
     */
    public double getArticlesPerMinute() {
        long minutes = getProcessingDuration().toMinutes();
        return minutes > 0 ? (double) articlesProcessed / minutes : 0.0;
    }
    
    /**
     * Checks if the crawl was successful (no critical errors).
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return errors.isEmpty() && articlesProcessed > 0;
    }
    
    /**
     * Builder for creating CrawlResult instances.
     */
    public static class Builder {
        private int totalArticlesFound = 0;
        private int articlesProcessed = 0;
        private int articlesSkipped = 0;
        private int articlesFailed = 0;
        private long processingTimeMs = 0;
        private List<String> errors = List.of();
        private List<Article> articles = List.of();
        private Instant startTime = Instant.now();
        private Instant endTime = Instant.now();
        private String crawlerSource = "unknown";
        
        public Builder totalArticlesFound(int totalArticlesFound) {
            this.totalArticlesFound = totalArticlesFound;
            return this;
        }
        
        public Builder articlesProcessed(int articlesProcessed) {
            this.articlesProcessed = articlesProcessed;
            return this;
        }
        
        public Builder articlesSkipped(int articlesSkipped) {
            this.articlesSkipped = articlesSkipped;
            return this;
        }
        
        public Builder articlesFailed(int articlesFailed) {
            this.articlesFailed = articlesFailed;
            return this;
        }
        
        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public Builder errors(List<String> errors) {
            this.errors = errors != null ? errors : List.of();
            return this;
        }
        
        public Builder articles(List<Article> articles) {
            this.articles = articles != null ? articles : List.of();
            return this;
        }
        
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder crawlerSource(String crawlerSource) {
            this.crawlerSource = crawlerSource;
            return this;
        }
        
        public CrawlResult build() {
            return new CrawlResult(
                totalArticlesFound,
                articlesProcessed,
                articlesSkipped,
                articlesFailed,
                processingTimeMs,
                errors,
                articles,
                startTime,
                endTime,
                crawlerSource
            );
        }
    }
}