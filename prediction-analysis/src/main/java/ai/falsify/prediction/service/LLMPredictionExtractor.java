package ai.falsify.prediction.service;

import ai.falsify.prediction.model.PredictionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LLM-based implementation of PredictionExtractor using LangChain4j.
 * This implementation provides a foundation for future integration with
 * Large Language Models for intelligent prediction extraction.
 * 
 * Supports both single article processing and batch processing for cost optimization.
 * Currently serves as a placeholder with configuration infrastructure
 * and will be enhanced with actual LLM integration in future iterations.
 */
@ApplicationScoped
public class LLMPredictionExtractor implements BatchPredictionExtractor {
    
    private static final Logger LOG = Logger.getLogger(LLMPredictionExtractor.class);
    
    @ConfigProperty(name = "prediction.llm.enabled", defaultValue = "false")
    boolean llmEnabled;
    
    @ConfigProperty(name = "prediction.llm.provider", defaultValue = "openai")
    String llmProvider;
    
    @ConfigProperty(name = "prediction.llm.model", defaultValue = "gpt-3.5-turbo")
    String llmModel;
    
    @ConfigProperty(name = "prediction.llm.api-key")
    Optional<String> apiKey;
    
    @ConfigProperty(name = "prediction.llm.api-url")
    Optional<String> apiUrl;
    
