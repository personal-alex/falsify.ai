# Multi-Crawler Development Setup

This document describes how to run multiple crawler modules simultaneously for development and testing purposes.

## Overview

The project supports running multiple crawler modules concurrently:
- **crawler-caspit**: Crawls Ben Caspit articles from Maariv (Port 8080)
- **crawler-drucker**: Crawls articles from Drucker10.net (Port 8081)

Each crawler operates independently with its own configuration, Redis key space, and crawler source identification.

## Quick Start

### 1. Start Both Crawlers Automatically

```bash
# Start both crawlers with a single command
./run-both-crawlers.sh
```

This script will:
- ✅ Check port availability
- 🚀 Start both crawlers on different ports
- 📋 Create separate log files
- 🛑 Handle graceful shutdown with Ctrl+C

### 2. Start Crawlers Manually

If you prefer manual control, use separate terminal windows:

```bash
# Terminal 1: Start crawler-caspit
mvn quarkus:dev -pl crawler-caspit

# Terminal 2: Start crawler-drucker  
mvn quarkus:dev -pl crawler-drucker
```

## Configuration Details

### Port Assignments

| Crawler | Port | URL |
|---------|------|-----|
| crawler-caspit | 8080 | http://localhost:8080 |
| crawler-drucker | 8081 | http://localhost:8081 |

### Redis Configuration

Each crawler uses separate Redis key prefixes to avoid conflicts:

| Crawler | Redis Key Prefix | Purpose |
|---------|------------------|---------|
| crawler-caspit | `crawler:caspit` | URL deduplication |
| crawler-drucker | `crawler:drucker` | URL deduplication |

### Crawler Source Identification

Articles are tagged with their source crawler:

| Crawler | Source Name | Database Field |
|---------|-------------|----------------|
| crawler-caspit | `caspit` | `crawler_source` |
| crawler-drucker | `drucker` | `crawler_source` |

## API Endpoints

### Crawler-Caspit (Port 8080)

```bash
# Health check
curl http://localhost:8080/q/health

# Get crawler status
curl http://localhost:8080/caspit/status

# Start crawling
curl -X POST http://localhost:8080/caspit/crawl

# Development UI
http://localhost:8080/q/dev/
```

### Crawler-Drucker (Port 8081)

```bash
# Health check
curl http://localhost:8081/q/health

# Get crawler status
curl http://localhost:8081/drucker/status

# Start crawling
curl -X POST http://localhost:8081/drucker/crawl

# Development UI
http://localhost:8081/q/dev/
```

## Monitoring and Logs

### Log Files

When using the startup script, logs are written to separate files:

```bash
# Watch caspit logs
tail -f caspit.log

# Watch drucker logs
tail -f drucker.log

# Watch both logs simultaneously
tail -f caspit.log drucker.log
```

### Real-time Monitoring

```bash
# Monitor both crawlers' health
watch -n 5 'echo "=== Caspit Health ===" && curl -s http://localhost:8080/q/health | jq . && echo -e "\n=== Drucker Health ===" && curl -s http://localhost:8081/q/health | jq .'

# Monitor crawler status
watch -n 10 'echo "=== Caspit Status ===" && curl -s http://localhost:8080/caspit/status && echo -e "\n=== Drucker Status ===" && curl -s http://localhost:8081/drucker/status'
```

## Testing the Setup

### Automated Testing

Run the comprehensive test suite:

```bash
./test-multi-crawler-setup.sh
```

This will verify:
- ✅ Project structure
- ✅ Configuration files
- ✅ Port assignments
- ✅ Redis key prefixes
- ✅ Crawler source configurations
- ✅ Compilation success
- ✅ Port availability
- ✅ Script executability

### Manual Testing

1. **Start the crawlers:**
   ```bash
   ./run-both-crawlers.sh
   ```

2. **Test health endpoints:**
   ```bash
   curl http://localhost:8080/q/health
   curl http://localhost:8081/q/health
   ```

3. **Test crawler status:**
   ```bash
   curl http://localhost:8080/caspit/status
   curl http://localhost:8081/drucker/status
   ```

4. **Start crawling operations:**
   ```bash
   curl -X POST http://localhost:8080/caspit/crawl
   curl -X POST http://localhost:8081/drucker/crawl
   ```

## Database Integration

### Shared Database

Both crawlers use the same PostgreSQL database (`falsify`) but store articles with different `crawler_source` values:

```sql
-- View articles by crawler source
SELECT title, url, crawler_source, created_at 
FROM articles 
WHERE crawler_source = 'caspit';

SELECT title, url, crawler_source, created_at 
FROM articles 
WHERE crawler_source = 'drucker';

-- Count articles by source
SELECT crawler_source, COUNT(*) as article_count 
FROM articles 
GROUP BY crawler_source;
```

