package ai.falsify.prediction.service;

import ai.falsify.prediction.config.LLMConfiguration;
import ai.falsify.prediction.model.PredictionResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gemini-based implementation of PredictionExtractor using LangChain4j.
 * This implementation provides real LLM integration with Google's Gemini model
 * for intelligent prediction extraction from articles.
 */
@ApplicationScoped
public class GeminiPredictionExtractor implements BatchPredictionExtractor {

    private static final Logger LOG = Logger.getLogger(GeminiPredictionExtractor.class);

    @Inject
    LLMConfiguration config;

    @ConfigProperty(name = "prediction.llm.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "prediction.llm.enabled", defaultValue = "false")
    boolean directEnabled;

    @ConfigProperty(name = "prediction.llm.provider", defaultValue = "gemini")
    String directProvider;

    @ConfigProperty(name = "prediction.llm.model", defaultValue = "gemini-1.5-flash")
    String directModel;

    @ConfigProperty(name = "prediction.llm.max-batch-size", defaultValue = "10")
    int directMaxBatchSize;

    @ConfigProperty(name = "prediction.llm.max-tokens", defaultValue = "1000")
    int directMaxTokens;

    @ConfigProperty(name = "prediction.llm.temperature", defaultValue = "0.3")
    double directTemperature;

    @ConfigProperty(name = "prediction.llm.async-timeout-seconds", defaultValue = "120")
    int asyncTimeoutSeconds;

    private ChatLanguageModel chatModel;

    @jakarta.annotation.PostConstruct
    void init() {
        LOG.infof("GeminiPredictionExtractor initialized with config: enabled=%s, provider=%s, model=%s, apiKey=%s",
                config.enabled, config.provider, config.model, apiKey.isPresent() ? "***set***" : "not set");
        LOG.infof(
                "Direct config properties: enabled=%s, provider=%s, model=%s, maxTokens=%d, temperature=%f, maxBatchSize=%d, asyncTimeout=%ds",
                directEnabled, directProvider, directModel, directMaxTokens, directTemperature, directMaxBatchSize,
                asyncTimeoutSeconds);
        LOG.infof("LLMConfiguration object: %s", config);
        LOG.infof("Config validation result: %s", config.isValid());
    }

    /**
     * Initialize the Gemini chat model lazily.
     */
    private ChatLanguageModel getChatModel() {
        if (chatModel == null && directEnabled && apiKey.isPresent()) {
            try {
                LOG.debugf("Initializing Gemini chat model with: model=%s, maxTokens=%d, temperature=%f",
                        directModel, directMaxTokens, directTemperature);

                chatModel = GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKey.get())
                        .modelName(directModel)
                        .temperature(directTemperature)
                        .maxOutputTokens(directMaxTokens)
                        .logRequestsAndResponses(true)
                        .build();

                LOG.infof("Successfully initialized Gemini chat model: %s with maxTokens=%d", directModel,
                        directMaxTokens);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to initialize Gemini chat model");
                chatModel = null;
            }
        }
        return chatModel;
    }

