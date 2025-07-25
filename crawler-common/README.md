# Crawler Common Module

The `crawler-common` module provides shared services, data models, configuration framework, and utilities that can be used by all crawler implementations in the Falsify system.

## Overview

This module was created to eliminate code duplication between crawlers and provide a consistent foundation for building new crawlers. It includes:

- **Shared Services**: Content validation, retry logic, Redis operations
- **Data Models**: Common article models and processing results
- **Configuration Framework**: Centralized configuration with validation
- **Exception Hierarchy**: Consistent error handling across crawlers
- **Utilities**: Logging, metrics, and performance monitoring

## Architecture

```
crawler-common/
├── src/main/java/ai/falsify/crawlers/common/
│   ├── config/              # Configuration framework
│   ├── exception/           # Common exception hierarchy
│   ├── model/              # Shared data models
│   └── service/            # Shared services
│       └── redis/          # Redis-specific services
├── src/test/java/          # Unit and integration tests
└── src/main/resources/     # Configuration files
```

## Core Components

### Configuration Framework

The configuration system provides a hierarchical approach to managing settings:

```java
@Inject
CrawlerConfiguration config;

// Access configuration sections
config.network().timeout();           // Network settings
config.content().minContentLength();  // Content validation
config.retry().maxAttempts();         // Retry configuration
config.redis().keyPrefix();           // Redis settings
```

**Key Configuration Classes:**
- `CrawlerConfiguration` - Main configuration interface
- `NetworkConfiguration` - HTTP client settings
- `ContentConfiguration` - Content validation rules
- `RetryConfiguration` - Retry and circuit breaker settings
- `RedisConfiguration` - Redis connection and key patterns
- `PerformanceConfiguration` - Concurrency and performance limits

### Data Models

**Core Models:**
- `Article` - Immutable record representing an article
- `ArticleEntity` - JPA entity for database persistence
- `CrawlResult` - Result of a crawling operation with metrics
- `ArticleProcessingResult` - Result of processing a single article

**Example Usage:**
```java
Article article = new Article("Title", "https://example.com", "Content");
CrawlResult result = new CrawlResult.Builder()
    .totalArticlesFound(10)
    .articlesProcessed(8)
    .articlesSkipped(1)
    .articlesFailed(1)
    .build();
```

### Services

#### ContentValidator

Validates article content according to configured rules:

```java
@Inject
ContentValidator validator;

// Validate article content
validator.validateArticle(title, url, content);
```

**Validation Rules:**
- Minimum content length
- Required fields (title, URL, content)
- URL format validation
- Content quality checks

#### RetryService

Provides retry logic with exponential backoff and circuit breaker:

```java
@Inject
RetryService retryService;

// Execute with retry
String result = retryService.executeWithRetry(() -> {
    return someOperation();
}, "operation-name");

// Execute with specific exception types
retryService.executeWithRetry(operation, "name", NetworkException.class);
```

**Features:**
- Configurable retry attempts
- Exponential backoff
- Circuit breaker pattern
- Operation-specific retry policies

#### DeduplicationService

Redis-based URL deduplication with crawler isolation:

```java
@Inject
DeduplicationService deduplicationService;

// Check if URL is new for specific crawler
if (deduplicationService.isNewUrl("crawler-name", url)) {
    // Process new URL
}

// Mark URL as processed
deduplicationService.markUrlProcessed("crawler-name", url);

// Management operations
long count = deduplicationService.getProcessedUrlCount("crawler-name");
deduplicationService.clearProcessedUrls("crawler-name");
```

**Features:**
- Crawler-specific namespacing
- TTL support for URL expiration
- Bulk operations for management
- Performance optimized with Redis sets

#### RedisService

Low-level Redis operations abstraction:

```java
@Inject
RedisService redisService;

// Basic operations
redisService.set("key", "value");
Optional<String> value = redisService.get("key");
boolean exists = redisService.exists("key");

// Set operations
redisService.sadd("set-key", "member");
boolean isMember = redisService.sismember("set-key", "member");
long cardinality = redisService.scard("set-key");

// With expiration
redisService.set("key", "value", Duration.ofHours(1));
redisService.setnx("key", "value", Duration.ofMinutes(30));
```

### Exception Hierarchy

Consistent exception handling across all crawlers:

```
CrawlingException (base)
├── NetworkException          # Network-related errors
├── ContentValidationException # Content validation failures
└── PersistenceException     # Database operation errors
```