### Redis Deduplication

Each crawler maintains its own deduplication cache:

```bash
# Connect to Redis and check keys
redis-cli

# View caspit keys
KEYS crawler:caspit:*

# View drucker keys  
KEYS crawler:drucker:*
```

## Troubleshooting

### Common Issues

#### Port Conflicts

**Problem:** Port already in use error
```
❌ Error: Port 8080 is already in use
```

**Solution:**
```bash
# Find what's using the port
lsof -i :8080
lsof -i :8081

# Kill the process if needed
kill -9 <PID>
```

#### Database Connection Issues

**Problem:** Database connection failures

**Solution:**
```bash
# Check PostgreSQL status
brew services list | grep postgresql

# Start PostgreSQL if needed
brew services start postgresql

# Test connection
psql -h localhost -U postgres -d falsify -c "SELECT 1;"
```

#### Redis Connection Issues

**Problem:** Redis connection failures

**Solution:**
In development mode, Quarkus automatically starts Redis dev services. If there are issues:

```bash
# Check if Redis is running
redis-cli ping

# If using external Redis, start it
brew services start redis
```

#### Compilation Errors

**Problem:** Maven compilation failures

**Solution:**
```bash
# Clean and recompile
mvn clean compile

# Check for dependency issues
mvn dependency:tree

# Compile specific modules
mvn compile -pl crawler-caspit
mvn compile -pl crawler-drucker
mvn compile -pl crawler-common
```

### Configuration Validation

Verify your configuration is correct:

```bash
# Check port configurations
grep "quarkus.http.port" crawler-*/src/main/resources/application.properties

# Check Redis prefixes
grep "crawler.common.redis.key-prefix" crawler-*/src/main/resources/application.properties

# Check crawler sources
grep -E "(caspit.crawler.source|crawler.source.name)" crawler-*/src/main/resources/application.properties
```

### Performance Monitoring

Monitor resource usage when running both crawlers:

```bash
# Monitor CPU and memory usage
top -p $(pgrep -f "quarkus:dev")

# Monitor network connections
netstat -tulpn | grep -E "(8080|8081)"

# Monitor database connections
psql -h localhost -U postgres -d falsify -c "SELECT * FROM pg_stat_activity WHERE application_name LIKE '%quarkus%';"
```

## Development Workflow

### Typical Development Session

1. **Start both crawlers:**
   ```bash
   ./run-both-crawlers.sh
   ```

2. **Make code changes** in your IDE

3. **Quarkus live reload** will automatically restart the affected crawler

4. **Test changes:**
   ```bash
   curl -X POST http://localhost:8080/caspit/crawl
   curl -X POST http://localhost:8081/drucker/crawl
   ```

5. **Monitor logs:**
   ```bash
   tail -f caspit.log drucker.log
   ```

6. **Stop crawlers** when done:
   ```
   Press Ctrl+C in the terminal running the script
   ```

### Adding New Crawlers

To add a new crawler module:

1. **Create new module** following the existing pattern
2. **Configure unique port** in `application.properties`
3. **Set unique Redis key prefix** 
4. **Configure crawler source name**
5. **Update startup script** to include the new crawler
6. **Update test script** to validate the new configuration
7. **Update this documentation**

## Best Practices

### Development

- ✅ Always use the startup script for consistent environment
- ✅ Monitor logs regularly for errors and performance issues
- ✅ Test both crawlers after making changes
- ✅ Use the test script to validate configuration changes
- ✅ Keep crawler source names consistent with module names

### Production Considerations

- 🔧 Configure external Redis for production
- 🔧 Use environment-specific configuration profiles
- 🔧 Implement proper monitoring and alerting
- 🔧 Consider load balancing for high availability
- 🔧 Set up proper logging aggregation
- 🔧 Configure database connection pooling

## Files and Scripts

| File | Purpose |
|------|---------|
| `run-both-crawlers.sh` | Start both crawlers simultaneously |
| `test-multi-crawler-setup.sh` | Comprehensive setup testing |
| `MULTI_CRAWLER_SETUP.md` | This documentation |
| `crawler-caspit/src/main/resources/application.properties` | Caspit crawler configuration |
| `crawler-drucker/src/main/resources/application.properties` | Drucker crawler configuration |

## Support

For issues or questions:

1. **Check the logs** first: `tail -f caspit.log drucker.log`
2. **Run the test script**: `./test-multi-crawler-setup.sh`
3. **Verify configuration** using the troubleshooting commands above
4. **Check database and Redis connectivity**
5. **Review this documentation** for common solutions

---

*Last updated: $(date)*
*Multi-crawler setup version: 1.0*