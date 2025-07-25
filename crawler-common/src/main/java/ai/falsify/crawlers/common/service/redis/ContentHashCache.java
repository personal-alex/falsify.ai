package ai.falsify.crawlers.common.service.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-based cache for content hashes used in duplicate content detection.
 * Provides efficient storage and retrieval of content hashes with configurable expiration.
 */
@ApplicationScoped
public class ContentHashCache {

    private static final Logger LOG = Logger.getLogger(ContentHashCache.class);
    private static final String HASH_KEY_PREFIX = "content:hash:";
    private static final String HASH_SET_KEY = "content:hashes";

    @Inject
    RedisService redisService;

    /**
     * Checks if a content hash already exists in the cache.
     * 
     * @param contentHash the content hash to check
     * @return true if the hash exists (duplicate content), false if it's new
     */
    public boolean isDuplicateContent(String contentHash) {
        boolean exists = redisService.sismember(HASH_SET_KEY, contentHash);
        LOG.debugf("Content hash duplicate check: hash=%s, isDuplicate=%s", 
                contentHash.substring(0, Math.min(8, contentHash.length())), exists);
        return exists;
    }

    /**
     * Adds a content hash to the cache.
     * 
     * @param contentHash the content hash to add
     * @return true if the hash was added (new content), false if it already existed
     */
    public boolean addContentHash(String contentHash) {
        boolean added = redisService.sadd(HASH_SET_KEY, contentHash);
        if (added) {
            LOG.debugf("Added new content hash: %s", 
                    contentHash.substring(0, Math.min(8, contentHash.length())));
        }
        return added;
    }

    /**
     * Adds a content hash with additional metadata.
     * 
     * @param contentHash the content hash
     * @param url the URL of the content
     * @param crawlerName the name of the crawler
     * @return true if the hash was added, false if it already existed
     */
    public boolean addContentHash(String contentHash, String url, String crawlerName) {
        boolean added = addContentHash(contentHash);
        
        if (added) {
            // Store metadata for the hash
            String metadataKey = HASH_KEY_PREFIX + contentHash;
            String metadata = String.format("url=%s,crawler=%s,timestamp=%d", 
                    url, crawlerName, System.currentTimeMillis());
            redisService.set(metadataKey, metadata);
        }
        
        return added;
    }

    /**
     * Adds a content hash with expiration.
     * 
     * @param contentHash the content hash
     * @param url the URL of the content
     * @param crawlerName the name of the crawler
     * @param expiration the expiration duration
     * @return true if the hash was added, false if it already existed
     */
    public boolean addContentHash(String contentHash, String url, String crawlerName, Duration expiration) {
        boolean added = addContentHash(contentHash, url, crawlerName);
        
        if (added) {
            // Set expiration for the metadata
            String metadataKey = HASH_KEY_PREFIX + contentHash;
            redisService.expire(metadataKey, expiration);
            
            // Note: Redis sets don't support per-member expiration,
            // so we rely on periodic cleanup for the main hash set
        }
        
        return added;
    }

    /**
     * Removes a content hash from the cache.
     * 
     * @param contentHash the content hash to remove
     * @return true if the hash was removed, false if it didn't exist
     */
    public boolean removeContentHash(String contentHash) {
        boolean removed = redisService.srem(HASH_SET_KEY, contentHash);
        
        if (removed) {
            // Also remove metadata
            String metadataKey = HASH_KEY_PREFIX + contentHash;
            redisService.delete(metadataKey);
            LOG.debugf("Removed content hash: %s", 
                    contentHash.substring(0, Math.min(8, contentHash.length())));
        }
        
        return removed;
    }

    /**
     * Gets the metadata for a content hash.
     * 
     * @param contentHash the content hash
     * @return metadata string if found, null otherwise
     */
    public String getContentHashMetadata(String contentHash) {
        String metadataKey = HASH_KEY_PREFIX + contentHash;
        return redisService.get(metadataKey).orElse(null);
    }

    /**
     * Gets the total number of content hashes in the cache.
     * 
     * @return the number of content hashes
     */
    public long getContentHashCount() {
        return redisService.scard(HASH_SET_KEY);
    }

    /**
     * Gets all content hashes in the cache.
     * Note: This operation can be expensive for large caches.
     * 
     * @return set of all content hashes
     */
    public Set<String> getAllContentHashes() {
        // This would require SMEMBERS command which isn't directly available
        // in the current RedisService interface. For now, return empty set.
        LOG.warn("getAllContentHashes() not implemented - would require SMEMBERS command");
        return Set.of();
    }

    /**
     * Clears all content hashes from the cache.
     * 
     * @return true if the cache was cleared
     */
    public boolean clearCache() {
        boolean cleared = redisService.delete(HASH_SET_KEY);
        
        // Also clear all metadata keys
        Set<String> metadataKeys = redisService.keys(HASH_KEY_PREFIX + "*");
        for (String key : metadataKeys) {
            redisService.delete(key);
        }
        
        LOG.info("Content hash cache cleared");
        return cleared;
    }

    /**
     * Performs cleanup of expired metadata entries.
     * This should be called periodically to clean up orphaned metadata.
     */
    public void cleanup() {
        Set<String> metadataKeys = redisService.keys(HASH_KEY_PREFIX + "*");
        int cleaned = 0;
        
        for (String metadataKey : metadataKeys) {
            if (!redisService.exists(metadataKey)) {
                // Metadata key has expired, remove corresponding hash from set
                String contentHash = metadataKey.substring(HASH_KEY_PREFIX.length());
                if (redisService.srem(HASH_SET_KEY, contentHash)) {
                    cleaned++;
                }
            }
        }
        
        if (cleaned > 0) {
            LOG.infof("Cleaned up %d expired content hashes", cleaned);
        }
    }

    /**
     * Generates a content hash from the given content.
     * Uses a normalized version of the content to handle minor formatting differences.
     * 
     * @param content the content to hash
     * @return the content hash
     */
    public String generateContentHash(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        // Normalize content for hashing
        String normalized = content
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("[\\p{Punct}&&[^.!?]]", "") // Remove punctuation except sentence endings
                .toLowerCase()
                .trim();

        return String.valueOf(normalized.hashCode());
    }
}