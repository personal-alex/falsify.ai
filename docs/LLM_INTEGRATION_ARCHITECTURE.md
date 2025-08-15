# LLM Integration Architecture

## Overview

This document outlines the architecture and implementation plan for integrating Large Language Models (LLMs) into the prediction analysis module using LangChain4j. The current implementation provides a complete infrastructure foundation with placeholder implementations that can be enhanced with actual LLM capabilities.

## Architecture Components

### 1. Core Interfaces

#### PredictionExtractor Interface
- **Purpose**: Unified interface for all prediction extraction methods
- **Implementations**: 
  - `MockPredictionExtractor` (fully implemented)
  - `LLMPredictionExtractor` (infrastructure ready with batch support)
- **Key Methods**:
  - `extractPredictions(String articleText, String articleTitle)`
  - `isAvailable()`
  - `getExtractorType()`

#### BatchPredictionExtractor Interface
- **Purpose**: Extended interface for batch processing capabilities
- **Features**: Cost optimization through batch requests
- **Key Methods**:
  - `extractPredictionsBatch(Map<String, ArticleData> articles)`
  - `isBatchModeEnabled()`
  - `getMaxBatchSize()`

#### LangChain4jClient Interface
- **Purpose**: Abstraction layer for LLM operations
- **Implementation**: `LangChain4jClientImpl` (placeholder)
- **Key Operations**:
  - Text generation
  - Embeddings generation
  - Structured data extraction
  - Async operations support

### 2. Configuration Management

#### LLMConfiguration Class
- **Purpose**: Centralized LLM configuration management
- **Features**:
  - Multi-provider support (OpenAI, Anthropic, Azure, HuggingFace)
  - Security considerations for API keys
  - Cost management and rate limiting
  - Validation and error handling

#### Configuration Properties
```properties
# Core Settings
prediction.llm.enabled=false
prediction.llm.provider=openai
prediction.llm.model=gpt-3.5-turbo
prediction.llm.api-key=your-api-key-here

# Request Configuration
prediction.llm.timeout-seconds=30
prediction.llm.max-tokens=1000
prediction.llm.temperature=0.3

# Batch Processing
prediction.llm.batch-mode=false
prediction.llm.batch-size=10
prediction.llm.batch-timeout-seconds=60

# Rate Limiting
prediction.llm.rate-limit-per-minute=60
prediction.llm.rate-limit-per-hour=1000

# Cost Management
prediction.llm.max-cost-per-request=0.10
prediction.llm.daily-cost-limit=10.00

# Fallback and Caching
prediction.llm.fallback-to-mock=true
prediction.llm.enable-caching=true
```

### 3. Extractor Factory and Switching

#### PredictionExtractorFactory
- **Purpose**: Manages extractor selection and fallback logic
- **Features**:
  - Automatic fallback mechanisms
  - Runtime extractor switching
  - Configuration validation
  - Health monitoring

#### Switching Logic
1. **Primary Selection**: Uses configured `prediction.extractor.type`
2. **Availability Check**: Verifies extractor is available and configured
3. **Fallback Logic**: Automatically falls back to available extractors
4. **Preference Handling**: Supports LLM preference with `prediction.extractor.prefer-llm`

## Implementation Phases

### Phase 1: Infrastructure (COMPLETED)
- ✅ Created LLM configuration classes
- ✅ Implemented extractor factory with switching logic
- ✅ Added placeholder LangChain4j client interface
- ✅ Configured application properties
- ✅ Added security considerations for API key management

### Phase 2: LangChain4j Integration (FUTURE)
- [ ] Add LangChain4j dependencies to POM
- [ ] Implement actual LLM client operations
- [ ] Add prompt engineering for prediction extraction
- [ ] Implement structured output parsing
- [ ] Add retry logic with exponential backoff

### Phase 3: Advanced Features (FUTURE)
- [ ] Implement embeddings-based similarity search
- [ ] Add response caching mechanisms
- [ ] Implement cost tracking and budgeting
- [ ] Add A/B testing between different models
- [ ] Implement fine-tuning capabilities

## Security Considerations

### API Key Management
- **Storage**: API keys stored in configuration properties
- **Masking**: Keys are masked in logs and debug output
- **Validation**: Format validation based on provider
- **Environment Variables**: Support for environment-based configuration

