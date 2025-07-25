package ai.falsify.crawlers.common.service.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of RedisService using Quarkus Redis client.
 * Provides error handling and logging for Redis operations.
 */
@ApplicationScoped
public class RedisServiceImpl implements RedisService {

    private static final Logger LOG = Logger.getLogger(RedisServiceImpl.class);

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final SetCommands<String, String> setCommands;

    @Inject
    public RedisServiceImpl(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.setCommands = redisDataSource.set(String.class);
    }

    @Override
    public boolean setnx(String key, String value) {
        try {
            Boolean result = valueCommands.setnx(key, value);
            LOG.debugf("SETNX operation: key=%s, result=%s", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SETNX for key: %s", key);
            throw new RedisOperationException("SETNX failed for key: " + key, e);
        }
    }

    @Override
    public boolean setnx(String key, String value, Duration expiration) {
        try {
            // Use setnx first, then set expiration if successful
            Boolean result = valueCommands.setnx(key, value);
            if (Boolean.TRUE.equals(result)) {
                keyCommands.expire(key, expiration);
            }
            LOG.debugf("SETNX with expiration operation: key=%s, expiration=%s, result=%s", key, expiration, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SETNX with expiration for key: %s", key);
            throw new RedisOperationException("SETNX with expiration failed for key: " + key, e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            String value = valueCommands.get(key);
            LOG.debugf("GET operation: key=%s, found=%s", key, value != null);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute GET for key: %s", key);
            throw new RedisOperationException("GET failed for key: " + key, e);
        }
    }

    @Override
    public void set(String key, String value) {
        try {
            valueCommands.set(key, value);
            LOG.debugf("SET operation: key=%s", key);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SET for key: %s", key);
            throw new RedisOperationException("SET failed for key: " + key, e);
        }
    }

    @Override
    public void set(String key, String value, Duration expiration) {
        try {
            valueCommands.set(key, value);
            keyCommands.expire(key, expiration);
            LOG.debugf("SET with expiration operation: key=%s, expiration=%s", key, expiration);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SET with expiration for key: %s", key);
            throw new RedisOperationException("SET with expiration failed for key: " + key, e);
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            int result = keyCommands.del(key);
            boolean deleted = result > 0;
            LOG.debugf("DELETE operation: key=%s, deleted=%s", key, deleted);
            return deleted;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute DELETE for key: %s", key);
            throw new RedisOperationException("DELETE failed for key: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            boolean exists = keyCommands.exists(key);
            LOG.debugf("EXISTS operation: key=%s, exists=%s", key, exists);
            return exists;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute EXISTS for key: %s", key);
            throw new RedisOperationException("EXISTS failed for key: " + key, e);
        }
    }

    @Override
    public boolean expire(String key, Duration expiration) {
        try {
            Boolean result = keyCommands.expire(key, expiration);
            boolean expired = Boolean.TRUE.equals(result);
            LOG.debugf("EXPIRE operation: key=%s, expiration=%s, result=%s", key, expiration, expired);
            return expired;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute EXPIRE for key: %s", key);
            throw new RedisOperationException("EXPIRE failed for key: " + key, e);
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        try {
            java.util.List<String> keysList = keyCommands.keys(pattern);
            Set<String> result = new java.util.HashSet<>(keysList);
            LOG.debugf("KEYS operation: pattern=%s, count=%d", pattern, result.size());
            return result;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute KEYS for pattern: %s", pattern);
            throw new RedisOperationException("KEYS failed for pattern: " + pattern, e);
        }
    }

    @Override
    public boolean sadd(String key, String value) {
        try {
            int result = setCommands.sadd(key, value);
            boolean added = result > 0;
            LOG.debugf("SADD operation: key=%s, value=%s, added=%s", key, value, added);
            return added;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SADD for key: %s", key);
            throw new RedisOperationException("SADD failed for key: " + key, e);
        }
    }

    @Override
    public boolean sismember(String key, String value) {
        try {
            Boolean result = setCommands.sismember(key, value);
            boolean isMember = Boolean.TRUE.equals(result);
            LOG.debugf("SISMEMBER operation: key=%s, value=%s, isMember=%s", key, value, isMember);
            return isMember;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SISMEMBER for key: %s", key);
            throw new RedisOperationException("SISMEMBER failed for key: " + key, e);
        }
    }

    @Override
    public boolean srem(String key, String value) {
        try {
            int result = setCommands.srem(key, value);
            boolean removed = result > 0;
            LOG.debugf("SREM operation: key=%s, value=%s, removed=%s", key, value, removed);
            return removed;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SREM for key: %s", key);
            throw new RedisOperationException("SREM failed for key: " + key, e);
        }
    }

    @Override
    public long scard(String key) {
        try {
            Long result = setCommands.scard(key);
            long size = result != null ? result : 0;
            LOG.debugf("SCARD operation: key=%s, size=%d", key, size);
            return size;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute SCARD for key: %s", key);
            throw new RedisOperationException("SCARD failed for key: " + key, e);
        }
    }

    @Override
    public long incr(String key) {
        try {
            Long result = valueCommands.incr(key);
            long value = result != null ? result : 0;
            LOG.debugf("INCR operation: key=%s, newValue=%d", key, value);
            return value;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute INCR for key: %s", key);
            throw new RedisOperationException("INCR failed for key: " + key, e);
        }
    }

    @Override
    public long incrby(String key, long increment) {
        try {
            Long result = valueCommands.incrby(key, increment);
            long value = result != null ? result : 0;
            LOG.debugf("INCRBY operation: key=%s, increment=%d, newValue=%d", key, increment, value);
            return value;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute INCRBY for key: %s", key);
            throw new RedisOperationException("INCRBY failed for key: " + key, e);
        }
    }

    @Override
    public long getCounter(String key) {
        try {
            String value = valueCommands.get(key);
            long counter = value != null ? Long.parseLong(value) : 0;
            LOG.debugf("GET counter operation: key=%s, value=%d", key, counter);
            return counter;
        } catch (NumberFormatException e) {
            LOG.warnf("Invalid counter value for key %s, returning 0", key);
            return 0;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to get counter for key: %s", key);
            throw new RedisOperationException("GET counter failed for key: " + key, e);
        }
    }

    /**
     * Exception thrown when Redis operations fail.
     */
    public static class RedisOperationException extends RuntimeException {
        public RedisOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}