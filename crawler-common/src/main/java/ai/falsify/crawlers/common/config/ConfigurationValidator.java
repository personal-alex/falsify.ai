package ai.falsify.crawlers.common.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validation service for crawler configuration.
 * Provides comprehensive validation of configuration values and relationships.
 */
@ApplicationScoped
public class ConfigurationValidator {

    private static final Logger LOG = Logger.getLogger(ConfigurationValidator.class);

    @Inject
    Validator validator;

    @Inject
    CrawlerConfiguration config;

    /**
     * Validates the entire configuration and logs any issues.
     * 
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateConfiguration() {
        LOG.info("Starting configuration validation...");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Bean validation
        Set<ConstraintViolation<CrawlerConfiguration>> violations = validator.validate(config);
        for (ConstraintViolation<CrawlerConfiguration> violation : violations) {
            String error = String.format("Configuration validation error: %s = %s (%s)", 
                    violation.getPropertyPath(), violation.getInvalidValue(), violation.getMessage());
            errors.add(error);
            LOG.error(error);
        }
        
        // Custom validation rules
        validateNetworkConfiguration(errors, warnings);
        validateContentConfiguration(errors, warnings);
        validatePerformanceConfiguration(errors, warnings);
        validateRetryConfiguration(errors, warnings);
        validateRedisConfiguration(errors, warnings);
        validateLoggingConfiguration(errors, warnings);
        
        // Log warnings
        for (String warning : warnings) {
            LOG.warn(warning);
        }
        
        boolean isValid = errors.isEmpty();
        
        if (isValid) {
            LOG.info("Configuration validation completed successfully");
            logConfigurationSummary();
        } else {
            LOG.errorf("Configuration validation failed with %d errors and %d warnings", 
                    errors.size(), warnings.size());
        }
        
        return new ValidationResult(isValid, errors, warnings);
    }

    private void validateNetworkConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.NetworkConfig network = config.network();
        
        // Validate timeouts
        if (network.connectionTimeout().compareTo(Duration.ofSeconds(1)) < 0) {
            errors.add("Connection timeout must be at least 1 second");
        }
        
        if (network.readTimeout().compareTo(Duration.ofSeconds(1)) < 0) {
            errors.add("Read timeout must be at least 1 second");
        }
        
        if (network.connectionTimeout().compareTo(Duration.ofMinutes(5)) > 0) {
            warnings.add("Connection timeout is very high (>5 minutes), consider reducing it");
        }
        
        if (network.readTimeout().compareTo(Duration.ofMinutes(10)) > 0) {
            warnings.add("Read timeout is very high (>10 minutes), consider reducing it");
        }
        
        // Validate user agent
        if (network.userAgent().length() < 10) {
            warnings.add("User agent is very short, consider using a more descriptive one");
        }
        
        LOG.debug("Network configuration validated successfully");
    }

    private void validateContentConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.ContentConfig content = config.content();
        
        // Validate content length constraints
        if (content.minContentLength() >= content.maxContentLength()) {
            errors.add("Minimum content length must be less than maximum content length");
        }
        
        if (content.minContentLength() < 10) {
            warnings.add("Minimum content length is very low (<10), may result in poor quality content");
        }
        
        if (content.maxContentLength() > 100000) {
            warnings.add("Maximum content length is very high (>100KB), may impact performance");
        }
        
        // Validate HTML ratio
        if (content.maxHtmlRatio() < 0.0 || content.maxHtmlRatio() > 1.0) {
            errors.add("Maximum HTML ratio must be between 0.0 and 1.0");
        }
        
        // Validate duplicate cache TTL
        if (content.duplicateCacheTtl().compareTo(Duration.ofHours(1)) < 0) {
            warnings.add("Duplicate cache TTL is very short (<1 hour), may result in duplicate processing");
        }
        
        LOG.debug("Content configuration validated successfully");
    }

    private void validatePerformanceConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.PerformanceConfig performance = config.performance();
        
        // Validate concurrent requests
        if (performance.maxConcurrentRequests() > 20) {
            warnings.add("High number of concurrent requests (>20) may overwhelm target servers");
        }
        
        // Validate request delay
        if (performance.requestDelay().compareTo(Duration.ofMillis(100)) < 0) {
            warnings.add("Very short request delay (<100ms) may overwhelm target servers");
        }
        
        // Validate batch size
        if (performance.batchSize() > 100) {
            warnings.add("Large batch size (>100) may impact memory usage");
        }
        
        // Validate memory usage
        if (performance.maxMemoryUsageMb() < 128) {
            warnings.add("Low memory limit (<128MB) may impact performance");
        }
        
        if (performance.maxMemoryUsageMb() > 2048) {
            warnings.add("High memory limit (>2GB) may not be necessary");
        }
        
        LOG.debug("Performance configuration validated successfully");
    }

    private void validateRetryConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.RetryConfig retry = config.retry();
        
        // Validate retry delays
        if (retry.initialDelay().compareTo(retry.maxDelay()) > 0) {
            errors.add("Initial retry delay must be less than or equal to maximum delay");
        }
        
        if (retry.backoffMultiplier() < 1.0) {
            errors.add("Backoff multiplier must be at least 1.0");
        }
        
        if (retry.backoffMultiplier() > 10.0) {
            warnings.add("Very high backoff multiplier (>10) may result in very long delays");
        }
        
        // Validate circuit breaker
        if (retry.enableCircuitBreaker() && retry.circuitBreakerFailureThreshold() < 2) {
            warnings.add("Very low circuit breaker threshold (<2) may cause frequent circuit opening");
        }
        
        if (retry.circuitBreakerTimeout().compareTo(Duration.ofMinutes(10)) > 0) {
            warnings.add("Very long circuit breaker timeout (>10 minutes) may delay recovery");
        }
        
        LOG.debug("Retry configuration validated successfully");
    }

    private void validateRedisConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.RedisConfig redis = config.redis();
        
        // Validate connection settings
        if (redis.maxConnections() < 5) {
            warnings.add("Low Redis connection pool size (<5) may impact performance");
        }
        
        if (redis.maxConnections() > 100) {
            warnings.add("High Redis connection pool size (>100) may be unnecessary");
        }
        
        if (redis.connectionTimeout().compareTo(Duration.ofSeconds(1)) < 0) {
            errors.add("Redis connection timeout must be at least 1 second");
        }
        
        // Validate TTL settings
        if (redis.defaultTtl().compareTo(Duration.ofMinutes(5)) < 0) {
            warnings.add("Very short default TTL (<5 minutes) may cause frequent cache misses");
        }
        
        // Validate key prefix
        if (redis.keyPrefix().isEmpty()) {
            errors.add("Redis key prefix cannot be empty");
        }
        
        if (redis.keyPrefix().contains(":")) {
            warnings.add("Redis key prefix contains colons, ensure this doesn't conflict with key structure");
        }
        
        LOG.debug("Redis configuration validated successfully");
    }

    private void validateLoggingConfiguration(List<String> errors, List<String> warnings) {
        CrawlerConfiguration.LoggingConfig logging = config.logging();
        
        // Validate log level
        String logLevel = logging.logLevel().toUpperCase();
        if (!List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR").contains(logLevel)) {
            errors.add("Invalid log level: " + logging.logLevel());
        }
        
        // Performance warnings
        if (logging.logRequestDetails() || logging.logResponseDetails()) {
            warnings.add("Detailed request/response logging is enabled, may impact performance");
        }
        
        LOG.debug("Logging configuration validated successfully");
    }

    private void logConfigurationSummary() {
        LOG.info("=== Configuration Summary ===");
        
        CrawlerConfiguration.NetworkConfig network = config.network();
        LOG.infof("Network: timeout=%s, user-agent=%s", 
                network.connectionTimeout(), network.userAgent());
        
        CrawlerConfiguration.ContentConfig content = config.content();
        LOG.infof("Content: min-length=%d, max-length=%d, validation=%s", 
                content.minContentLength(), content.maxContentLength(), content.enableContentValidation());
        
        CrawlerConfiguration.PerformanceConfig performance = config.performance();
        LOG.infof("Performance: concurrent=%d, delay=%s, metrics=%s", 
                performance.maxConcurrentRequests(), performance.requestDelay(), performance.enableMetrics());
        
        CrawlerConfiguration.RetryConfig retry = config.retry();
        LOG.infof("Retry: max-attempts=%d, circuit-breaker=%s", 
                retry.maxAttempts(), retry.enableCircuitBreaker());
        
        CrawlerConfiguration.RedisConfig redis = config.redis();
        LOG.infof("Redis: enabled=%s, prefix=%s, ttl=%s", 
                redis.enableRedis(), redis.keyPrefix(), redis.defaultTtl());
    }

    /**
     * Result of configuration validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getWarningCount() {
            return warnings.size();
        }

        @Override
        public String toString() {
            return String.format("ValidationResult[valid=%s, errors=%d, warnings=%d]", 
                    valid, errors.size(), warnings.size());
        }
    }
}