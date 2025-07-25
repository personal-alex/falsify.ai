# Developer Guide

This guide provides detailed instructions for developers working with the Falsify Crawler System, including how to extend the system with new crawlers using the common module.

## Table of Contents

1. [Development Environment Setup](#development-environment-setup)
2. [Project Structure](#project-structure)
3. [Creating a New Crawler](#creating-a-new-crawler)
4. [Common Module Integration](#common-module-integration)
5. [Testing Guidelines](#testing-guidelines)
6. [Code Conventions](#code-conventions)
7. [Deployment](#deployment)

## Development Environment Setup

### Prerequisites

- **Java 21+**: Required for Quarkus 3.19.4
- **Maven 3.8+**: Build tool
- **Docker**: For PostgreSQL and Redis (or use Quarkus Dev Services)
- **IDE**: IntelliJ IDEA or VS Code with Java extensions

### Initial Setup

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd falsify-crawler
   ```

2. **Build the Project**:
   ```bash
   mvn clean compile
   ```

3. **Start Development Services**:
   ```bash
   # Quarkus Dev Services will automatically start PostgreSQL and Redis
   # Or manually:
   docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:17
   docker run -d --name redis -p 6379:6379 redis:7
   ```

4. **Run Tests**:
   ```bash
   mvn test
   ```

### IDE Configuration

#### IntelliJ IDEA

1. Import as Maven project
2. Set Project SDK to Java 21
3. Enable annotation processing
4. Install Quarkus plugin (optional but recommended)

#### VS Code

1. Install Java Extension Pack
2. Install Quarkus Extension
3. Configure Java 21 as default

## Project Structure

```
falsify-crawler/
├── crawler-common/              # Shared module
│   ├── src/main/java/ai/falsify/crawlers/common/
│   │   ├── config/             # Configuration framework
│   │   ├── exception/          # Exception hierarchy
│   │   ├── model/             # Data models
│   │   └── service/           # Shared services
│   └── src/test/java/         # Tests
├── crawler-drucker/            # Drucker crawler
├── crawler-caspit/             # Caspit crawler
├── crawler-manager/            # Future orchestration service
├── docs/                       # Documentation
├── pom.xml                     # Parent POM
└── README.md                   # Main documentation
```

### Package Structure

All Java code follows the package structure:
```
ai.falsify.crawlers.{module}.{category}
```

Examples:
- `ai.falsify.crawlers.common.service` - Common services
- `ai.falsify.crawlers.drucker` - Drucker crawler main classes
- `ai.falsify.crawlers.caspit.model` - Caspit-specific models

## Creating a New Crawler

This section provides a step-by-step guide to creating a new crawler using the common module.

### Step 1: Create Maven Module

1. **Add Module to Parent POM**:
   ```xml
   <modules>
       <module>crawler-common</module>
       <module>crawler-drucker</module>
       <module>crawler-caspit</module>
       <module>crawler-yournewcrawler</module> <!-- Add this -->
   </modules>
   ```

2. **Create Module Directory**:
   ```bash
   mkdir crawler-yournewcrawler
   cd crawler-yournewcrawler
   ```

3. **Create Module POM** (`crawler-yournewcrawler/pom.xml`):
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                                http://maven.apache.org/xsd/maven-4.0.0.xsd">
       <modelVersion>4.0.0</modelVersion>
       
       <parent>
           <groupId>ai.falsify</groupId>
           <artifactId>falsify-crawler-parent</artifactId>
           <version>1.0.0-SNAPSHOT</version>
       </parent>
       
       <artifactId>crawler-yournewcrawler</artifactId>
       <name>Your New Crawler</name>
       
       <dependencies>
           <!-- Common module dependency -->
           <dependency>
               <groupId>ai.falsify</groupId>
               <artifactId>crawler-common</artifactId>
               <version>${project.version}</version>
           </dependency>
           
           <!-- Quarkus dependencies inherited from parent -->
       </dependencies>
   </project>
   ```

### Step 2: Create Directory Structure

```bash
mkdir -p src/main/java/ai/falsify/crawlers
mkdir -p src/main/resources
mkdir -p src/test/java/ai/falsify/crawlers
```

### Step 3: Implement Main Crawler Class

Create `src/main/java/ai/falsify/crawlers/YourNewCrawler.java`:

```java
package ai.falsify.crawlers;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.exception.CrawlingException;
import ai.falsify.crawlers.common.exception.NetworkException;
import ai.falsify.crawlers.common.exception.PersistenceException;
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class YourNewCrawler {
    
    private static final Logger LOG = Logger.getLogger(YourNewCrawler.class);
    private static final String CRAWLER_NAME = "yournewcrawler";
    
    @Inject
    CrawlerConfiguration config;
    
    @Inject
    ContentValidator contentValidator;
    
    @Inject
    RetryService retryService;
    
    @Inject
    DeduplicationService deduplicationService;
    
    public CrawlResult crawl() {
        LOG.infof("Starting %s crawl operation", CRAWLER_NAME);
        Instant startTime = Instant.now();
        
        List<Article> articles = new ArrayList<>();
        int totalFound = 0;
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // Step 1: Get URLs to crawl
            List<String> urls = getUrlsToCrawl();
            totalFound = urls.size();
            LOG.infof("Found %d URLs to process", totalFound);
            
            // Step 2: Process each URL
            for (String url : urls) {
                try {
                    if (processUrl(url, articles)) {
                        processed++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    failed++;
                    String error = String.format("Failed to process %s: %s", url, e.getMessage());
                    errors.add(error);
                    LOG.error(error, e);
                }
            }
            
            Instant endTime = Instant.now();
            long processingTimeMs = Duration.between(startTime, endTime).toMillis();
            
            LOG.infof("Crawl completed: %d processed, %d skipped, %d failed in %dms", 
                     processed, skipped, failed, processingTimeMs);
            
            return new CrawlResult.Builder()
                .totalArticlesFound(totalFound)
                .articlesProcessed(processed)
                .articlesSkipped(skipped)
                .articlesFailed(failed)
                .processingTimeMs(processingTimeMs)
                .articles(articles)
                .startTime(startTime)
                .endTime(endTime)
                .crawlerSource(CRAWLER_NAME)
                .errors(errors)
                .build();
                
        } catch (Exception e) {
            LOG.error("Crawl operation failed", e);
            throw new CrawlingException("Crawl failed for " + CRAWLER_NAME, e);
        }
    }
    
    private List<String> getUrlsToCrawl() throws NetworkException {
        // Implement your URL discovery logic here
        // This could involve:
        // 1. Fetching a sitemap
        // 2. Crawling category pages
        // 3. Using an API
        // 4. Reading from a configuration file
        
        return retryService.executeWithRetry(() -> {
            // Example: Fetch URLs from a listing page
            Document doc = Jsoup.connect("https://yoursite.com/articles")
                .timeout((int) config.network().timeout().toMillis())
                .userAgent(config.network().userAgent())
                .get();
            
            Elements links = doc.select("a[href*='/article/']");
            return links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> !url.isEmpty())
                .distinct()
                .toList();
                
        }, "fetch-urls", NetworkException.class);
    }
    
    private boolean processUrl(String url, List<Article> articles) throws Exception {
        // Check deduplication first
        if (!deduplicationService.isNewUrl(CRAWLER_NAME, url)) {
            LOG.debugf("Skipping duplicate URL: %s", url);
            return false;
        }
        
        // Fetch and parse content
        Article article = retryService.executeWithRetry(() -> {
            return fetchAndParseArticle(url);
        }, "fetch-article-" + url, NetworkException.class);
        
        // Validate content
        try {
            contentValidator.validateArticle(article.title(), article.url(), article.text());
        } catch (ContentValidationException e) {
            LOG.warnf("Content validation failed for %s: %s", url, e.getMessage());
            throw e;
        }
        
        // Save to database
        retryService.executeWithRetry(() -> {
            saveArticle(article);
            return null;
        }, "save-article-" + url, PersistenceException.class);
        
        articles.add(article);
        LOG.debugf("Successfully processed article: %s", article.title());
        return true;
    }
    
    private Article fetchAndParseArticle(String url) throws NetworkException {
        try {
            Document doc = Jsoup.connect(url)
                .timeout((int) config.network().timeout().toMillis())
                .userAgent(config.network().userAgent())
                .get();
            
            // Extract article data - customize based on your site's structure
            String title = extractTitle(doc);
            String content = extractContent(doc);
            
            return new Article(title, url, content);
            
        } catch (Exception e) {
            throw new NetworkException("Failed to fetch article from " + url, e);
        }
    }
    
    private String extractTitle(Document doc) {
        // Customize based on your site's HTML structure
        Element titleElement = doc.selectFirst("h1, .article-title, .post-title");
        return titleElement != null ? titleElement.text().trim() : "No Title";
    }
    
    private String extractContent(Document doc) {
        // Customize based on your site's HTML structure
        Elements contentElements = doc.select(".article-content, .post-content, .entry-content p");
        
        StringBuilder content = new StringBuilder();
        for (Element element : contentElements) {
            content.append(element.text()).append("\n");
        }
        
        return content.toString().trim();
    }
    
    @Transactional
    private void saveArticle(Article article) throws PersistenceException {
        try {
            ArticleEntity entity = new ArticleEntity();
            entity.title = article.title();
            entity.url = article.url();
            entity.text = article.text();
            entity.crawlerSource = CRAWLER_NAME;
            entity.persist();
            
        } catch (Exception e) {
            throw new PersistenceException("Failed to save article: " + article.url(), e);
        }
    }
}
```

### Step 4: Create REST Resource

Create `src/main/java/ai/falsify/crawlers/YourNewCrawlerResource.java`:

```java
package ai.falsify.crawlers;

import ai.falsify.crawlers.common.model.CrawlResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/yournewcrawler")
public class YourNewCrawlerResource {
    
    @Inject
    YourNewCrawler crawler;
    
    @POST
    @Path("/crawl")
    @Produces(MediaType.APPLICATION_JSON)
    public Response crawl() {
        try {
            CrawlResult result = crawler.crawl();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(new HealthResponse("OK", "yournewcrawler")).build();
    }
    
    public static record ErrorResponse(String error) {}
    public static record HealthResponse(String status, String crawler) {}
}
```

### Step 5: Configuration

Create `src/main/resources/application.properties`:

```properties
# Application configuration
quarkus.application.name=yournewcrawler
quarkus.http.port=8082

# Database configuration (inherited from common)
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.database.generation=update

# Crawler-specific configuration
crawler.yournewcrawler.base-url=https://yoursite.com
crawler.yournewcrawler.max-pages=100

# Override common settings if needed
crawler.common.content.min-content-length=200
crawler.common.retry.max-attempts=5

# Logging
quarkus.log.category."ai.falsify.crawlers".level=INFO
quarkus.log.category."ai.falsify.crawlers.YourNewCrawler".level=DEBUG
```

### Step 6: Testing

Create comprehensive tests for your crawler:

#### Unit Test

Create `src/test/java/ai/falsify/crawlers/YourNewCrawlerTest.java`:

```java
package ai.falsify.crawlers;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.model.CrawlResult;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
class YourNewCrawlerTest {
    
    @Inject
    YourNewCrawler crawler;
    
    @InjectMock
    DeduplicationService deduplicationService;
    
    @InjectMock
    ContentValidator contentValidator;
    
    @InjectMock
    RetryService retryService;
    
    @BeforeEach
    void setUp() {
        // Setup common mocks
        Mockito.when(deduplicationService.isNewUrl(anyString(), anyString()))
               .thenReturn(true);
        
        Mockito.when(retryService.executeWithRetry(any(), anyString()))
               .thenAnswer(invocation -> {
                   return ((java.util.concurrent.Callable<?>) invocation.getArgument(0)).call();
               });
    }
    
    @Test
    void testCrawlSuccess() {
        // Test successful crawl operation
        CrawlResult result = crawler.crawl();
        
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("yournewcrawler", result.crawlerSource());
    }
    
    @Test
    void testDuplicateUrlHandling() {
        // Test that duplicate URLs are skipped
        Mockito.when(deduplicationService.isNewUrl("yournewcrawler", anyString()))
               .thenReturn(false);
        
        CrawlResult result = crawler.crawl();
        
        assertTrue(result.articlesSkipped() > 0);
    }
}
```

#### Integration Test

Create `src/test/java/ai/falsify/crawlers/YourNewCrawlerIT.java`:

```java
package ai.falsify.crawlers;

import ai.falsify.crawlers.common.model.CrawlResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class YourNewCrawlerIT {
    
    @Inject
    YourNewCrawler crawler;
    
    @Test
    void testFullCrawlIntegration() {
        // Test full crawl with real dependencies
        CrawlResult result = crawler.crawl();
        
        assertNotNull(result);
        assertNotNull(result.startTime());
        assertNotNull(result.endTime());
        assertTrue(result.processingTimeMs() > 0);
    }
}
```

### Step 7: Documentation

Create `crawler-yournewcrawler/README.md`:

```markdown
# Your New Crawler

Description of what this crawler does and which site it targets.

## Configuration

Specific configuration options for this crawler.

## Usage

How to run and use this crawler.

## Development

Development notes specific to this crawler.
```

## Common Module Integration

### Best Practices

1. **Always Use Common Services**: Don't duplicate functionality that exists in the common module
2. **Follow Configuration Patterns**: Extend common configuration rather than creating separate systems
3. **Handle Exceptions Properly**: Use the common exception hierarchy
4. **Implement Proper Logging**: Use structured logging patterns from common module
5. **Test Integration**: Always test with the common module services

### Service Injection

```java
@ApplicationScoped
public class MyCrawler {
    
    // Always inject common services
    @Inject
    ContentValidator contentValidator;
    
    @Inject
    RetryService retryService;
    
    @Inject
    DeduplicationService deduplicationService;
    
    @Inject
    CrawlerConfiguration config;
}
```

### Error Handling

```java
try {
    // Crawler operation
    String content = fetchContent(url);
    Article article = parseContent(content);
    contentValidator.validateArticle(article.title(), article.url(), article.text());
    
} catch (NetworkException e) {
    // Handle network-specific errors
    LOG.warnf("Network error for %s: %s", url, e.getMessage());
    throw e;
    
} catch (ContentValidationException e) {
    // Handle validation errors
    LOG.warnf("Content validation failed for %s: %s", url, e.getMessage());
    throw e;
    
} catch (Exception e) {
    // Handle unexpected errors
    LOG.errorf("Unexpected error processing %s", url, e);
    throw new CrawlingException("Processing failed", e);
}
```

## Testing Guidelines

### Test Structure

```
src/test/java/
├── ai/falsify/crawlers/
│   ├── YourCrawlerTest.java           # Unit tests
│   ├── YourCrawlerIT.java             # Integration tests
│   └── YourCrawlerResourceTest.java   # REST endpoint tests
```

### Test Categories

1. **Unit Tests**: Test individual methods with mocked dependencies
2. **Integration Tests**: Test with real common module services
3. **End-to-End Tests**: Test complete crawl operations
4. **Performance Tests**: Test under load conditions

### Mocking Common Services

```java
@QuarkusTest
class MyCrawlerTest {
    
    @InjectMock
    DeduplicationService deduplicationService;
    
    @InjectMock
    ContentValidator contentValidator;
    
    @Test
    void testWithMockedServices() {
        // Setup mocks
        when(deduplicationService.isNewUrl(anyString(), anyString()))
            .thenReturn(true);
        
        doNothing().when(contentValidator)
            .validateArticle(anyString(), anyString(), anyString());
        
        // Test your crawler
        CrawlResult result = crawler.crawl();
        
        // Verify interactions
        verify(deduplicationService).isNewUrl("mycrawler", "test-url");
        verify(contentValidator).validateArticle(anyString(), anyString(), anyString());
    }
}
```

## Code Conventions

### Naming Conventions

- **Classes**: PascalCase (e.g., `DruckerCrawler`)
- **Methods**: camelCase (e.g., `processArticle`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `CRAWLER_NAME`)
- **Packages**: lowercase with dots (e.g., `ai.falsify.crawlers.drucker`)

### Code Style

- Use dependency injection with `@Inject`
- Mark services as `@ApplicationScoped`
- Use `@Transactional` for database operations
- Follow Java 21 features (records, pattern matching, etc.)
- Use meaningful variable names
- Add comprehensive JavaDoc for public APIs

### Error Handling

- Use specific exception types from common module
- Log errors with appropriate levels
- Include context in error messages
- Don't swallow exceptions

### Performance

- Use connection pooling for HTTP clients
- Implement proper retry logic
- Consider async operations for I/O
- Monitor resource usage

## Deployment

### Development

```bash
# Run in development mode
mvn quarkus:dev -pl crawler-yournewcrawler
```

### Production

```bash
# Build for production
mvn package -pl crawler-yournewcrawler

# Run production build
java -jar crawler-yournewcrawler/target/quarkus-app/quarkus-run.jar
```

### Docker

Create `Dockerfile` in your crawler module:

```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-21:1.20

ENV LANGUAGE='en_US:en'

COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
```

Build and run:

```bash
docker build -t yournewcrawler .
docker run -p 8082:8080 yournewcrawler
```

## Troubleshooting

### Common Issues

1. **Dependency Injection Failures**: Ensure proper CDI annotations
2. **Configuration Issues**: Check property names and values
3. **Database Connection**: Verify PostgreSQL is running
4. **Redis Connection**: Verify Redis is accessible
5. **Network Timeouts**: Adjust timeout configurations

### Debug Tips

1. Enable debug logging for your crawler
2. Use Quarkus Dev UI for monitoring
3. Check health endpoints
4. Monitor resource usage
5. Use profiling tools for performance issues

For more specific troubleshooting, see the [Troubleshooting Guide](TROUBLESHOOTING.md).