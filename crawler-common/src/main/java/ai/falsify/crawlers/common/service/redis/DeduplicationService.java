package ai.falsify.crawlers.common.service.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Set;

/**
 * Service for handling deduplication of crawled content using Redis.
 * Provides configurable key patterns and expiration policies.
 */
@ApplicationScoped
public class DeduplicationService {

    private static final Logger LOG = Logger.getLogger(DeduplicationService.class);

    @Inject
    RedisService redisService;

    /**
     * Checks if a URL has already been processed by a specific crawler.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to check
     * @return true if the URL is new (not processed), false if it's a duplicate
     */
    public boolean isNewUrl(String crawlerName, String url) {
        String key = buildUrlKey(crawlerName, url);
        return redisService.setnx(key, "1");
    }

    /**
     * Checks if a URL has already been processed with expiration.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to check
     * @param expiration the expiration duration for the deduplication entry
     * @return true if the URL is new (not processed), false if it's a duplicate
     */
    public boolean isNewUrl(String crawlerName, String url, Duration expiration) {
        String key = buildUrlKey(crawlerName, url);
        return redisService.setnx(key, "1", expiration);
    }

    /**
     * Marks a URL as processed by a specific crawler.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to mark as processed
     */
    public void markUrlProcessed(String crawlerName, String url) {
        String key = buildUrlKey(crawlerName, url);
        redisService.set(key, "1");
        LOG.debugf("Marked URL as processed: %s", url);
    }

    /**
     * Marks a URL as processed with expiration.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to mark as processed
     * @param expiration the expiration duration
     */
    public void markUrlProcessed(String crawlerName, String url, Duration expiration) {
        String key = buildUrlKey(crawlerName, url);
        redisService.set(key, "1", expiration);
        LOG.debugf("Marked URL as processed with expiration %s: %s", expiration, url);
    }

    /**
     * Checks if a URL has been processed.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to check
     * @return true if the URL has been processed, false otherwise
     */
    public boolean isUrlProcessed(String crawlerName, String url) {
        String key = buildUrlKey(crawlerName, url);
        return redisService.exists(key);
    }

    /**
     * Removes a URL from the processed list.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL to remove
     * @return true if the URL was removed, false if it wasn't found
     */
    public boolean removeUrl(String crawlerName, String url) {
        String key = buildUrlKey(crawlerName, url);
        boolean removed = redisService.delete(key);
        if (removed) {
            LOG.debugf("Removed URL from processed list: %s", url);
        }
        return removed;
    }

    /**
     * Gets all processed URLs for a crawler.
     * 
     * @param crawlerName the name of the crawler
     * @return set of processed URLs
     */
    public Set<String> getProcessedUrls(String crawlerName) {
        String pattern = buildUrlPattern(crawlerName);
        Set<String> keys = redisService.keys(pattern);
        
        // Extract URLs from keys
        String prefix = buildUrlPrefix(crawlerName);
        return keys.stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Gets the count of processed URLs for a crawler.
     * 
     * @param crawlerName the name of the crawler
     * @return count of processed URLs
     */
    public long getProcessedUrlCount(String crawlerName) {
        String pattern = buildUrlPattern(crawlerName);
        return redisService.keys(pattern).size();
    }

    /**
     * Clears all processed URLs for a crawler.
     * 
     * @param crawlerName the name of the crawler
     * @return number of URLs cleared
     */
    public long clearProcessedUrls(String crawlerName) {
        String pattern = buildUrlPattern(crawlerName);
        Set<String> keys = redisService.keys(pattern);
        
        long cleared = 0;
        for (String key : keys) {
            if (redisService.delete(key)) {
                cleared++;
            }
        }
        
        LOG.infof("Cleared %d processed URLs for crawler: %s", cleared, crawlerName);
        return cleared;
    }

    /**
     * Builds a Redis key for URL deduplication.
     * 
     * @param crawlerName the name of the crawler
     * @param url the URL
     * @return the Redis key
     */
    private String buildUrlKey(String crawlerName, String url) {
        return String.format("crawler:%s:url:%s", crawlerName, hashUrl(url));
    }

    /**
     * Builds a Redis key pattern for URL deduplication.
     * 
     * @param crawlerName the name of the crawler
     * @return the Redis key pattern
     */
    private String buildUrlPattern(String crawlerName) {
        return String.format("crawler:%s:url:*", crawlerName);
    }

    /**
     * Builds a Redis key prefix for URL deduplication.
     * 
     * @param crawlerName the name of the crawler
     * @return the Redis key prefix
     */
    private String buildUrlPrefix(String crawlerName) {
        return String.format("crawler:%s:url:", crawlerName);
    }

    /**
     * Creates a hash of the URL for use as a Redis key.
     * This helps with very long URLs and ensures consistent key format.
     * 
     * @param url the URL to hash
     * @return the hashed URL
     */
    private String hashUrl(String url) {
        // For now, use the URL directly, but in production you might want to hash it
        // to handle very long URLs and special characters
        return url.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}