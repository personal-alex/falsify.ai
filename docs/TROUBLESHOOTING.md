# Troubleshooting Guide

This guide helps you diagnose and resolve common issues in the Falsify Crawler System.

## Table of Contents

1. [General Troubleshooting](#general-troubleshooting)
2. [Common Module Issues](#common-module-issues)
3. [Configuration Problems](#configuration-problems)
4. [Database Issues](#database-issues)
5. [Redis Issues](#redis-issues)
6. [Network and Crawling Issues](#network-and-crawling-issues)
7. [Performance Issues](#performance-issues)
8. [Testing Issues](#testing-issues)
9. [Deployment Issues](#deployment-issues)

## General Troubleshooting

### Enable Debug Logging

First step in troubleshooting is to enable debug logging:

```properties
# Enable debug logging for all crawler components
quarkus.log.category."ai.falsify.crawlers".level=DEBUG

# Enable debug logging for specific components
quarkus.log.category."ai.falsify.crawlers.common.service".level=DEBUG
quarkus.log.category."ai.falsify.crawlers.drucker".level=DEBUG

# Enable Quarkus debug logging
quarkus.log.category."io.quarkus".level=DEBUG
```

### Check Health Endpoints

Use Quarkus health endpoints to check system status:

```bash
# Overall health
curl http://localhost:8080/q/health

# Readiness check
curl http://localhost:8080/q/health/ready

# Liveness check  
curl http://localhost:8080/q/health/live

# Specific health checks
curl http://localhost:8080/q/health/group/ready
```

### View Application Metrics

Check application metrics for insights:

```bash
# Prometheus metrics
curl http://localhost:8080/q/metrics

# Application info
curl http://localhost:8080/q/info
```

### Check Dev UI

In development mode, use the Quarkus Dev UI:

- Navigate to http://localhost:8080/q/dev/
- Check configuration, health, metrics, and logs
- Use the configuration editor to test different settings

## Common Module Issues

### Issue: CDI Injection Failures

**Symptoms:**
```
jakarta.enterprise.inject.UnsatisfiedResolutionException: Unsatisfied dependency for type ai.falsify.crawlers.common.service.ContentValidator
```

**Causes:**
- Missing `@ApplicationScoped` annotation on service classes
- Incorrect package structure
- Missing CDI beans.xml file

**Solutions:**

1. **Check Service Annotations**:
   ```java
   @ApplicationScoped  // Make sure this is present
   public class ContentValidator {
       // Implementation
   }
   ```

2. **Verify Package Structure**:
   ```
   ai.falsify.crawlers.common.service.ContentValidator ✓
   ai.falsify.crawlers.service.ContentValidator ✗
   ```

3. **Add beans.xml** (if missing):
   ```xml
   <!-- src/main/resources/META-INF/beans.xml -->
   <?xml version="1.0" encoding="UTF-8"?>
   <beans xmlns="http://xmlns.jcp.org/xml/ns/javaee"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee 
                              http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd"
          version="2.0" bean-discovery-mode="all">
   </beans>
   ```

### Issue: Configuration Validation Errors

**Symptoms:**
```
Configuration validation failed: Network timeout must be positive
```

**Solutions:**

1. **Check Configuration Values**:
   ```properties
   # Ensure positive duration values
   crawler.common.network.timeout=PT30S  # Not PT-30S or 0
   crawler.common.retry.max-attempts=3   # Not 0 or negative
   ```

2. **Verify Duration Format**:
   ```properties
   # Correct ISO-8601 duration formats
   crawler.common.network.timeout=PT30S      # 30 seconds
   crawler.common.retry.initial-delay=PT1S   # 1 second
   crawler.common.redis.default-ttl=PT24H    # 24 hours
   ```

3. **Check Validation Constraints**:
   ```java
   // Review validation annotations in configuration interfaces
   @Min(value = 1, message = "Max attempts must be at least 1")
   int maxAttempts();
   ```

### Issue: Service Integration Problems

**Symptoms:**
- Services not working together correctly
- Unexpected behavior in service chains

**Solutions:**

1. **Check Service Dependencies**:
   ```java
   @ApplicationScoped
   public class MyCrawler {
       @Inject
       ContentValidator contentValidator;  // Check injection
       
       @Inject
       RetryService retryService;         // Check injection
       
       @Inject
       DeduplicationService deduplicationService;  // Check injection
   }
   ```

2. **Verify Service Initialization Order**:
   ```java
   @ApplicationScoped
   public class ServiceInitializer {
       @EventObserver
       void onStart(@Observes StartupEvent event) {
           // Initialize services in correct order
       }
   }
   ```

## Configuration Problems

### Issue: Properties Not Loading

**Symptoms:**
- Default values being used instead of configured values
- Configuration changes not taking effect

**Solutions:**

1. **Check Property Names**:
   ```properties
   # Correct
   crawler.common.network.timeout=PT30S
   
   # Incorrect
   crawler.common.network-timeout=PT30S  # Wrong separator
   crawler-common.network.timeout=PT30S  # Wrong prefix
   ```

2. **Verify Profile Activation**:
   ```bash
   # Check active profile
   java -Dquarkus.profile=prod -jar app.jar
   
   # Or via environment variable
   export QUARKUS_PROFILE=prod
   ```

3. **Check Property Precedence**:
   ```bash
   # System properties override everything
   java -Dcrawler.common.network.timeout=PT45S -jar app.jar
   
   # Environment variables
   export CRAWLER_COMMON_NETWORK_TIMEOUT=PT45S
   ```

### Issue: Type Conversion Errors

**Symptoms:**
```
Could not convert 'invalid-duration' to java.time.Duration
```

**Solutions:**

1. **Use Correct Duration Format**:
   ```properties
   # Correct ISO-8601 duration formats
   timeout=PT30S     # 30 seconds
   timeout=PT5M      # 5 minutes  
   timeout=PT1H      # 1 hour
   timeout=P1D       # 1 day
   
   # Incorrect formats
   timeout=30s       # Missing PT prefix
   timeout=5min      # Wrong format
   ```

2. **Check Numeric Values**:
   ```properties
   # Correct
   max-attempts=3
   
   # Incorrect
   max-attempts=three  # Not a number
   max-attempts=3.5    # Should be integer
   ```

## Database Issues

### Issue: Database Connection Failures

**Symptoms:**
```
Connection to localhost:5432 refused
```

**Solutions:**

1. **Check Database Status**:
   ```bash
   # Check if PostgreSQL is running
   docker ps | grep postgres
   
   # Start PostgreSQL if needed
   docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:17
   ```

2. **Verify Connection Configuration**:
   ```properties
   # Check database configuration
   quarkus.datasource.db-kind=postgresql
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus
   quarkus.datasource.username=quarkus
   quarkus.datasource.password=quarkus
   ```

3. **Test Connection**:
   ```bash
   # Test database connection
   psql -h localhost -p 5432 -U quarkus -d quarkus
   ```

### Issue: Schema/Table Issues

**Symptoms:**
```
Table "articleentity" doesn't exist
```

**Solutions:**

1. **Check Hibernate Configuration**:
   ```properties
   # For development - auto-create tables
   quarkus.hibernate-orm.database.generation=drop-and-create
   
   # For production - validate existing schema
   quarkus.hibernate-orm.database.generation=validate
   ```

2. **Run Database Migrations**:
   ```bash
   # If using Flyway
   mvn flyway:migrate
   
   # If using Liquibase  
   mvn liquibase:update
   ```

### Issue: Transaction Problems

**Symptoms:**
```
Transaction marked for rollback
```

**Solutions:**

1. **Check Transaction Annotations**:
   ```java
   @Transactional  // Make sure this is present for database operations
   public void saveArticle(Article article) {
       ArticleEntity entity = new ArticleEntity();
       entity.persist();
   }
   ```

2. **Handle Exceptions Properly**:
   ```java
   @Transactional
   public void processArticle(Article article) {
       try {
           // Database operations
           entity.persist();
       } catch (Exception e) {
           // Log error but let transaction roll back
           LOG.error("Failed to save article", e);
           throw e;  // Re-throw to trigger rollback
       }
   }
   ```

## Redis Issues

### Issue: Redis Connection Problems

**Symptoms:**
```
Unable to connect to Redis server at localhost:6379
```

**Solutions:**

1. **Check Redis Status**:
   ```bash
   # Check if Redis is running
   docker ps | grep redis
   
   # Start Redis if needed
   docker run -d --name redis -p 6379:6379 redis:7
   
   # Test Redis connection
   redis-cli ping
   ```

2. **Verify Redis Configuration**:
   ```properties
   # Check Redis configuration
   quarkus.redis.hosts=redis://localhost:6379
   quarkus.redis.timeout=5s
   ```

3. **Check Redis Authentication**:
   ```properties
   # If Redis requires authentication
   quarkus.redis.password=your-redis-password
   ```

### Issue: Redis Key Issues

**Symptoms:**
- Keys not found when expected
- Duplicate processing despite deduplication

**Solutions:**

1. **Check Key Patterns**:
   ```bash
   # Connect to Redis and check keys
   redis-cli
   > KEYS crawler:*
   > GET crawler:drucker:url:https___example.com_article
   ```

2. **Verify Key Generation**:
   ```java
   // Check how keys are generated in DeduplicationService
   String key = String.format("%s:%s:url:%s", 
       config.redis().keyPrefix(), 
       crawlerName, 
       urlToKey(url));
   ```

3. **Check TTL Settings**:
   ```bash
   # Check key expiration
   redis-cli
   > TTL crawler:drucker:url:https___example.com_article
   ```

## Network and Crawling Issues

### Issue: HTTP Timeout Errors

**Symptoms:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solutions:**

1. **Increase Timeout Values**:
   ```properties
   # Increase network timeout
   crawler.common.network.timeout=PT60S
   
   # Increase connection timeout
   crawler.common.network.connection-timeout=PT30S
   ```

2. **Check Target Website**:
   ```bash
   # Test website accessibility
   curl -I https://target-website.com
   
   # Check response time
   curl -w "@curl-format.txt" -o /dev/null -s https://target-website.com
   ```

3. **Implement Retry Logic**:
   ```java
   // Use RetryService for network operations
   String content = retryService.executeWithRetry(() -> {
       return Jsoup.connect(url)
           .timeout((int) config.network().timeout().toMillis())
           .get()
           .html();
   }, "fetch-" + url, NetworkException.class);
   ```

### Issue: Content Parsing Errors

**Symptoms:**
```
Content validation failed: Content too short
```

**Solutions:**

1. **Check HTML Structure**:
   ```java
   // Debug HTML parsing
   Document doc = Jsoup.connect(url).get();
   LOG.debugf("HTML structure: %s", doc.html());
   
   // Check selectors
   Elements content = doc.select(".article-content");
   LOG.debugf("Found %d content elements", content.size());
   ```

2. **Adjust Content Selectors**:
   ```java
   // Try multiple selectors
   String content = extractContent(doc, 
       ".article-content", 
       ".post-content", 
       ".entry-content",
       "article p");
   ```

3. **Relax Validation Rules**:
   ```properties
   # Temporarily reduce minimum content length
   crawler.common.content.min-content-length=50
   ```

### Issue: Rate Limiting

**Symptoms:**
```
HTTP 429 Too Many Requests
```

**Solutions:**

1. **Increase Request Delays**:
   ```properties
   # Add delay between requests
   crawler.common.performance.request-delay=PT5S
   
   # Reduce concurrent requests
   crawler.common.performance.max-concurrent-requests=1
   ```

2. **Implement Exponential Backoff**:
   ```java
   // RetryService already implements exponential backoff
   retryService.executeWithRetry(() -> {
       return fetchContent(url);
   }, "fetch-" + url, NetworkException.class);
   ```

3. **Use Different User Agents**:
   ```properties
   # Rotate user agents
   crawler.common.network.user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
   ```

## Performance Issues

### Issue: Slow Crawling Performance

**Symptoms:**
- Long processing times
- High memory usage
- CPU bottlenecks

**Solutions:**

1. **Optimize Concurrency**:
   ```properties
   # Increase concurrent requests (carefully)
   crawler.common.performance.max-concurrent-requests=10
   
   # Increase thread pool size
   crawler.common.performance.thread-pool-size=20
   ```

2. **Profile Application**:
   ```bash
   # Run with profiling
   java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr -jar app.jar
   
   # Analyze with JProfiler or similar tools
   ```

3. **Monitor Resource Usage**:
   ```bash
   # Check memory usage
   jstat -gc <pid>
   
   # Check thread usage
   jstack <pid>
   ```

### Issue: Memory Leaks

**Symptoms:**
- Increasing memory usage over time
- OutOfMemoryError exceptions

**Solutions:**

1. **Check Connection Pooling**:
   ```java
   // Ensure HTTP connections are properly closed
   try (Response response = httpClient.execute(request)) {
       // Process response
   }
   ```

2. **Monitor Object Creation**:
   ```bash
   # Use memory profiling tools
   java -XX:+HeapDumpOnOutOfMemoryError -jar app.jar
   ```

3. **Optimize Data Structures**:
   ```java
   // Use streaming for large datasets
   articles.stream()
       .filter(this::isValid)
       .map(this::process)
       .forEach(this::save);
   ```

## Testing Issues

### Issue: Test Failures

**Symptoms:**
- Tests failing in CI but passing locally
- Intermittent test failures

**Solutions:**

1. **Check Test Isolation**:
   ```java
   @BeforeEach
   void setUp() {
       // Clean up test data
       deduplicationService.clearProcessedUrls("test-crawler");
   }
   ```

2. **Use Test Profiles**:
   ```java
   @QuarkusTest
   @TestProfile(MyTestProfile.class)
   class MyCrawlerTest {
       // Test implementation
   }
   
   public static class MyTestProfile implements QuarkusTestProfile {
       @Override
       public Map<String, String> getConfigOverrides() {
           return Map.of(
               "crawler.common.network.timeout", "PT5S",
               "crawler.common.retry.max-attempts", "1"
           );
       }
   }
   ```

3. **Mock External Dependencies**:
   ```java
   @QuarkusTest
   class MyCrawlerTest {
       @InjectMock
       DeduplicationService deduplicationService;
       
       @Test
       void testCrawl() {
           when(deduplicationService.isNewUrl(anyString(), anyString()))
               .thenReturn(true);
           // Test implementation
       }
   }
   ```

### Issue: Testcontainers Problems

**Symptoms:**
```
Could not start container
```

**Solutions:**

1. **Check Docker Status**:
   ```bash
   # Ensure Docker is running
   docker info
   
   # Check available resources
   docker system df
   ```

2. **Configure Testcontainers**:
   ```properties
   # Testcontainers configuration
   testcontainers.reuse.enable=true
   testcontainers.ryuk.disabled=false
   ```

3. **Use Specific Container Versions**:
   ```java
   @Container
   static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
       .withDatabaseName("testdb")
       .withUsername("test")
       .withPassword("test");
   ```

## Deployment Issues

### Issue: Application Won't Start

**Symptoms:**
```
Application failed to start
```

**Solutions:**

1. **Check Java Version**:
   ```bash
   # Ensure Java 21 is being used
   java -version
   
   # Set JAVA_HOME if needed
   export JAVA_HOME=/path/to/java21
   ```

2. **Verify Dependencies**:
   ```bash
   # Check for dependency conflicts
   mvn dependency:tree
   
   # Resolve conflicts
   mvn dependency:resolve
   ```

3. **Check Configuration**:
   ```bash
   # Validate configuration
   java -Dquarkus.log.category."io.quarkus.config".level=DEBUG -jar app.jar
   ```

### Issue: Docker Build Problems

**Symptoms:**
```
Docker build failed
```

**Solutions:**

1. **Check Dockerfile**:
   ```dockerfile
   # Use correct base image
   FROM registry.access.redhat.com/ubi8/openjdk-21:1.20
   
   # Ensure correct file permissions
   COPY --chown=185 target/quarkus-app/ /deployments/
   ```

2. **Build with Correct Profile**:
   ```bash
   # Build for production
   mvn package -Dquarkus.package.jar.type=uber-jar
   
   # Build Docker image
   docker build -t my-crawler .
   ```

### Issue: Kubernetes Deployment Problems

**Symptoms:**
- Pods not starting
- Service discovery issues

**Solutions:**

1. **Check Resource Limits**:
   ```yaml
   resources:
     requests:
       memory: "512Mi"
       cpu: "500m"
     limits:
       memory: "1Gi"
       cpu: "1000m"
   ```

2. **Verify ConfigMaps**:
   ```bash
   # Check ConfigMap
   kubectl get configmap crawler-config -o yaml
   
   # Check if mounted correctly
   kubectl exec -it pod-name -- ls /deployments/config
   ```

3. **Check Service Discovery**:
   ```properties
   # Use Kubernetes service names
   quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-service:5432/crawler
   quarkus.redis.hosts=redis://redis-service:6379
   ```

## Getting Help

### Log Analysis

When reporting issues, include relevant logs:

```bash
# Capture logs with debug information
java -Dquarkus.log.level=DEBUG \
     -Dquarkus.log.category."ai.falsify.crawlers".level=DEBUG \
     -jar app.jar > crawler.log 2>&1
```

### System Information

Include system information:

```bash
# Java version
java -version

# Maven version
mvn -version

# Docker version
docker --version

# OS information
uname -a
```

### Configuration Dump

Include configuration information:

```bash
# Dump configuration
curl http://localhost:8080/q/info/config > config.json
```

### Health Check Results

Include health check results:

```bash
# Health status
curl http://localhost:8080/q/health > health.json
```

### Contact Information

For additional support:

1. Check the project documentation
2. Search existing issues in the project repository
3. Create a new issue with:
   - Detailed problem description
   - Steps to reproduce
   - Expected vs actual behavior
   - Relevant logs and configuration
   - System information

Remember to remove any sensitive information (passwords, API keys, etc.) before sharing logs or configuration files.