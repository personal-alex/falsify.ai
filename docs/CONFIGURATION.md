# Configuration Guide

This guide covers all configuration options available in the Falsify Crawler System, including common module settings and crawler-specific configurations.

## Table of Contents

1. [Configuration Overview](#configuration-overview)
2. [Common Module Configuration](#common-module-configuration)
3. [Crawler-Specific Configuration](#crawler-specific-configuration)
4. [Environment Profiles](#environment-profiles)
5. [Configuration Validation](#configuration-validation)
6. [Advanced Configuration](#advanced-configuration)

## Configuration Overview

The Falsify Crawler System uses a hierarchical configuration approach:

1. **Common Configuration**: Shared settings defined in the `crawler-common` module
2. **Module-Specific Configuration**: Crawler-specific overrides and extensions
3. **Environment Profiles**: Profile-specific configurations (`%dev`, `%test`, `%prod`)
4. **System Properties**: Runtime overrides via `-D` flags or environment variables

### Configuration Sources (in order of precedence)

1. System properties (`-Dproperty=value`)
2. Environment variables (`PROPERTY=value`)
3. Profile-specific properties (`application-{profile}.properties`)
4. Application properties (`application.properties`)
5. Default values in configuration classes

## Common Module Configuration

The common module provides shared configuration that all crawlers inherit. These settings are prefixed with `crawler.common.`.

### Network Configuration

Controls HTTP client behavior for web scraping:

```properties
# HTTP request timeout (ISO-8601 duration format)
crawler.common.network.timeout=PT30S

# User agent string for HTTP requests
crawler.common.network.user-agent=Mozilla/5.0 (compatible; CrawlerCommon/1.0)

# Maximum number of HTTP redirects to follow
crawler.common.network.max-redirects=5

# Connection pool size for HTTP client
crawler.common.network.connection-pool-size=10

# Keep-alive timeout for HTTP connections
crawler.common.network.keep-alive-timeout=PT5M
```

**Default Values:**
- `timeout`: 30 seconds
- `user-agent`: "Mozilla/5.0 (compatible; CrawlerCommon/1.0)"
- `max-redirects`: 5
- `connection-pool-size`: 10
- `keep-alive-timeout`: 5 minutes

### Content Configuration

Controls content validation and processing:

```properties
# Minimum content length for articles (characters)
crawler.common.content.min-content-length=100

# Maximum content length for articles (characters)
crawler.common.content.max-content-length=50000

# Enable/disable content validation
crawler.common.content.enable-content-validation=true

# Required fields validation (comma-separated)
crawler.common.content.required-fields=title,url,text

# Content quality threshold (0.0 to 1.0)
crawler.common.content.quality-threshold=0.7
```

**Default Values:**
- `min-content-length`: 100
- `max-content-length`: 50,000
- `enable-content-validation`: true
- `required-fields`: "title,url,text"
- `quality-threshold`: 0.7

### Retry Configuration

Controls retry logic and circuit breaker behavior:

```properties
# Maximum number of retry attempts
crawler.common.retry.max-attempts=3

# Initial delay between retries (ISO-8601 duration)
crawler.common.retry.initial-delay=PT1S

# Maximum delay between retries (ISO-8601 duration)
crawler.common.retry.max-delay=PT30S

# Backoff multiplier for exponential backoff
crawler.common.retry.backoff-multiplier=2.0

# Enable/disable circuit breaker
crawler.common.retry.enable-circuit-breaker=true

# Number of failures to trigger circuit breaker
crawler.common.retry.circuit-breaker-failure-threshold=5

# Circuit breaker timeout before attempting reset
crawler.common.retry.circuit-breaker-timeout=PT1M

# Circuit breaker reset timeout
crawler.common.retry.circuit-breaker-reset-timeout=PT5M
```

**Default Values:**
- `max-attempts`: 3
- `initial-delay`: 1 second
- `max-delay`: 30 seconds
- `backoff-multiplier`: 2.0
- `enable-circuit-breaker`: true
- `circuit-breaker-failure-threshold`: 5
- `circuit-breaker-timeout`: 1 minute
- `circuit-breaker-reset-timeout`: 5 minutes

### Redis Configuration

Controls Redis connection and key management:

```properties
# Enable/disable Redis functionality
crawler.common.redis.enabled=true

# Redis key prefix for all operations
crawler.common.redis.key-prefix=crawler

# Default TTL for Redis keys (ISO-8601 duration)
crawler.common.redis.default-ttl=PT24H

# Redis connection timeout
crawler.common.redis.connection-timeout=PT5S

# Redis command timeout
crawler.common.redis.command-timeout=PT3S

# Redis connection pool size
crawler.common.redis.pool-size=10
```

**Default Values:**
- `enabled`: true
- `key-prefix`: "crawler"
- `default-ttl`: 24 hours
- `connection-timeout`: 5 seconds
- `command-timeout`: 3 seconds
- `pool-size`: 10

### Performance Configuration

Controls concurrency and performance settings:

```properties
# Maximum concurrent HTTP requests
crawler.common.performance.max-concurrent-requests=5

# Delay between requests to avoid overwhelming servers
crawler.common.performance.request-delay=PT1S

# Enable/disable metrics collection
crawler.common.performance.enable-metrics=true

# Thread pool size for async operations
crawler.common.performance.thread-pool-size=10

# Queue size for pending operations
crawler.common.performance.queue-size=100
```

**Default Values:**
- `max-concurrent-requests`: 5
- `request-delay`: 1 second
- `enable-metrics`: true
- `thread-pool-size`: 10
- `queue-size`: 100

### Logging Configuration

Controls logging behavior and formatting:

```properties
# Enable/disable structured logging (JSON format)
crawler.common.logging.enable-structured-logging=true

# Default log level for crawler operations
crawler.common.logging.log-level=INFO

# Enable/disable performance logging
crawler.common.logging.enable-performance-logging=true

# Log format pattern (when structured logging is disabled)
crawler.common.logging.pattern=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
```

**Default Values:**
- `enable-structured-logging`: true
- `log-level`: INFO
- `enable-performance-logging`: true
- `pattern`: Standard Quarkus pattern

## Crawler-Specific Configuration

Individual crawlers can define their own configuration properties and override common settings.

### Drucker Crawler Configuration

```properties
# Drucker-specific settings
crawler.drucker.base-url=https://drucker10.net
crawler.drucker.max-pages=50
crawler.drucker.category-urls=politics,economy,technology

# Override common settings for Drucker
crawler.common.content.min-content-length=200
crawler.common.retry.max-attempts=5
```

### Caspit Crawler Configuration

```properties
# Caspit-specific settings
crawler.caspit.base-url=https://caspit.co.il
crawler.caspit.max-articles-per-page=20
crawler.caspit.include-categories=news,opinion

# Override common settings for Caspit
crawler.common.performance.request-delay=PT2S
crawler.common.content.max-content-length=100000
```

### Creating Custom Configuration

For new crawlers, you can create custom configuration interfaces:

```java
@ConfigProperties(prefix = "crawler.mycrawler")
public interface MyCrawlerConfiguration extends CrawlerConfiguration {
    
    @ConfigProperty(name = "base-url")
    String baseUrl();
    
    @ConfigProperty(name = "max-pages", defaultValue = "100")
    int maxPages();
    
    @ConfigProperty(name = "categories")
    Optional<List<String>> categories();
    
    @ConfigProperty(name = "api-key")
    Optional<String> apiKey();
}
```

Then inject and use in your crawler:

```java
@ApplicationScoped
public class MyCrawler {
    
    @Inject
    MyCrawlerConfiguration config;
    
    public void crawl() {
        String baseUrl = config.baseUrl();
        int maxPages = config.maxPages();
        // Use configuration...
    }
}
```

## Environment Profiles

The system supports different configuration profiles for different environments.

### Development Profile (`%dev`)

```properties
# Development-specific settings
%dev.quarkus.log.level=DEBUG
%dev.quarkus.log.category."ai.falsify.crawlers".level=DEBUG

# Use embedded databases for development
%dev.quarkus.datasource.db-kind=h2
%dev.quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb

# Relaxed content validation for development
%dev.crawler.common.content.min-content-length=10
%dev.crawler.common.retry.max-attempts=1
```

### Test Profile (`%test`)

```properties
# Test-specific settings
%test.quarkus.log.level=WARN
%test.quarkus.hibernate-orm.database.generation=drop-and-create

# Fast settings for tests
%test.crawler.common.network.timeout=PT5S
%test.crawler.common.retry.max-attempts=2
%test.crawler.common.retry.initial-delay=PT100MS

# Disable external services in tests
%test.crawler.common.redis.enabled=false
```

### Production Profile (`%prod`)

```properties
# Production-specific settings
%prod.quarkus.log.level=INFO
%prod.quarkus.log.console.json=true

# Production database settings
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.quarkus.datasource.username=${DATABASE_USERNAME}
%prod.quarkus.datasource.password=${DATABASE_PASSWORD}

# Production Redis settings
%prod.quarkus.redis.hosts=${REDIS_URL}

# Strict validation for production
%prod.crawler.common.content.enable-content-validation=true
%prod.crawler.common.retry.enable-circuit-breaker=true
```

### Activating Profiles

```bash
# Development (default)
mvn quarkus:dev

# Test
mvn test

# Production
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar

# Multiple profiles
java -Dquarkus.profile=prod,monitoring -jar target/quarkus-app/quarkus-run.jar
```

## Configuration Validation

The system includes comprehensive configuration validation that runs at startup.

### Validation Rules

1. **Network Configuration**:
   - Timeout must be positive
   - User agent cannot be empty
   - Max redirects must be >= 0

2. **Content Configuration**:
   - Min content length must be positive
   - Max content length must be greater than min
   - Quality threshold must be between 0.0 and 1.0

3. **Retry Configuration**:
   - Max attempts must be positive
   - Delays must be positive
   - Backoff multiplier must be >= 1.0

4. **Performance Configuration**:
   - Concurrent requests must be positive
   - Thread pool size must be positive
   - Queue size must be positive

### Custom Validation

You can add custom validation to your crawler configuration:

```java
@ConfigProperties(prefix = "crawler.mycrawler")
public interface MyCrawlerConfiguration extends CrawlerConfiguration {
    
    @ConfigProperty(name = "base-url")
    @Pattern(regexp = "https?://.*", message = "Base URL must be a valid HTTP/HTTPS URL")
    String baseUrl();
    
    @ConfigProperty(name = "max-pages", defaultValue = "100")
    @Min(value = 1, message = "Max pages must be at least 1")
    @Max(value = 1000, message = "Max pages cannot exceed 1000")
    int maxPages();
}
```

### Validation Startup Bean

Create a startup bean to validate configuration:

```java
@ApplicationScoped
public class ConfigurationValidator {
    
    @Inject
    MyCrawlerConfiguration config;
    
    @EventObserver
    void onStart(@Observes StartupEvent event) {
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        // Custom validation logic
        if (config.maxPages() > 500) {
            LOG.warn("Max pages is set to a high value: " + config.maxPages());
        }
    }
}
```

## Advanced Configuration

### Environment Variable Mapping

Quarkus automatically maps environment variables to configuration properties:

```bash
# Property: crawler.common.network.timeout
# Environment variable: CRAWLER_COMMON_NETWORK_TIMEOUT
export CRAWLER_COMMON_NETWORK_TIMEOUT=PT45S

# Property: crawler.drucker.base-url  
# Environment variable: CRAWLER_DRUCKER_BASE_URL
export CRAWLER_DRUCKER_BASE_URL=https://custom-drucker-url.com
```

### Configuration Profiles with Environment Variables

```bash
# Set profile via environment variable
export QUARKUS_PROFILE=prod

# Override specific properties
export CRAWLER_COMMON_RETRY_MAX_ATTEMPTS=5
export CRAWLER_COMMON_REDIS_KEY_PREFIX=prod-crawler
```

### Dynamic Configuration

For configuration that needs to change at runtime, use CDI events:

```java
@ApplicationScoped
public class DynamicConfigurationService {
    
    @Inject
    Event<ConfigurationChangeEvent> configChangeEvent;
    
    public void updateMaxConcurrentRequests(int newValue) {
        // Update configuration
        configChangeEvent.fire(new ConfigurationChangeEvent("max-concurrent-requests", newValue));
    }
}

@ApplicationScoped
public class ConfigurationChangeListener {
    
    void onConfigurationChange(@Observes ConfigurationChangeEvent event) {
        LOG.infof("Configuration changed: %s = %s", event.property(), event.value());
        // Handle configuration change
    }
}
```

### Configuration Encryption

For sensitive configuration values, use Quarkus configuration encryption:

```properties
# Encrypted password
quarkus.datasource.password=${aes-gcm-nopadding::encrypted-value}

# Encrypted API key
crawler.mycrawler.api-key=${aes-gcm-nopadding::encrypted-api-key}
```

### Configuration from External Sources

#### Consul Configuration

```properties
# Enable Consul configuration source
quarkus.config.source.consul.enabled=true
quarkus.config.source.consul.agent-host=localhost
quarkus.config.source.consul.agent-port=8500
quarkus.config.source.consul.key-prefix=config/crawler
```

#### Kubernetes ConfigMaps

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: crawler-config
data:
  application.properties: |
    crawler.common.network.timeout=PT45S
    crawler.common.retry.max-attempts=5
```

Mount as volume in deployment:

```yaml
spec:
  containers:
  - name: crawler
    volumeMounts:
    - name: config
      mountPath: /deployments/config
  volumes:
  - name: config
    configMap:
      name: crawler-config
```

### Configuration Monitoring

Monitor configuration changes and values:

```java
@ApplicationScoped
public class ConfigurationMonitor {
    
    @Inject
    CrawlerConfiguration config;
    
    @Scheduled(every = "1m")
    void logConfiguration() {
        LOG.infof("Current configuration - Timeout: %s, Max attempts: %d", 
                 config.network().timeout(), 
                 config.retry().maxAttempts());
    }
}
```

### Configuration Best Practices

1. **Use Type-Safe Configuration**: Prefer `@ConfigProperties` interfaces over `@ConfigProperty` annotations
2. **Provide Sensible Defaults**: Always provide default values for optional properties
3. **Validate Early**: Validate configuration at startup, not during runtime
4. **Document Properties**: Include comments explaining what each property does
5. **Use Profiles**: Separate configuration for different environments
6. **Secure Sensitive Data**: Use encryption or external secret management for sensitive values
7. **Monitor Configuration**: Log important configuration values at startup
8. **Test Configuration**: Include configuration testing in your test suite

### Configuration Testing

Test your configuration setup:

```java
@QuarkusTest
class ConfigurationTest {
    
    @Inject
    CrawlerConfiguration config;
    
    @Test
    void testDefaultConfiguration() {
        assertEquals(Duration.ofSeconds(30), config.network().timeout());
        assertEquals(3, config.retry().maxAttempts());
        assertTrue(config.content().enableContentValidation());
    }
    
    @Test
    @TestProfile(CustomTestProfile.class)
    void testCustomConfiguration() {
        assertEquals(Duration.ofSeconds(10), config.network().timeout());
        assertEquals(1, config.retry().maxAttempts());
    }
    
    public static class CustomTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "crawler.common.network.timeout", "PT10S",
                "crawler.common.retry.max-attempts", "1"
            );
        }
    }
}
```

## Troubleshooting Configuration

### Common Configuration Issues

1. **Property Not Found**: Check property name spelling and prefix
2. **Type Conversion Errors**: Ensure property values match expected types
3. **Validation Failures**: Check that values meet validation constraints
4. **Profile Not Active**: Verify the correct profile is being used
5. **Environment Variable Override**: Check if environment variables are overriding expected values

### Debug Configuration

Enable configuration debugging:

```properties
quarkus.log.category."io.quarkus.config".level=DEBUG
```

Or use the configuration dump endpoint:

```bash
curl http://localhost:8080/q/info/config
```

### Configuration Health Check

Create a health check for configuration:

```java
@ApplicationScoped
public class ConfigurationHealthCheck implements HealthCheck {
    
    @Inject
    CrawlerConfiguration config;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Validate critical configuration
            if (config.network().timeout().isNegative()) {
                return HealthCheckResponse.down("Invalid network timeout");
            }
            
            return HealthCheckResponse.up("Configuration valid");
            
        } catch (Exception e) {
            return HealthCheckResponse.down("Configuration error: " + e.getMessage());
        }
    }
}
```

For more troubleshooting help, see the [Troubleshooting Guide](TROUBLESHOOTING.md).