### Data Privacy
- **Request Logging**: Configurable request/response logging
- **Data Retention**: No persistent storage of LLM requests/responses
- **Content Filtering**: Input sanitization and validation
- **Compliance**: GDPR and data protection considerations

### Rate Limiting and Cost Control
- **Request Limits**: Per-minute and per-hour rate limiting
- **Cost Budgets**: Daily and per-request cost limits
- **Circuit Breaker**: Automatic failure protection
- **Monitoring**: Usage tracking and alerting

## Provider Support

### OpenAI
- **Models**: GPT-3.5-turbo, GPT-4, GPT-4-turbo
- **API**: REST API with JSON responses
- **Features**: Chat completions, embeddings, structured outputs, batch API
- **Cost**: Token-based pricing
- **Batch Support**: Yes (native batch API)

### Anthropic Claude
- **Models**: Claude-3-sonnet, Claude-3-opus
- **API**: REST API with streaming support
- **Features**: Large context windows, safety features
- **Cost**: Token-based pricing
- **Batch Support**: No (individual requests only)

### Google Gemini
- **Models**: Gemini-1.5-flash, Gemini-1.5-pro
- **API**: REST API via Google AI Studio
- **Features**: Multimodal capabilities, large context windows, competitive pricing
- **Cost**: Very competitive token-based pricing ($0.35/$3.50 per 1M tokens)
- **Batch Support**: Yes (batch requests supported)

### Azure OpenAI
- **Models**: Same as OpenAI but hosted on Azure
- **API**: Azure-specific endpoints and authentication
- **Features**: Enterprise security and compliance, batch processing
- **Cost**: Azure pricing model
- **Batch Support**: Yes (Azure batch API)

### HuggingFace
- **Models**: Open-source models via Inference API
- **API**: REST API with model-specific endpoints
- **Features**: Free tier available, custom models
- **Cost**: Usage-based pricing
- **Batch Support**: Limited (depends on specific model)

## Prompt Engineering

### Prediction Extraction Prompt Template
```
Analyze the following article and extract any predictions or forecasts mentioned.

Title: {article_title}

Article Text:
{article_text}

Please identify and extract any predictions, forecasts, or future-oriented statements from this article.
For each prediction, provide:
1. The exact prediction text
2. The category/type of prediction (political, economic, sports, technology, social)
3. A confidence score (0.0 to 1.0) based on how certain the prediction seems
4. A quality rating (1-5 stars) based on specificity and credibility
5. The timeframe mentioned (if any)
6. The subject or topic of the prediction

Format your response as JSON with the following structure:
{
  "predictions": [
    {
      "text": "prediction text",
      "type": "category",
      "confidence": 0.85,
      "rating": 4,
      "timeframe": "timeframe",
      "subject": "subject",
      "context": "surrounding context"
    }
  ]
}
```

### Structured Output Schema
```json
{
  "type": "object",
  "properties": {
    "predictions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "text": {"type": "string"},
          "type": {"type": "string", "enum": ["political", "economic", "sports", "technology", "social"]},
          "confidence": {"type": "number", "minimum": 0.0, "maximum": 1.0},
          "rating": {"type": "integer", "minimum": 1, "maximum": 5},
          "timeframe": {"type": "string"},
          "subject": {"type": "string"},
          "context": {"type": "string"}
        },
        "required": ["text", "type", "confidence", "rating"]
      }
    }
  }
}
```

## Error Handling

### Exception Hierarchy
- `LLMException`: Base exception for all LLM-related errors
- Error codes: `CLIENT_UNAVAILABLE`, `NOT_IMPLEMENTED`, `INVALID_REQUEST`, `RATE_LIMITED`, `COST_EXCEEDED`
- Detailed error information with context

### Fallback Strategies
1. **Primary Failure**: Fall back to mock extractor if configured
2. **Rate Limiting**: Queue requests or return cached results
3. **Cost Limits**: Disable LLM temporarily, use fallback
4. **Network Issues**: Retry with exponential backoff

### Monitoring and Alerting
- Usage statistics tracking
- Error rate monitoring
- Cost tracking and budgeting
- Performance metrics collection

## Batch Processing

### Overview
Batch processing allows multiple articles to be analyzed in a single LLM request, significantly reducing API costs and improving efficiency. This is particularly beneficial for providers like OpenAI and Gemini that support native batch operations.

