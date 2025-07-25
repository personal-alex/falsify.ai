# Caspit Crawler

A web crawler for caspit.co.il that extracts articles and predictions using the Falsify Crawler Common Module.

## Overview

The Caspit Crawler is built on the [Falsify Crawler System](../README.md) and uses the [Common Module](../crawler-common/README.md) for shared functionality including:

- Content validation and quality control
- Redis-based URL deduplication
- Retry logic with circuit breaker
- Structured logging and metrics
- Configuration management

## Features

- **Article Extraction**: Scrapes articles from caspit.co.il
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
mvn quarkus:dev -pl crawler-caspit
```

The application will start on http://localhost:8081 with:
- Dev UI: http://localhost:8081/q/dev/
- Health checks: http://localhost:8081/q/health
- Metrics: http://localhost:8081/q/metrics

### Running the Crawler

```bash
# Trigger a crawl operation
curl -X POST http://localhost:8081/caspit/crawl

# Check crawler status
curl http://localhost:8081/caspit/health
```

## Configuration

The Caspit Crawler uses the common configuration framework with crawler-specific overrides.

### Key Configuration Properties

```properties
# Caspit-specific settings
crawler.caspit.base-url=https://caspit.co.il
crawler.caspit.max-articles-per-page=20
crawler.caspit.include-categories=news,opinion

# Override common settings for Caspit
crawler.common.performance.request-delay=PT2S
crawler.common.content.max-content-length=100000
crawler.common.retry.max-attempts=3
```

### Environment Profiles

#### Development (`%dev`)
```properties
%dev.crawler.common.content.min-content-length=50
%dev.crawler.caspit.max-articles-per-page=5
%dev.quarkus.log.category."ai.falsify.crawlers.caspit".level=DEBUG
```

#### Production (`%prod`)
```properties
%prod.crawler.common.content.enable-content-validation=true
%prod.crawler.caspit.max-articles-per-page=50
%prod.quarkus.log.console.json=true
```

## Architecture

The Caspit Crawler follows the common module architecture:

```
CaspitCrawler
├── ContentValidator (from common)
├── RetryService (from common)
├── DeduplicationService (from common)
├── CaspitCrawlerConfig (crawler-specific)
└── CaspitPageNavigator (crawler-specific)
```

### Main Components

- **CaspitCrawler**: Main crawler implementation
- **CaspitCrawlerResource**: REST API endpoints
- **CaspitCrawlerConfig**: Configuration management
- **CaspitPageNavigator**: Page navigation logic

## API Endpoints

### Crawl Operations

```bash
# Start crawl operation
POST /caspit/crawl
Response: CrawlResult with processing statistics

# Get crawler health
GET /caspit/health
Response: Health status and basic info
```

### Example Response

```json
{
  "totalArticlesFound": 30,
  "articlesProcessed": 28,
  "articlesSkipped": 1,
  "articlesFailed": 1,
  "processingTimeMs": 52000,
  "startTime": "2024-01-15T10:30:00Z",
  "endTime": "2024-01-15T10:30:52Z",
  "crawlerSource": "caspit",
  "successful": true,
  "errors": ["Failed to process https://example.com/article: Content validation failed"]
}
```

## Development

### Project Structure

```
crawler-caspit/
├── src/main/java/ai/falsify/crawlers/
│   ├── CaspitCrawler.java            # Main crawler logic
│   ├── CaspitCrawlerResource.java    # REST endpoints
│   ├── CaspitCrawlerConfig.java      # Configuration
│   ├── CaspitPageNavigator.java      # Page navigation
│   ├── PredictionExtractor.java      # Prediction extraction
│   └── model/
│       └── Prediction.java           # Prediction data model
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
mvn compile -pl crawler-caspit

# Run tests
mvn test -pl crawler-caspit

# Package
mvn package -pl crawler-caspit
```

### Testing

```bash
# Unit tests
mvn test -pl crawler-caspit -Dtest="*Test"

# Integration tests
mvn test -pl crawler-caspit -Dtest="*IT"

# Specific test
mvn test -pl crawler-caspit -Dtest="CaspitCrawlerTest"
```

## Monitoring and Observability

### Health Checks

```bash
# Overall health
curl http://localhost:8081/q/health

# Readiness (dependencies available)
curl http://localhost:8081/q/health/ready

# Liveness (application responsive)
curl http://localhost:8081/q/health/live
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8081/q/metrics

# Application-specific metrics
curl http://localhost:8081/q/metrics/application
```

### Logging

The crawler uses structured logging with contextual information:

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "ai.falsify.crawlers.CaspitCrawler",
  "message": "CRAWL_COMPLETE: Processed 28 articles in 52000ms",
  "crawler": "caspit",
  "operation": "crawl",
  "articles_processed": 28,
  "processing_time_ms": 52000
}
```

## Deployment

### Docker

```bash
# Build application
mvn package -pl crawler-caspit

# Build Docker image
docker build -t caspit-crawler crawler-caspit/

# Run container
docker run -p 8081:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/crawler \
  -e QUARKUS_REDIS_HOSTS=redis://host.docker.internal:6379 \
  caspit-crawler
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: caspit-crawler
spec:
  replicas: 1
  selector:
    matchLabels:
      app: caspit-crawler
  template:
    metadata:
      labels:
        app: caspit-crawler
    spec:
      containers:
      - name: caspit-crawler
        image: caspit-crawler:latest
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
quarkus.log.category."ai.falsify.crawlers.caspit".level=DEBUG
quarkus.log.category."ai.falsify.crawlers.common".level=DEBUG
```

### Performance Tuning

```properties
# Adjust concurrency
crawler.common.performance.max-concurrent-requests=5
crawler.common.performance.request-delay=PT2S

# Tune retry behavior
crawler.common.retry.max-attempts=3
crawler.common.retry.initial-delay=PT1S
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