**Usage:**
```java
try {
    // Crawler operation
} catch (NetworkException e) {
    // Handle network issues
} catch (ContentValidationException e) {
    // Handle validation failures
} catch (CrawlingException e) {
    // Handle general crawling errors
}
```

## Integration Guide

### Adding Common Module to Your Crawler

1. **Add Dependency** in your crawler's `pom.xml`:
```xml
<dependency>
    <groupId>ai.falsify</groupId>
    <artifactId>crawler-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

2. **Update Imports** to use common classes:
```java
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
```

3. **Inject Services** in your crawler:
```java
@ApplicationScoped
public class MyCrawler {
    
    @Inject
    ContentValidator contentValidator;
    
    @Inject
    RetryService retryService;
    
    @Inject
    DeduplicationService deduplicationService;
    
    // Your crawler implementation
}
```

### Configuration Integration

1. **Extend Base Configuration** (optional):
```java
@ConfigProperties(prefix = "crawler.mycrawler")
public interface MyCrawlerConfiguration extends CrawlerConfiguration {
    // Add crawler-specific settings
    @ConfigProperty(name = "specific-setting", defaultValue = "default")
    String specificSetting();
}
```

2. **Use Configuration** in your crawler:
```java
@Inject
CrawlerConfiguration config;

// Access common settings
Duration timeout = config.network().timeout();
int minLength = config.content().minContentLength();
```

### Implementing a Crawler

Here's a basic crawler implementation using the common module:

```java
@ApplicationScoped
public class MyCrawler {
    
    private static final Logger LOG = Logger.getLogger(MyCrawler.class);
    
    @Inject
    ContentValidator contentValidator;
    
    @Inject
    RetryService retryService;
    
    @Inject
    DeduplicationService deduplicationService;
    
    @Inject
    CrawlerConfiguration config;
    
    public CrawlResult crawl() {
        Instant startTime = Instant.now();
        List<Article> articles = new ArrayList<>();
        int processed = 0, skipped = 0, failed = 0;
        
        try {
            // Get URLs to crawl
            List<String> urls = getUrlsToCrawl();
            
            for (String url : urls) {
                try {
                    // Check deduplication
                    if (!deduplicationService.isNewUrl("mycrawler", url)) {
                        skipped++;
                        continue;
                    }
                    
                    // Fetch content with retry
                    String content = retryService.executeWithRetry(() -> 
                        fetchContent(url), "fetch-" + url, NetworkException.class);
                    
                    // Parse article
                    Article article = parseArticle(url, content);
                    
                    // Validate content
                    contentValidator.validateArticle(
                        article.title(), article.url(), article.text());
                    
                    // Save to database with retry
                    retryService.executeWithRetry(() -> {
                        saveArticle(article);
                        return null;
                    }, "save-" + url, PersistenceException.class);
                    
                    articles.add(article);
                    processed++;
                    
                } catch (Exception e) {
                    LOG.errorf("Failed to process URL %s: %s", url, e.getMessage());
                    failed++;
                }
            }
            
            return new CrawlResult.Builder()
                .totalArticlesFound(urls.size())
                .articlesProcessed(processed)
                .articlesSkipped(skipped)
                .articlesFailed(failed)
                .articles(articles)
                .startTime(startTime)
                .endTime(Instant.now())
                .crawlerSource("mycrawler")
                .build();
                
        } catch (Exception e) {
            LOG.error("Crawl operation failed", e);
            throw new CrawlingException("Crawl failed", e);
        }
    }
    
    private List<String> getUrlsToCrawl() {
        // Implementation specific to your crawler
        return List.of();
    }
    
    private String fetchContent(String url) throws NetworkException {
        // HTTP client implementation
        return "";
    }
    
    private Article parseArticle(String url, String content) {
        // Parse HTML content to extract article data
        return new Article("Title", url, "Content");
    }
    