    @Override
    public List<PredictionResult> extractPredictions(String articleText, String articleTitle) {
        if (!isAvailable()) {
            LOG.debug("Gemini prediction extractor is not available");
            return Collections.emptyList();
        }

        if (articleText == null || articleText.trim().isEmpty()) {
            LOG.debug("Article text is empty, no predictions to extract");
            return Collections.emptyList();
        }

        String titlePreview = articleTitle != null ? articleTitle.substring(0, Math.min(50, articleTitle.length()))
                : "Untitled";
        LOG.infof("Starting Gemini prediction extraction for article: %s", titlePreview);
        LOG.debugf("Article text length: %d characters", articleText.length());

        try {
            String prompt = buildPredictionExtractionPrompt(articleText, articleTitle);
            LOG.debugf("Generated prompt length: %d characters", prompt.length());
            LOG.debugf("Prompt preview (first 200 chars): %s", prompt.substring(0, Math.min(200, prompt.length())));

            ChatLanguageModel model = getChatModel();
            if (model == null) {
                LOG.warn("Chat model is not available");
                return Collections.emptyList();
            }

            LOG.debug("Sending request to Gemini API...");
            LOG.debugf("Full prompt being sent to Gemini:\n%s", prompt);
            long startTime = System.currentTimeMillis();

            String responseText = model.generate(prompt);

            long endTime = System.currentTimeMillis();
            LOG.infof("Gemini API call completed in %d ms", (endTime - startTime));

            if (responseText == null) {
                LOG.warn("Gemini API returned null response");
                return Collections.emptyList();
            }

            LOG.debugf("Received Gemini response (%d chars) for article: %s",
                    responseText.length(), titlePreview);
            LOG.debugf("Full Gemini response: %s", responseText);

            List<PredictionResult> results = parsePredictionResponse(responseText, articleText);
            LOG.infof("Extraction completed: found %d predictions for article: %s", results.size(), titlePreview);

            return results;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to extract predictions using Gemini for article: %s", titlePreview);
            LOG.errorf("Exception details: %s", e.getMessage());
            if (e.getCause() != null) {
                LOG.errorf("Root cause: %s", e.getCause().getMessage());
            }
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, List<PredictionResult>> extractPredictionsBatch(Map<String, ArticleData> articles) {
        if (!isAvailable()) {
            LOG.debug("Gemini prediction extractor is not available for batch processing");
            return Collections.emptyMap();
        }

        if (articles == null || articles.isEmpty()) {
            LOG.debug("No articles provided for batch processing");
            return Collections.emptyMap();
        }

        LOG.infof("Starting Gemini batch prediction extraction for %d articles", articles.size());

        // Process articles in batches up to maxBatchSize
        Map<String, List<PredictionResult>> allResults = new HashMap<>();
        List<Map.Entry<String, ArticleData>> articleList = new ArrayList<>(articles.entrySet());

        for (int i = 0; i < articleList.size(); i += directMaxBatchSize) {
            int endIndex = Math.min(i + directMaxBatchSize, articleList.size());
            Map<String, ArticleData> batch = new HashMap<>();

            for (int j = i; j < endIndex; j++) {
                Map.Entry<String, ArticleData> entry = articleList.get(j);
                batch.put(entry.getKey(), entry.getValue());
            }

            LOG.infof("Processing Gemini batch %d/%d with %d articles",
                    (i / directMaxBatchSize) + 1,
                    (articleList.size() + directMaxBatchSize - 1) / directMaxBatchSize,
                    batch.size());

            try {
                Map<String, List<PredictionResult>> batchResults;

                if (directMaxBatchSize == 1) {
                    // Use sequential calls for single article processing
                    batchResults = extractPredictionsSequentially(batch);
                } else {
                    // Use batch processing
                    batchResults = extractPredictionsBatchInternal(batch);
                }

                allResults.putAll(batchResults);

            } catch (Exception e) {
                LOG.errorf(e, "Failed to process Gemini batch, skipping %d articles", batch.size());
                // Continue with next batch
            }
        }

        return allResults;
    }

    /**
     * Extract predictions from multiple articles sequentially.
     * Used when maxBatchSize is 1 or batch processing is not desired.
     */
    private Map<String, List<PredictionResult>> extractPredictionsSequentially(Map<String, ArticleData> articles) {
        Map<String, List<PredictionResult>> results = new HashMap<>();

        for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
            String articleId = entry.getKey();
            ArticleData article = entry.getValue();

            try {
                List<PredictionResult> predictions = extractPredictions(article.text(), article.title());
                results.put(articleId, predictions);

                // Add small delay between requests to respect rate limits
                Thread.sleep(100);

            } catch (Exception e) {
                LOG.warnf("Failed to extract predictions for article %s: %s", articleId, e.getMessage());
                results.put(articleId, Collections.emptyList());
            }
        }

        return results;
    }

    /**
     * Extract predictions from multiple articles in a single batch request.
     */
    private Map<String, List<PredictionResult>> extractPredictionsBatchInternal(Map<String, ArticleData> articles) {
        try {
            // Validate API key before making the call
            if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
                LOG.error("Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable.");
                return Collections.emptyMap();
            }

            String key = apiKey.get().trim();
            if (!key.startsWith("AIza") || key.length() <= 30) {
                LOG.errorf(
                        "Invalid Gemini API key format. Expected format: AIza... with length > 30, got: %s... (length: %d)",
                        key.substring(0, Math.min(4, key.length())), key.length());
                return Collections.emptyMap();
            }

            String batchPrompt = buildBatchPredictionExtractionPrompt(articles);

            ChatLanguageModel model = getChatModel();
            if (model == null) {
                LOG.warn("Chat model is not available for batch processing");
                return Collections.emptyMap();
            }

            LOG.debugf("Making Gemini API call with prompt length: %d characters", batchPrompt.length());

            // Check if prompt is too long (Gemini has token limits)
            if (batchPrompt.length() > 30000) {
                LOG.warnf(
                        "Prompt is very long (%d chars). This might cause MAX_TOKENS issues. Consider reducing batch size.",
                        batchPrompt.length());
                LOG.warnf("Current batch size: %d articles, maxOutputTokens: %d", articles.size(), directMaxTokens);
            }

            // Log a sample of the prompt for debugging
            LOG.debugf("Prompt sample (first 500 chars): %s",
                    batchPrompt.substring(0, Math.min(500, batchPrompt.length())));

            String responseText;
            try {
                LOG.debugf("Starting async Gemini API call...");

                // Use CompletableFuture for async call with timeout
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return model.generate(batchPrompt);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });

                // Wait for response with configurable timeout
                responseText = future.get(asyncTimeoutSeconds, TimeUnit.SECONDS);

                LOG.debugf("Async Gemini API call completed successfully");

            } catch (TimeoutException e) {
                LOG.errorf("Gemini API call timed out after %d seconds. Consider reducing batch size or prompt length.",
                        asyncTimeoutSeconds);
                throw new RuntimeException("Gemini API call timed out", e);

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof CompletionException) {
                    cause = cause.getCause();
                }

