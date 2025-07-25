# Drucker Crawler

A web crawler for drucker10.net that extracts articles and predictions using the Falsify Crawler Common Module.

## Overview

The Drucker Crawler is built on the [Falsify Crawler System](../README.md) and uses the [Common Module](../crawler-common/README.md) for shared functionality including:

- Content validation and quality control
- Redis-based URL deduplication
- Retry logic with circuit breaker
- Structured logging and metrics
- Configuration management

## Features

- **Article Extraction**: Scrapes articles from drucker10.net
- **Content Validation**: Validates article content quality and completeness
- **Deduplication**: Prevents processing duplicate URLs using Redis
- **Error Handling**: Comprehensive retry logic for network and processing failures
- **Performance Monitoring**: Built-in metrics and structured logging
- **Configuration**: Flexible configuration with environment profiles

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for PostgreSQL and Redis)

### Running in Development Mode

```bash
# Start from project root
mvn quarkus:dev -pl crawler-drucker
```

The application will start on http://localhost:8080 with:
- Dev UI: http://localhost:8080/q/dev/
- Health checks: http://localhost:8080/q/health
- Metrics: http://localhost:8080/q/metrics

### Running the Crawler

```bash
# Trigger a crawl operation
curl -X POST http://localhost:8080/drucker/crawl

# Check crawler status
curl http://localhost:8080/drucker/health
```

## Configuration

The Drucker Crawler uses the common configuration framework with crawler-specific overrides.

### Key Configuration Properties

```properties
# Drucker-specific settings
crawler.drucker.base-url=https://drucker10.net
crawler.drucker.max-pages=50
crawler.drucker.category-urls=politics,economy,technology

# Override common settings for Drucker
crawler.common.content.min-content-length=200
crawler.common.retry.max-attempts=5
crawler.common.performance.request-delay=PT2S
```

### Environment Profiles

#### Development (`%dev`)
```properties
%dev.crawler.common.content.min-content-length=50
%dev.crawler.drucker.max-pages=10
%dev.quarkus.log.category."ai.falsify.crawlers.drucker".level=DEBUG
```

#### Production (`%prod`)
```properties
%prod.crawler.common.content.enable-content-validation=true
%prod.crawler.drucker.max-pages=100
%prod.quarkus.log.console.json=true
```

## Architecture

The Drucker Crawler follows the common module architecture:

```
DruckerCrawler
├── ContentValidator (from common)
├── RetryService (from common)
├── DeduplicationService (from common)
├── CrawlingMetrics (crawler-specific)
├── LoggingContext (crawler-specific)
└── StructuredLogFormatter (crawler-specific)
```

### Main Components

- **DruckerCrawler**: Main crawler implementation
- **DruckerCrawlerResource**: REST API endpoints
- **CrawlingMetrics**: Performance monitoring
- **LoggingContext**: Structured logging utilities

## API Endpoints

### Crawl Operations

```bash
# Start crawl operation
POST /drucker/crawl
Response: CrawlResult with processing statistics

# Get crawler health
GET /drucker/health
Response: Health status and basic info
```

### Example Response

```json
{
  "totalArticlesFound": 25,
  "articlesProcessed": 23,
  "articlesSkipped": 1,
  "articlesFailed": 1,
  "processingTimeMs": 45000,
  "startTime": "2024-01-15T10:30:00Z",
  "endTime": "2024-01-15T10:30:45Z",
  "crawlerSource": "drucker",
  "successful": true,
  "errors": ["Failed to process https://example.com/article: Network timeout"]
}
```

## Development

### Project Structure

```
crawler-drucker/
├── src/main/java/ai/falsify/crawlers/
│   ├── DruckerCrawler.java           # Main crawler logic
│   ├── DruckerCrawlerResource.java   # REST endpoints
│   └── service/                      # Crawler-specific services
│       ├── CrawlingMetrics.java
│       ├── LoggingContext.java
│       └── StructuredLogFormatter.java
├── src/test/java/                    # Tests
├── src/main/resources/
│   └── application.properties        # Configuration
└── pom.xml                          # Dependencies
```