    @Transactional
    private void saveArticle(Article article) throws PersistenceException {
        // Save to database
        ArticleEntity entity = new ArticleEntity();
        entity.title = article.title();
        entity.url = article.url();
        entity.text = article.text();
        entity.persist();
    }
}
```

## Configuration Reference

### Network Configuration
```properties
# HTTP client settings
crawler.common.network.timeout=PT30S
crawler.common.network.user-agent=Mozilla/5.0 (compatible; CrawlerCommon/1.0)
crawler.common.network.max-redirects=5
```

### Content Configuration
```properties
# Content validation settings
crawler.common.content.min-content-length=100
crawler.common.content.max-content-length=50000
crawler.common.content.enable-content-validation=true
```

### Retry Configuration
```properties
# Retry and circuit breaker settings
crawler.common.retry.max-attempts=3
crawler.common.retry.initial-delay=PT1S
crawler.common.retry.max-delay=PT30S
crawler.common.retry.backoff-multiplier=2.0
crawler.common.retry.enable-circuit-breaker=true
crawler.common.retry.circuit-breaker-failure-threshold=5
crawler.common.retry.circuit-breaker-timeout=PT1M
```

### Redis Configuration
```properties
# Redis settings
crawler.common.redis.enabled=true
crawler.common.redis.key-prefix=crawler
crawler.common.redis.default-ttl=PT24H
```

### Performance Configuration
```properties
# Performance and concurrency settings
crawler.common.performance.max-concurrent-requests=5
crawler.common.performance.request-delay=PT1S
crawler.common.performance.enable-metrics=true
```

### Logging Configuration
```properties
# Logging settings
crawler.common.logging.enable-structured-logging=true
crawler.common.logging.log-level=INFO
```

## Testing

The common module includes comprehensive test coverage:

### Unit Tests
```bash
# Run unit tests
mvn test -pl crawler-common
```

### Integration Tests
```bash
# Run integration tests (requires Docker)
mvn verify -pl crawler-common
```

**Test Categories:**
- **Service Tests**: Individual service functionality
- **Integration Tests**: Cross-service interactions
- **Configuration Tests**: Configuration validation
- **Error Handling Tests**: Exception scenarios
- **Performance Tests**: Load and concurrency testing

### Test Utilities

The module provides test utilities for crawler implementations:

```java
// Test configuration
@TestProfile(CommonModuleTestProfile.class)
class MyCrawlerTest {
    // Test implementation
}
```

## Performance Considerations

### Redis Optimization
- Use connection pooling for high-throughput scenarios
- Consider Redis clustering for large-scale deployments
- Monitor Redis memory usage with TTL settings

### Database Performance
- Use batch operations for bulk inserts
- Implement proper indexing on frequently queried fields
- Consider read replicas for high-read scenarios

### Concurrency
- Configure appropriate thread pool sizes
- Use async operations where possible
- Monitor resource usage under load

## Troubleshooting

### Common Issues

**Configuration Validation Errors:**
```
Configuration validation failed: Network timeout must be positive
```
- Check configuration values are within valid ranges
- Ensure required properties are set

**Redis Connection Issues:**
```
Unable to connect to Redis server
```
- Verify Redis server is running
- Check connection settings and network connectivity
- Review Redis authentication if configured

**Content Validation Failures:**
```
Content validation failed: Content too short
```
- Review content validation rules
- Check if content extraction is working correctly
- Consider adjusting minimum content length

### Debug Logging

Enable debug logging for troubleshooting:

```properties
quarkus.log.category."ai.falsify.crawlers.common".level=DEBUG
```

### Health Checks

The module provides health check endpoints:
- `/q/health` - Overall health status
- `/q/health/ready` - Readiness check
- `/q/health/live` - Liveness check

## Migration Guide

### From Individual Crawler Services

If you have existing crawler implementations, follow these steps to migrate to the common module:

1. **Identify Shared Code**: Look for duplicate services, models, and utilities
2. **Update Dependencies**: Add crawler-common dependency
3. **Replace Imports**: Update imports to use common module classes
4. **Update Configuration**: Migrate to common configuration framework
5. **Test Integration**: Run comprehensive tests to ensure functionality
6. **Remove Duplicates**: Delete duplicate code from individual crawlers

### Breaking Changes

When upgrading the common module, be aware of potential breaking changes:
- Configuration property name changes
- Service interface modifications
- Exception hierarchy updates

Always review the changelog and test thoroughly before upgrading.

## Contributing

When contributing to the common module:

1. **Maintain Backward Compatibility**: Avoid breaking existing crawler implementations
2. **Add Comprehensive Tests**: Include unit and integration tests
3. **Update Documentation**: Keep this README and other docs current
4. **Follow Conventions**: Use established patterns and naming conventions
5. **Performance Impact**: Consider the impact on all crawlers

## Support

For questions or issues with the common module:
1. Check this documentation and troubleshooting guide
2. Review existing tests for usage examples
3. Check the project's issue tracker
4. Contact the development team