# Prediction Analysis Module

This module provides AI-powered prediction analysis capabilities for articles scraped by the crawler system. It's designed as a standalone service that can analyze articles and extract predictions using various AI models, with Gemini 2.5 Flash as the default.

## Features

- **AI-Powered Prediction Extraction**: Uses Google's Gemini model for intelligent prediction identification
- **Batch Processing**: Supports processing multiple articles in batches for efficiency
- **Async Processing**: Non-blocking analysis jobs with real-time progress updates
- **WebSocket Notifications**: Real-time updates on job progress and completion
- **Flexible Configuration**: Configurable batch sizes, timeouts, and model parameters
- **Fallback Support**: Automatic fallback to mock predictions if AI service is unavailable

## Architecture

The module is built with:
- **Quarkus**: Modern Java framework for cloud-native applications
- **LangChain4j**: Java library for LLM integration
- **Google Gemini**: Default AI model for prediction extraction
- **WebSockets**: Real-time communication with clients
- **PostgreSQL**: Shared database with other crawler modules

## Configuration

Key configuration properties in `application.properties`:

```properties
# LLM Configuration - Gemini as default
prediction.llm.enabled=true
prediction.llm.provider=gemini
prediction.llm.model=gemini-1.5-flash
prediction.llm.api-key=${GEMINI_API_KEY:}
prediction.llm.max-batch-size=10
prediction.llm.batch-timeout-seconds=60

# Analysis job configuration
prediction.analysis.max-concurrent-jobs=3
prediction.analysis.job-timeout-minutes=30
```

## API Endpoints

### Start Analysis
```
POST /api/prediction-analysis/start
{
  "articleIds": [1, 2, 3],
  "analysisType": "gemini"
}
```

### Get Job Status
```
GET /api/prediction-analysis/jobs/{jobId}
```

### Get Job Results
```
GET /api/prediction-analysis/jobs/{jobId}/results
```

### Cancel Job
```
DELETE /api/prediction-analysis/jobs/{jobId}
```

### Get Job History
```
GET /api/prediction-analysis/history?page=0&size=20
```

### System Status
```
GET /api/prediction-analysis/status
```

## WebSocket Updates

Connect to `/ws/analysis` to receive real-time updates:

```javascript
const ws = new WebSocket('ws://localhost:8083/ws/analysis');

ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};
```

Message types:
- `job.status.update`: Job status changed
- `job.progress.update`: Article processed
- `job.completed`: Job finished successfully
- `job.failed`: Job failed with error
- `job.cancelled`: Job was cancelled

## Running the Module

1. Set up environment variables:
```bash
export GEMINI_API_KEY="your-gemini-api-key"
export DB_URL="jdbc:postgresql://localhost:5432/crawler_db"
export REDIS_URL="redis://localhost:6379"
```

2. Run in development mode:
```bash
mvn quarkus:dev
```

3. The service will be available at `http://localhost:8083`

## Batch Processing

The module supports intelligent batch processing:

- **Max Batch Size**: Configure the maximum number of articles processed in a single LLM request
- **Sequential Processing**: When `max-batch-size=1`, articles are processed one by one
- **Batch Optimization**: Automatically groups articles into optimal batch sizes
- **Rate Limiting**: Respects API rate limits and includes delays between requests

## Error Handling

The module includes comprehensive error handling:

- **Graceful Degradation**: Falls back to mock predictions if AI service fails
- **Retry Logic**: Automatic retries for transient failures
- **Circuit Breaker**: Prevents cascading failures
- **Detailed Logging**: Comprehensive logging for debugging and monitoring

## Integration

This module integrates with:

- **Crawler Common**: Shared entities and services
- **Crawler Manager**: UI integration for triggering analysis
- **Database**: Shared PostgreSQL database for articles and predictions
- **Redis**: Shared cache for deduplication and performance

## Development

To extend the module:

1. Add new prediction extractors by implementing `BatchPredictionExtractor`
2. Update `PredictionExtractorFactory` to support new extractor types
3. Add new API endpoints in `PredictionAnalysisResource`
4. Extend WebSocket notifications in `AnalysisNotificationService`

## Testing

Run tests with:
```bash
mvn test
```

The module includes unit tests for all major components and integration tests for the REST API.