# Falsify Crawler System

A multi-module web crawling system built with Quarkus for extracting and analyzing predictions from articles. The system is designed for fact-checking and prediction tracking with AI-powered content analysis.

## Architecture Overview

The system follows a modular architecture with shared common functionality:

```
├── crawler-common/          # Shared services, models, and utilities
├── crawler-drucker/         # Drucker.net crawler implementation
├── crawler-caspit/          # Caspit.co.il crawler implementation
├── crawler-manager/         # Orchestration service (future)
└── pom.xml                  # Parent POM with dependency management
```

## Key Features

- **Shared Infrastructure**: Common services for content validation, retry logic, Redis operations, and configuration
- **Modular Design**: Easy to add new crawlers using the common module
- **Content Validation**: AI-powered content analysis and validation
- **Deduplication**: Redis-based URL deduplication per crawler
- **Error Handling**: Comprehensive retry mechanisms with circuit breaker pattern
- **Performance Monitoring**: Built-in metrics and structured logging
- **Configuration Management**: Centralized configuration with validation

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for PostgreSQL and Redis)
- Docker Compose (optional)

### Running the System

1. **Start Dependencies**:
   ```bash
   # PostgreSQL and Redis will be started automatically by Quarkus Dev Services
   # Or manually with Docker:
   docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:17
   docker run -d --name redis -p 6379:6379 redis:7
   ```

2. **Build the Project**:
   ```bash
   mvn clean compile
   ```

3. **Run Crawlers**:

   **Option A: Run Both Crawlers Simultaneously (Recommended for Development)**
   ```bash
   # Start both crawlers with automatic setup
   ./run-both-crawlers.sh
   ```
   
   **Option B: Run Individual Crawlers**
   ```bash
   # Drucker crawler (port 8081)
   mvn quarkus:dev -pl crawler-drucker
   
   # Caspit crawler (port 8080) - in separate terminal
   mvn quarkus:dev -pl crawler-caspit
   ```

4. **Access Points**:
   - **Caspit Crawler**: http://localhost:8080
     - Dev UI: http://localhost:8080/q/dev/
     - Health: http://localhost:8080/q/health
     - API: http://localhost:8080/caspit/
   - **Drucker Crawler**: http://localhost:8081
     - Dev UI: http://localhost:8081/q/dev/
     - Health: http://localhost:8081/q/health
     - API: http://localhost:8081/drucker/

## Module Documentation

- [Multi-Crawler Setup](MULTI_CRAWLER_SETUP.md) - **Running multiple crawlers simultaneously**
- [Common Module Guide](crawler-common/README.md) - Shared services and utilities
- [Drucker Crawler](crawler-drucker/README.md) - Drucker.net crawler
- [Caspit Crawler](crawler-caspit/README.md) - Caspit.co.il crawler
- [Developer Guide](docs/DEVELOPER_GUIDE.md) - How to extend the system
- [Configuration Guide](docs/CONFIGURATION.md) - Configuration options
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions

## Technology Stack

- **Framework**: Quarkus 3.19.4 with Java 21
- **Build Tool**: Maven multi-module project
- **Database**: PostgreSQL with Hibernate ORM Panache
- **Cache**: Redis for deduplication and caching
- **Web Scraping**: JSoup for HTML parsing
- **AI Integration**: LangChain4j with OpenAI for content analysis
- **Testing**: JUnit 5, Testcontainers, REST Assured

## Development

### Code Style and Conventions

- Use `@ApplicationScoped` for singleton services
- Constructor injection with `@Inject`
- Mark database operations with `@Transactional`
- Entity naming: `*Entity.java` (e.g., `ArticleEntity`)
- Java records for immutable DTOs
- Package structure: `ai.falsify.crawlers.*`

### Testing

```bash
# Test multi-crawler setup
./test-multi-crawler-setup.sh

# Run all tests
mvn test

# Run integration tests
mvn verify

# Run specific module tests
mvn test -pl crawler-common
```

### Adding a New Crawler

See the [Developer Guide](docs/DEVELOPER_GUIDE.md) for detailed instructions on creating new crawlers using the common module.

## Configuration

The system uses a hierarchical configuration approach:

1. **Common Configuration**: Shared settings in `crawler-common`
2. **Module-Specific**: Override or extend in individual crawlers
3. **Environment**: Profile-specific configurations (`%dev`, `%test`, `%prod`)

### Multi-Crawler Configuration

When running multiple crawlers simultaneously:

| Crawler | Port | Redis Prefix | Source ID |
|---------|------|--------------|-----------|
| crawler-caspit | 8080 | `crawler:caspit` | `caspit` |
| crawler-drucker | 8081 | `crawler:drucker` | `drucker` |

Key configuration areas:
- Network settings (timeouts, user agents)
- Content validation rules
- Retry and circuit breaker settings
- Redis connection and key patterns
- Performance and concurrency limits
- Port assignments for simultaneous operation

## Monitoring and Observability

- **Structured Logging**: JSON-formatted logs with context
- **Metrics Collection**: Processing times, success/failure rates
- **Health Checks**: Built-in health endpoints
- **Dev UI**: Quarkus development interface

## Contributing

1. Follow the existing code patterns and conventions
2. Add comprehensive tests for new functionality
3. Update documentation for any changes
4. Ensure all tests pass before submitting

## License

[Add your license information here]