                LOG.errorf(cause, "Async Gemini API call failed");

                // Check if it's the specific null parts issue (usually caused by MAX_TOKENS)
                if (cause.getMessage() != null && cause.getMessage().contains("parts")
                        && cause.getMessage().contains("null")) {
                    LOG.error(
                            "DETECTED: LangChain4j 'null parts' issue - this usually happens when Gemini hits the token limit (MAX_TOKENS).");
                    LOG.error("Current maxOutputTokens: " + directMaxTokens + ". Consider increasing this value.");
                    LOG.error(
                            "The Gemini API returned a response without 'parts' field, which LangChain4j cannot parse.");
                    LOG.error(
                            "Solutions: 1) Increase maxOutputTokens, 2) Reduce prompt size, 3) Implement direct HTTP calls");
                }

                throw new RuntimeException("Async Gemini API call failed", cause);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.errorf("Gemini API call was interrupted");
                throw new RuntimeException("Gemini API call was interrupted", e);

            } catch (Exception apiException) {
                LOG.errorf(apiException,
                        "Direct API call failed, this might be a LangChain4j compatibility issue with Gemini API");

                // Check if it's the specific null parts issue (usually caused by MAX_TOKENS)
                if (apiException.getMessage() != null && apiException.getMessage().contains("parts")
                        && apiException.getMessage().contains("null")) {
                    LOG.error(
                            "DETECTED: LangChain4j 'null parts' issue - this usually happens when Gemini hits the token limit (MAX_TOKENS).");
                    LOG.error("Current maxOutputTokens: " + directMaxTokens + ". Consider increasing this value.");
                    LOG.error(
                            "The Gemini API returned a response without 'parts' field, which LangChain4j cannot parse.");
                    LOG.error(
                            "Solutions: 1) Increase maxOutputTokens, 2) Reduce prompt size, 3) Implement direct HTTP calls");
                }

                throw apiException; // Re-throw to trigger fallback
            }

            if (responseText == null || responseText.trim().isEmpty()) {
                LOG.error(
                        "Gemini API returned null or empty response. This might indicate an API key issue or rate limiting.");
                return Collections.emptyMap();
            }

            LOG.debugf("Received Gemini batch response (%d chars) for %d articles",
                    responseText.length(), articles.size());

            return parseBatchPredictionResponse(responseText, articles);

        } catch (NullPointerException e) {
            LOG.errorf(e,
                    "CRITICAL: LangChain4j NullPointerException - 'parts' is null in Gemini API response. " +
                            "This is a known issue with LangChain4j and Gemini API compatibility. " +
                            "Solutions: 1) Update LangChain4j to latest version, 2) Implement direct HTTP calls to Gemini API");

            // For now, return empty results to prevent job failure
            return Collections.emptyMap();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to extract predictions using Gemini batch processing");

            // Return empty results to prevent job failure
            return Collections.emptyMap();
        }
    }

    /**
     * Extract predictions asynchronously with callback support.
     */
    public CompletableFuture<List<PredictionResult>> extractPredictionsAsync(String articleText, String articleTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractPredictions(articleText, articleTitle);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Extract predictions from multiple articles asynchronously.
     */
    public CompletableFuture<Map<String, List<PredictionResult>>> extractPredictionsBatchAsync(
            Map<String, ArticleData> articles) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractPredictionsBatch(articles);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        boolean available = directEnabled && apiKey.isPresent() && !apiKey.get().trim().isEmpty();
        LOG.debugf("GeminiPredictionExtractor availability check: enabled=%s, apiKeyPresent=%s, available=%s",
                directEnabled, apiKey.isPresent(), available);
        return available;
    }

    @Override
    public String getExtractorType() {
        return "gemini";
    }

    @Override
    public boolean isBatchModeEnabled() {
        return isAvailable() && directMaxBatchSize > 1;
    }

    @Override
    public int getMaxBatchSize() {
        return Math.min(directMaxBatchSize, 20); // Cap at 20 for Gemini
    }

    @Override
    public String getConfiguration() {
        return String.format(
                "Type: %s, Provider: %s, Model: %s, Enabled: %s, Available: %s, MaxBatchSize: %d, ApiKey: %s",
                getExtractorType(), directProvider, directModel, directEnabled, isAvailable(), getMaxBatchSize(),
                apiKey.isPresent() ? "***set***" : "not set");
    }

    /**
     * Build a prompt for prediction extraction from a single article.
     */
    private String buildPredictionExtractionPrompt(String articleText, String articleTitle) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert at analyzing news articles and identifying predictions or forecasts.\n\n");

        prompt.append(
                "Analyze the following article and extract any predictions, forecasts, or future-oriented statements.\n\n");

        if (articleTitle != null && !articleTitle.trim().isEmpty()) {
            prompt.append("Title: ").append(articleTitle).append("\n\n");
        }

        prompt.append("Article:\n").append(articleText).append("\n\n");

        prompt.append("For each prediction you find, provide:\n");
        prompt.append("1. The exact prediction text (quote from the article)\n");
        prompt.append("2. The type/category (political, economic, sports, technology, social, other)\n");
        prompt.append("3. A confidence score from 0.0 to 1.0 (how certain the prediction seems)\n");
        prompt.append("4. A quality rating from 1 to 5 stars (based on specificity and credibility)\n");
        prompt.append("5. Brief context (surrounding text that gives meaning to the prediction)\n\n");

        prompt.append("Format your response as:\n");
        prompt.append("PREDICTION: [exact quote]\n");
        prompt.append("TYPE: [category]\n");
        prompt.append("CONFIDENCE: [0.0-1.0]\n");
        prompt.append("RATING: [1-5]\n");
        prompt.append("CONTEXT: [brief context]\n");
        prompt.append("---\n\n");

        prompt.append("If no predictions are found, respond with: NO_PREDICTIONS_FOUND\n");

        return prompt.toString();
    }

    /**
     * Build a batch prompt for prediction extraction from multiple articles.
     */
    private String buildBatchPredictionExtractionPrompt(Map<String, ArticleData> articles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert at analyzing news articles and identifying predictions or forecasts.\n\n");

        prompt.append(
                "Analyze the following articles and extract any predictions, forecasts, or future-oriented statements from each.\n\n");

        // Add each article with a unique identifier
        for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
            String articleId = entry.getKey();
            ArticleData article = entry.getValue();

            prompt.append("=== ARTICLE ").append(articleId).append(" ===\n");

            if (article.title() != null && !article.title().trim().isEmpty()) {
                prompt.append("Title: ").append(article.title()).append("\n\n");
            }

            prompt.append("Content:\n").append(article.text()).append("\n\n");
        }

        prompt.append("For each article, provide predictions in this format:\n");
        prompt.append("ARTICLE_ID: [article_id]\n");
        prompt.append("PREDICTION: [exact quote]\n");
        prompt.append("TYPE: [category]\n");
        prompt.append("CONFIDENCE: [0.0-1.0]\n");
        prompt.append("RATING: [1-5]\n");
        prompt.append("CONTEXT: [brief context]\n");
        prompt.append("---\n\n");

        prompt.append("If an article has no predictions, write:\n");
        prompt.append("ARTICLE_ID: [article_id]\n");
        prompt.append("NO_PREDICTIONS_FOUND\n");
        prompt.append("---\n\n");

        return prompt.toString();
    }

    /**
     * Parse prediction response from Gemini for a single article.
     */
    private List<PredictionResult> parsePredictionResponse(String response, String originalText) {
        LOG.debugf("Starting to parse Gemini response: %s", response != null ? "present" : "null");

        if (response == null || response.trim().isEmpty()) {
            LOG.warn("Gemini response is null or empty");
            return Collections.emptyList();
        }

        LOG.debugf("Response length: %d characters", response.length());
        LOG.debugf("Response contains NO_PREDICTIONS_FOUND: %s", response.contains("NO_PREDICTIONS_FOUND"));

        if (response.contains("NO_PREDICTIONS_FOUND")) {
            LOG.debug("No predictions found in Gemini response");
            return Collections.emptyList();
        }

        List<PredictionResult> predictions = new ArrayList<>();

        // Split response by prediction separators
        String[] predictionBlocks = response.split("---");
        LOG.debugf("Split response into %d blocks", predictionBlocks.length);

        for (int i = 0; i < predictionBlocks.length; i++) {
            String block = predictionBlocks[i];
            LOG.debugf("Processing block %d: '%s'", i, block.trim().substring(0, Math.min(100, block.trim().length())));

            if (block.trim().isEmpty()) {
                LOG.debugf("Block %d is empty, skipping", i);
                continue;
            }

            try {
                PredictionResult prediction = parseSinglePrediction(block.trim());
                if (prediction != null) {
                    predictions.add(prediction);
                    LOG.debugf("Successfully parsed prediction %d: %s", predictions.size(),
                            prediction.predictionText());
                } else {
                    LOG.debugf("Block %d did not produce a valid prediction", i);
                }
            } catch (Exception e) {
                LOG.warnf("Failed to parse prediction block %d: %s", i, e.getMessage());
                LOG.debugf("Block content: %s", block.trim());
            }
        }

        LOG.infof("Parsed %d predictions from Gemini response", predictions.size());
        return predictions;
    }

    /**
     * Parse batch prediction response from Gemini.
     */
    private Map<String, List<PredictionResult>> parseBatchPredictionResponse(String response,
            Map<String, ArticleData> articles) {
        Map<String, List<PredictionResult>> results = new HashMap<>();

        // Initialize all articles with empty lists
        for (String articleId : articles.keySet()) {
            results.put(articleId, new ArrayList<>());
        }

        if (response == null || response.trim().isEmpty()) {
            return results;
        }

        // Split response by prediction separators
        String[] predictionBlocks = response.split("---");

        for (String block : predictionBlocks) {
            if (block.trim().isEmpty())
                continue;

            try {
                String articleId = extractArticleId(block);
                if (articleId != null && results.containsKey(articleId)) {
                    if (block.contains("NO_PREDICTIONS_FOUND")) {
                        // Article has no predictions - already initialized with empty list
                        continue;
                    }

                    PredictionResult prediction = parseSinglePrediction(block.trim());
                    if (prediction != null) {
                        results.get(articleId).add(prediction);
                    }
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse batch prediction block: %s", e.getMessage());
            }
        }

        int totalPredictions = results.values().stream().mapToInt(List::size).sum();
        LOG.infof("Parsed %d predictions from Gemini batch response for %d articles",
                totalPredictions, articles.size());

        return results;
    }

    /**
     * Extract article ID from a prediction block.
     */
    private String extractArticleId(String block) {
        Pattern pattern = Pattern.compile("ARTICLE_ID:\\s*(.+?)\\n", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Parse a single prediction from a text block.
     */
    private PredictionResult parseSinglePrediction(String block) {
        LOG.debugf("Parsing single prediction from block: %s", block.substring(0, Math.min(200, block.length())));

        try {
            String predictionText = extractField(block, "PREDICTION");
            String type = extractField(block, "TYPE");
            String confidenceStr = extractField(block, "CONFIDENCE");
            String ratingStr = extractField(block, "RATING");
            String context = extractField(block, "CONTEXT");

            LOG.debugf("Extracted fields - PREDICTION: %s, TYPE: %s, CONFIDENCE: %s, RATING: %s, CONTEXT: %s",
                    predictionText, type, confidenceStr, ratingStr, context);

            if (predictionText == null || predictionText.trim().isEmpty()) {
                LOG.debugf("Prediction text is null or empty, skipping block");
                return null;
            }

            double confidence = 0.5; // Default
            try {
                if (confidenceStr != null && !confidenceStr.trim().isEmpty()) {
                    confidence = Double.parseDouble(confidenceStr.trim());
                    confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to 0-1
                    LOG.debugf("Parsed confidence: %f", confidence);
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse confidence '%s': %s", confidenceStr, e.getMessage());
            }

            int rating = 3; // Default
            try {
                if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                    rating = Integer.parseInt(ratingStr.trim());
                    rating = Math.max(1, Math.min(5, rating)); // Clamp to 1-5
                    LOG.debugf("Parsed rating: %d", rating);
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse rating '%s': %s", ratingStr, e.getMessage());
            }

            PredictionResult result = new PredictionResult(
                    predictionText.trim(),
                    type != null ? type.trim() : "other",
                    rating,
                    java.math.BigDecimal.valueOf(confidence),
                    context != null ? context.trim() : "",
                    null, // timeframe
                    null // subject
            );

            LOG.debugf("Successfully created PredictionResult: %s", result.predictionText());
            return result;

        } catch (Exception e) {
            LOG.errorf("Error parsing single prediction: %s", e.getMessage());
            LOG.debugf("Full block content: %s", block);
            return null;
        }
    }

    /**
     * Extract a field value from a prediction block.
     */
    private String extractField(String block, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+?)(?=\\n[A-Z_]+:|\\n---|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}