### Dependencies

The crawler depends on:
- **crawler-common**: Shared services and utilities
- **Quarkus**: Web framework and dependency injection
- **JSoup**: HTML parsing
- **PostgreSQL**: Database persistence
- **Redis**: Deduplication and caching

### Building

```bash
# Compile
mvn compile -pl crawler-drucker

# Run tests
mvn test -pl crawler-drucker

# Package
mvn package -pl crawler-drucker
```

### Testing

```bash
# Unit tests
mvn test -pl crawler-drucker -Dtest="*Test"

# Integration tests
mvn test -pl crawler-drucker -Dtest="*IT"

# Specific test
mvn test -pl crawler-drucker -Dtest="DruckerCrawlerTest"
```

## Monitoring and Observability

### Health Checks

```bash
# Overall health
curl http://localhost:8080/q/health

# Readiness (dependencies available)
curl http://localhost:8080/q/health/ready

# Liveness (application responsive)
curl http://localhost:8080/q/health/live
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/q/metrics

# Application-specific metrics
curl http://localhost:8080/q/metrics/application
```

### Logging

The crawler uses structured logging with contextual information:

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "ai.falsify.crawlers.DruckerCrawler",
  "message": "CRAWL_COMPLETE: Processed 23 articles in 45000ms",
  "crawler": "drucker",
  "operation": "crawl",
  "articles_processed": 23,
  "processing_time_ms": 45000
}
```

## Deployment

### Docker

```bash
# Build application
mvn package -pl crawler-drucker

# Build Docker image
docker build -t drucker-crawler crawler-drucker/

# Run container
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/crawler \
  -e QUARKUS_REDIS_HOSTS=redis://host.docker.internal:6379 \
  drucker-crawler
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: drucker-crawler
spec:
  replicas: 1
  selector:
    matchLabels:
      app: drucker-crawler
  template:
    metadata:
      labels:
        app: drucker-crawler
    spec:
      containers:
      - name: drucker-crawler
        image: drucker-crawler:latest
        ports:
        - containerPort: 8080
        env:
        - name: QUARKUS_DATASOURCE_JDBC_URL
          value: "jdbc:postgresql://postgres-service:5432/crawler"
        - name: QUARKUS_REDIS_HOSTS
          value: "redis://redis-service:6379"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

## Troubleshooting

### Common Issues

1. **Network Timeouts**: Increase `crawler.common.network.timeout`
2. **Content Validation Failures**: Check `crawler.common.content.min-content-length`
3. **Redis Connection Issues**: Verify Redis is running and accessible
4. **Database Connection Issues**: Check PostgreSQL configuration

### Debug Logging

```properties
# Enable debug logging
quarkus.log.category."ai.falsify.crawlers.drucker".level=DEBUG
quarkus.log.category."ai.falsify.crawlers.common".level=DEBUG
```

### Performance Tuning

```properties
# Adjust concurrency
crawler.common.performance.max-concurrent-requests=10
crawler.common.performance.request-delay=PT1S

# Tune retry behavior
crawler.common.retry.max-attempts=5
crawler.common.retry.initial-delay=PT2S
```

For more detailed troubleshooting, see the [Troubleshooting Guide](../docs/TROUBLESHOOTING.md).

## Contributing

1. Follow the [Developer Guide](../docs/DEVELOPER_GUIDE.md)
2. Use the common module for shared functionality
3. Add comprehensive tests for new features
4. Update documentation for any changes
5. Ensure all tests pass before submitting

## Related Documentation

- [Main Project README](../README.md)
- [Common Module Guide](../crawler-common/README.md)
- [Developer Guide](../docs/DEVELOPER_GUIDE.md)
- [Configuration Guide](../docs/CONFIGURATION.md)
- [Troubleshooting Guide](../docs/TROUBLESHOOTING.md)