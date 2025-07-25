package ai.falsify.crawlers.common.service.redis;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for common Redis operations used across crawler implementations.
 * Provides abstraction over Redis operations with error handling and retry logic.
 */
public interface RedisService {

    /**
     * Sets a key-value pair if the key doesn't exist (SET IF NOT EXISTS).
     * 
     * @param key the Redis key
     * @param value the value to set
     * @return true if the key was set, false if it already existed
     */
    boolean setnx(String key, String value);

    /**
     * Sets a key-value pair with expiration if the key doesn't exist.
     * 
     * @param key the Redis key
     * @param value the value to set
     * @param expiration the expiration duration
     * @return true if the key was set, false if it already existed
     */
    boolean setnx(String key, String value, Duration expiration);

    /**
     * Gets the value for a key.
     * 
     * @param key the Redis key
     * @return Optional containing the value if it exists, empty otherwise
     */
    Optional<String> get(String key);

    /**
     * Sets a key-value pair.
     * 
     * @param key the Redis key
     * @param value the value to set
     */
    void set(String key, String value);

    /**
     * Sets a key-value pair with expiration.
     * 
     * @param key the Redis key
     * @param value the value to set
     * @param expiration the expiration duration
     */
    void set(String key, String value, Duration expiration);

    /**
     * Deletes a key.
     * 
     * @param key the Redis key to delete
     * @return true if the key was deleted, false if it didn't exist
     */
    boolean delete(String key);

    /**
     * Checks if a key exists.
     * 
     * @param key the Redis key
     * @return true if the key exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Sets expiration for a key.
     * 
     * @param key the Redis key
     * @param expiration the expiration duration
     * @return true if expiration was set, false if key doesn't exist
     */
    boolean expire(String key, Duration expiration);

    /**
     * Gets all keys matching a pattern.
     * 
     * @param pattern the key pattern (supports wildcards)
     * @return set of matching keys
     */
    Set<String> keys(String pattern);

    /**
     * Adds a value to a set.
     * 
     * @param key the Redis key for the set
     * @param value the value to add
     * @return true if the value was added, false if it already existed
     */
    boolean sadd(String key, String value);

    /**
     * Checks if a value exists in a set.
     * 
     * @param key the Redis key for the set
     * @param value the value to check
     * @return true if the value exists in the set, false otherwise
     */
    boolean sismember(String key, String value);

    /**
     * Removes a value from a set.
     * 
     * @param key the Redis key for the set
     * @param value the value to remove
     * @return true if the value was removed, false if it didn't exist
     */
    boolean srem(String key, String value);

    /**
     * Gets the size of a set.
     * 
     * @param key the Redis key for the set
     * @return the size of the set
     */
    long scard(String key);

    /**
     * Increments a counter.
     * 
     * @param key the Redis key for the counter
     * @return the new value after increment
     */
    long incr(String key);

    /**
     * Increments a counter by a specific amount.
     * 
     * @param key the Redis key for the counter
     * @param increment the amount to increment by
     * @return the new value after increment
     */
    long incrby(String key, long increment);

    /**
     * Gets the current value of a counter.
     * 
     * @param key the Redis key for the counter
     * @return the current counter value, 0 if key doesn't exist
     */
    long getCounter(String key);
}