### Cost Benefits
- **Reduced API Calls**: Process 10 articles in 1 request instead of 10 separate requests
- **Lower Latency**: Single network round-trip for multiple articles
- **Better Rate Limiting**: More efficient use of rate limits
- **Bulk Discounts**: Some providers offer better pricing for batch requests

### Configuration
```properties
# Enable batch processing
prediction.llm.batch-mode=true

# Number of articles per batch (recommended: 5-15)
prediction.llm.batch-size=10

# Extended timeout for batch requests
prediction.llm.batch-timeout-seconds=60
```

### Batch Processing Flow
1. **Collection**: Articles are collected into batches based on `batch-size`
2. **Prompt Engineering**: Single prompt containing all articles with unique identifiers
3. **LLM Request**: One API call processes the entire batch
4. **Response Parsing**: Results are mapped back to individual articles
5. **Fallback**: Individual processing if batch fails

### Provider Support
- **OpenAI**: Native batch API with significant cost savings
- **Gemini**: Supports batch requests with competitive pricing
- **Azure OpenAI**: Batch API available with enterprise features
- **Anthropic**: No native batch support (falls back to individual requests)
- **HuggingFace**: Limited support depending on model

### Batch Prompt Template
```
Analyze the following articles and extract predictions from each:

=== ARTICLE 1 (ID: article_123) ===
Title: Economic Forecast 2024
Content: The economy is expected to grow by 3% next year...

=== ARTICLE 2 (ID: article_124) ===
Title: Tech Trends
Content: AI adoption will accelerate in 2024...

Format response as JSON mapping article IDs to predictions.
```

## Performance Considerations

### Caching Strategy
- **Response Caching**: Cache LLM responses for identical inputs
- **TTL Management**: Configurable cache expiration
- **Cache Keys**: Hash-based keys for content similarity
- **Storage**: Redis-based caching implementation

### Optimization Techniques
- **Batch Processing**: Group multiple articles for efficiency
- **Prompt Optimization**: Minimize token usage while maintaining quality
- **Model Selection**: Choose appropriate models for different use cases
- **Parallel Processing**: Async operations for better throughput

### Scalability
- **Connection Pooling**: Efficient HTTP client management
- **Load Balancing**: Multiple API keys for higher throughput
- **Circuit Breaker**: Prevent cascade failures
- **Graceful Degradation**: Fallback to simpler methods under load

## Testing Strategy

### Unit Tests
- Configuration validation
- Extractor factory logic
- Error handling scenarios
- Mock implementations

### Integration Tests
- LLM client operations (when implemented)
- End-to-end prediction extraction
- Fallback mechanism testing
- Performance benchmarking

### Load Testing
- Rate limiting validation
- Cost control verification
- Concurrent request handling
- Failure recovery testing

## Migration Path

### From Mock to LLM
1. **Configuration**: Update properties to enable LLM
2. **API Keys**: Configure provider credentials
3. **Testing**: Validate with small dataset
4. **Gradual Rollout**: Percentage-based traffic routing
5. **Monitoring**: Track quality and performance metrics

### Rollback Strategy
- **Immediate**: Switch back to mock extractor
- **Configuration**: Disable LLM via properties
- **Fallback**: Automatic fallback on failures
- **Data Integrity**: No data loss during transitions

## Future Enhancements

### Advanced Features
- **Multi-model Ensemble**: Combine predictions from multiple models
- **Active Learning**: Improve predictions based on feedback
- **Custom Fine-tuning**: Domain-specific model training
- **Real-time Streaming**: Process articles as they arrive

### Integration Opportunities
- **Vector Databases**: Store and search embeddings
- **Knowledge Graphs**: Enhance predictions with structured knowledge
- **Feedback Loops**: Learn from user corrections
- **Analytics Dashboard**: Visualize prediction quality and trends

## Conclusion

The LLM integration architecture provides a robust foundation for incorporating advanced AI capabilities into the prediction analysis system. The current implementation includes all necessary infrastructure components and can be enhanced with actual LLM functionality by uncommenting dependencies and implementing the placeholder methods.

The design prioritizes security, cost control, and reliability while maintaining flexibility for future enhancements and provider changes. The fallback mechanisms ensure system stability even when LLM services are unavailable or misconfigured.