    @ConfigProperty(name = "prediction.llm.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;
    
    @ConfigProperty(name = "prediction.llm.max-tokens", defaultValue = "1000")
    int maxTokens;
    
    @ConfigProperty(name = "prediction.llm.temperature", defaultValue = "0.3")
    double temperature;
    
    @ConfigProperty(name = "prediction.llm.retry-attempts", defaultValue = "3")
    int retryAttempts;
    
    @ConfigProperty(name = "prediction.llm.rate-limit-per-minute", defaultValue = "60")
    int rateLimitPerMinute;
    
    @ConfigProperty(name = "prediction.llm.fallback-to-mock", defaultValue = "true")
    boolean fallbackToMock;
    
    @ConfigProperty(name = "prediction.llm.batch-mode", defaultValue = "false")
    boolean batchMode;
    
    @ConfigProperty(name = "prediction.llm.max-batch-size", defaultValue = "10")
    int maxBatchSize;
    
    @ConfigProperty(name = "prediction.llm.batch-timeout-seconds", defaultValue = "60")
    int batchTimeoutSeconds;
    
    @Inject
    MockPredictionExtractor mockExtractor;
    
    // Placeholder for future LangChain4j client injection
    // @Inject
    // LangChain4jClient langChainClient;
    
    // Rate limiting state
    private final Queue<Long> requestTimestamps = new LinkedList<>();
    private final Object rateLimitLock = new Object();
    
    @Override
    public List<PredictionResult> extractPredictions(String articleText, String articleTitle) {
        if (!llmEnabled) {
            LOG.debug("LLM prediction extraction is disabled");
            if (fallbackToMock && mockExtractor.isAvailable()) {
                LOG.debug("Falling back to mock prediction extraction");
                return mockExtractor.extractPredictions(articleText, articleTitle);
            }
            return Collections.emptyList();
        }
        
        if (!isConfigurationValid()) {
            LOG.warn("LLM configuration is invalid, cannot extract predictions");
            if (fallbackToMock && mockExtractor.isAvailable()) {
                LOG.debug("Falling back to mock prediction extraction due to invalid configuration");
                return mockExtractor.extractPredictions(articleText, articleTitle);
            }
            return Collections.emptyList();
        }
        
        if (articleText == null || articleText.trim().isEmpty()) {
            LOG.debug("Article text is empty, no predictions to extract");
            return Collections.emptyList();
        }
        
        String titlePreview = articleTitle != null ? articleTitle.substring(0, Math.min(50, articleTitle.length())) : "Untitled";
        LOG.info("Starting LLM prediction extraction for article: " + titlePreview);
        
        try {
            // Check rate limiting
            if (!checkRateLimit()) {
                LOG.warn("Rate limit exceeded, cannot process request");
                if (fallbackToMock && mockExtractor.isAvailable()) {
                    LOG.debug("Falling back to mock prediction extraction due to rate limit");
                    return mockExtractor.extractPredictions(articleText, articleTitle);
                }
                return Collections.emptyList();
            }
            
            // TODO: Implement actual LLM integration
            // For now, this is a placeholder that will be enhanced with LangChain4j
            return extractPredictionsWithLLM(articleText, articleTitle);
            
        } catch (Exception e) {
            LOG.error("Failed to extract predictions using LLM", e);
            if (fallbackToMock && mockExtractor.isAvailable()) {
                LOG.debug("Falling back to mock prediction extraction due to error");
                return mockExtractor.extractPredictions(articleText, articleTitle);
            }
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean isAvailable() {
        return llmEnabled && isConfigurationValid();
    }
    
    @Override
    public String getExtractorType() {
        return "llm";
    }
    
    @Override
    public String getConfiguration() {
        return String.format("Type: %s, Provider: %s, Model: %s, Enabled: %s, Available: %s, Fallback: %s",
                getExtractorType(), llmProvider, llmModel, llmEnabled, isAvailable(), fallbackToMock);
    }
    
    /**
     * Validates the LLM configuration.
     * 
     * @return true if configuration is valid for LLM operations
     */
    private boolean isConfigurationValid() {
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            LOG.debug("LLM API key is not configured");
            return false;
        }
        
        if (llmProvider == null || llmProvider.trim().isEmpty()) {
            LOG.debug("LLM provider is not configured");
            return false;
        }
        
        if (llmModel == null || llmModel.trim().isEmpty()) {
            LOG.debug("LLM model is not configured");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the current request is within rate limits.
     * 
     * @return true if request can proceed, false if rate limited
     */
    private boolean checkRateLimit() {
        synchronized (rateLimitLock) {
            long currentTime = System.currentTimeMillis();
            long oneMinuteAgo = currentTime - TimeUnit.MINUTES.toMillis(1);
            
            // Remove timestamps older than 1 minute
            while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < oneMinuteAgo) {
                requestTimestamps.poll();
            }
            
            // Check if we're within the rate limit
            if (requestTimestamps.size() >= rateLimitPerMinute) {
                return false;
            }
            
            // Add current request timestamp
            requestTimestamps.offer(currentTime);
            return true;
        }
    }
    
    /**
     * Placeholder method for actual LLM-based prediction extraction.
     * This will be implemented with LangChain4j integration in future iterations.
     * 
     * @param articleText the article text to analyze
     * @param articleTitle the article title
     * @return list of extracted predictions
     */
    private List<PredictionResult> extractPredictionsWithLLM(String articleText, String articleTitle) {
        LOG.info("LLM prediction extraction is not yet implemented - this is a placeholder");
        
        // TODO: Implement actual LLM integration using LangChain4j
        // The implementation will include:
        // 1. Prompt engineering for prediction extraction
        // 2. LLM API calls with proper error handling
        // 3. Response parsing and validation
        // 4. Confidence score calculation
        // 5. Retry logic with exponential backoff
        
        // For now, return empty list as this is infrastructure preparation
        return Collections.emptyList();
        
        /* Future implementation outline:
        
        try {
            // 1. Prepare the prompt for prediction extraction
            String prompt = buildPredictionExtractionPrompt(articleText, articleTitle);
            
            // 2. Create LLM request with configuration
            LLMRequest request = LLMRequest.builder()
                .model(llmModel)
                .prompt(prompt)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
            
            // 3. Execute LLM request with retry logic
            CompletableFuture<LLMResponse> future = langChainClient.generateAsync(request);
            LLMResponse response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // 4. Parse and validate the response
            List<PredictionResult> predictions = parseLLMResponse(response, articleText);
            
            // 5. Apply confidence scoring and filtering
            return filterAndScorePredictions(predictions);
            
        } catch (TimeoutException e) {
            LOG.warn("LLM request timed out after " + timeoutSeconds + " seconds");
            throw new LLMExtractionException("Request timeout", e);
        } catch (Exception e) {
            LOG.error("LLM prediction extraction failed", e);
            throw new LLMExtractionException("Extraction failed", e);
        }
        
        */
    }
    
    /**
     * Gets the current rate limit status.
     * 
     * @return map containing rate limit information
     */
    public Map<String, Object> getRateLimitStatus() {
        synchronized (rateLimitLock) {
            long currentTime = System.currentTimeMillis();
            long oneMinuteAgo = currentTime - TimeUnit.MINUTES.toMillis(1);
            
            // Count recent requests
            long recentRequests = requestTimestamps.stream()
                .filter(timestamp -> timestamp >= oneMinuteAgo)
                .count();
            
            Map<String, Object> status = new HashMap<>();
            status.put("rateLimitPerMinute", rateLimitPerMinute);
            status.put("recentRequests", recentRequests);
            status.put("remainingRequests", Math.max(0, rateLimitPerMinute - recentRequests));
            status.put("resetTime", oneMinuteAgo + TimeUnit.MINUTES.toMillis(1));
            
            return status;
        }
    }
    
    @Override
    public Map<String, List<PredictionResult>> extractPredictionsBatch(Map<String, ArticleData> articles) {
        if (!llmEnabled) {
            LOG.debug("LLM prediction extraction is disabled, falling back to individual processing");
            return extractPredictionsIndividually(articles);
        }
        
        if (!isConfigurationValid()) {
            LOG.warn("LLM configuration is invalid, cannot extract predictions in batch");
            if (fallbackToMock && mockExtractor.isAvailable()) {
                LOG.debug("Falling back to mock prediction extraction for batch");
                return extractPredictionsWithMockBatch(articles);
            }
            return Collections.emptyMap();
        }
        
        if (articles == null || articles.isEmpty()) {
            LOG.debug("No articles provided for batch processing");
            return Collections.emptyMap();
        }
        
        // Filter valid articles
        Map<String, ArticleData> validArticles = articles.entrySet().stream()
            .filter(entry -> entry.getValue().isValid())
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        
        if (validArticles.isEmpty()) {
            LOG.debug("No valid articles found for batch processing");
            return Collections.emptyMap();
        }
        
        LOG.infof("Starting LLM batch prediction extraction for %d articles", validArticles.size());
        
        // Process articles in batches up to maxBatchSize
        Map<String, List<PredictionResult>> allResults = new HashMap<>();
        List<Map.Entry<String, ArticleData>> articleList = new ArrayList<>(validArticles.entrySet());
        
        for (int i = 0; i < articleList.size(); i += maxBatchSize) {
            int endIndex = Math.min(i + maxBatchSize, articleList.size());
            Map<String, ArticleData> batch = new HashMap<>();
            
            for (int j = i; j < endIndex; j++) {
                Map.Entry<String, ArticleData> entry = articleList.get(j);
                batch.put(entry.getKey(), entry.getValue());
            }
            
            LOG.infof("Processing batch %d/%d with %d articles", 
                     (i / maxBatchSize) + 1, 
                     (articleList.size() + maxBatchSize - 1) / maxBatchSize, 
                     batch.size());

            try {
                // Check rate limiting for batch request
                if (!checkRateLimit()) {
                    LOG.warn("Rate limit exceeded, cannot process batch request");
                    if (fallbackToMock && mockExtractor.isAvailable()) {
                        LOG.debug("Falling back to mock prediction extraction for batch due to rate limit");
                        Map<String, List<PredictionResult>> batchResults = extractPredictionsWithMockBatch(batch);
                        allResults.putAll(batchResults);
                    }
                    continue;
                }

                // Process batch with LLM or fallback
                Map<String, List<PredictionResult>> batchResults;
                if (maxBatchSize == 1 || !batchMode) {
                    // Use sequential calls for single article processing
                    batchResults = extractPredictionsIndividually(batch);
                } else {
                    // Use batch processing
                    batchResults = extractPredictionsBatchWithLLM(batch);
                }
                
                allResults.putAll(batchResults);

            } catch (Exception e) {
                LOG.error("Failed to extract predictions for batch, falling back to mock", e);
                if (fallbackToMock && mockExtractor.isAvailable()) {
                    Map<String, List<PredictionResult>> batchResults = extractPredictionsWithMockBatch(batch);
                    allResults.putAll(batchResults);
                }
            }
        }

        return allResults;
    }
    
    @Override
    public boolean isBatchModeEnabled() {
        return llmEnabled && isConfigurationValid() && maxBatchSize > 1;
    }
    
    @Override
    public int getMaxBatchSize() {
        return Math.min(maxBatchSize, 50); // Cap at 50 for safety
    }
    
    /**
     * Extracts predictions from multiple articles individually (fallback method).
     * 
     * @param articles map of article ID to ArticleData
     * @return map of article ID to prediction results
     */
    private Map<String, List<PredictionResult>> extractPredictionsIndividually(Map<String, ArticleData> articles) {
        Map<String, List<PredictionResult>> results = new HashMap<>();
        
        for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
            String articleId = entry.getKey();
            ArticleData article = entry.getValue();
            
            try {
                List<PredictionResult> predictions = extractPredictions(article.text(), article.title());
                results.put(articleId, predictions);
            } catch (Exception e) {
                LOG.warnf("Failed to extract predictions for article %s: %s", articleId, e.getMessage());
                results.put(articleId, Collections.emptyList());
            }
        }
        
        return results;
    }
    
    /**
     * Extracts predictions using mock extractor for batch processing.
     * 
     * @param articles map of article ID to ArticleData
     * @return map of article ID to prediction results
     */
    private Map<String, List<PredictionResult>> extractPredictionsWithMockBatch(Map<String, ArticleData> articles) {
        Map<String, List<PredictionResult>> results = new HashMap<>();
        
        for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
            String articleId = entry.getKey();
            ArticleData article = entry.getValue();
            
            try {
                List<PredictionResult> predictions = mockExtractor.extractPredictions(article.text(), article.title());
                results.put(articleId, predictions);
            } catch (Exception e) {
                LOG.warnf("Failed to extract mock predictions for article %s: %s", articleId, e.getMessage());
                results.put(articleId, Collections.emptyList());
            }
        }
        
        return results;
    }
    
    /**
     * Placeholder method for actual LLM-based batch prediction extraction.
     * This will be implemented with LangChain4j integration in future iterations.
     * 
     * @param articles map of article ID to ArticleData
     * @return map of article ID to prediction results
     */
    private Map<String, List<PredictionResult>> extractPredictionsBatchWithLLM(Map<String, ArticleData> articles) {
        LOG.info("LLM batch prediction extraction is not yet implemented - this is a placeholder");
        
        // TODO: Implement actual LLM batch integration using LangChain4j
        // The implementation will include:
        // 1. Build batch prompt combining multiple articles
        // 2. Send single LLM request with all articles
        // 3. Parse response and map predictions back to articles
        // 4. Handle partial failures and retry logic
        
        // For now, return empty results as this is infrastructure preparation
        return Collections.emptyMap();
        
        /* Future implementation outline:
        
        try {
            // 1. Build batch prompt
            String batchPrompt = buildBatchPredictionExtractionPrompt(articles);
            
            // 2. Create LLM request with extended timeout for batch processing
            LLMRequest request = LLMRequest.builder()
                .model(llmModel)
                .prompt(batchPrompt)
                .maxTokens(maxTokens * articles.size()) // Scale tokens for batch
                .temperature(temperature)
                .timeout(Duration.ofSeconds(batchTimeoutSeconds))
                .build();
            
            // 3. Execute batch LLM request
            CompletableFuture<LLMResponse> future = langChainClient.generateAsync(request);
            LLMResponse response = future.get(batchTimeoutSeconds, TimeUnit.SECONDS);
            
            // 4. Parse batch response and map to articles
            return parseBatchLLMResponse(response, articles);
            
        } catch (TimeoutException e) {
            LOG.warn("LLM batch request timed out after {} seconds", batchTimeoutSeconds);
            throw new LLMExtractionException("Batch request timeout", e);
        } catch (Exception e) {
            LOG.error("LLM batch prediction extraction failed", e);
            throw new LLMExtractionException("Batch extraction failed", e);
        }
        
        */
    }
    
    /**
     * Gets detailed configuration information for debugging.
     * 
     * @return configuration details (sensitive information masked)
     */
    public Map<String, Object> getDetailedConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", llmEnabled);
        config.put("provider", llmProvider);
        config.put("model", llmModel);
        config.put("hasApiKey", apiKey.isPresent() && !apiKey.get().trim().isEmpty());
        config.put("apiKeyLength", apiKey.map(key -> key.length()).orElse(0));
        config.put("apiUrl", apiUrl.orElse("default"));
        config.put("timeoutSeconds", timeoutSeconds);
        config.put("maxTokens", maxTokens);
        config.put("temperature", temperature);
        config.put("retryAttempts", retryAttempts);
        config.put("rateLimitPerMinute", rateLimitPerMinute);
        config.put("fallbackToMock", fallbackToMock);
        config.put("batchMode", batchMode);
        config.put("maxBatchSize", maxBatchSize);
        config.put("batchTimeoutSeconds", batchTimeoutSeconds);
        config.put("configurationValid", isConfigurationValid());
        config.put("available", isAvailable());
        
        return config;
    }
}