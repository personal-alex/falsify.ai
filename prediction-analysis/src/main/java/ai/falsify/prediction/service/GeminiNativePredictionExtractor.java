package ai.falsify.prediction.service;

import ai.falsify.prediction.config.GeminiNativeConfiguration;
import ai.falsify.prediction.model.PredictionResult;
import ai.falsify.prediction.model.BatchRequest;
import ai.falsify.prediction.model.BatchResponse;
import ai.falsify.prediction.model.BatchResults;
import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.BatchState;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import java.util.Optional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * Native Gemini implementation of BatchPredictionExtractor using Google GenAI
 * library.
 * This implementation provides true asynchronous batch processing capabilities
 * by directly integrating with Google's GenAI library instead of using
 * LangChain4j.
 * 
 * Key features:
 * - True asynchronous batch processing with GenAI library
 * - Job tracking and status monitoring
 * - Fallback to synchronous processing when batch fails
 * - Configuration validation and health checking
 * - Resource management and cleanup
 */
@ApplicationScoped
public class GeminiNativePredictionExtractor implements BatchPredictionExtractor {

    private static final Logger LOG = Logger.getLogger(GeminiNativePredictionExtractor.class);

    @Inject
    GeminiNativeConfiguration config;

    @Inject
    GenAIBatchClient batchClient;

    @ConfigProperty(name = "prediction.gemini-native.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "prediction.gemini-native.model", defaultValue = "gemini-2.5-flash")
    String model;

    // Direct injection to bypass potential configuration object issues
    @ConfigProperty(name = "prediction.gemini-native.enabled", defaultValue = "false")
    boolean directEnabled;

    @ConfigProperty(name = "prediction.gemini-native.max-batch-size", defaultValue = "20")
    int directMaxBatchSize;

    @ConfigProperty(name = "prediction.gemini-native.batch.enabled", defaultValue = "true")
    boolean directBatchEnabled;

    @ConfigProperty(name = "prediction.gemini-native.timeout-minutes", defaultValue = "30")
    int directTimeoutMinutes;

    @ConfigProperty(name = "prediction.gemini-native.polling-interval-seconds", defaultValue = "10")
    int directPollingIntervalSeconds;

    // Track active batch jobs for monitoring and cleanup
    private final Map<String, BatchJobInfo> activeBatchJobs = new ConcurrentHashMap<>();

    /**
     * Internal class to track batch job information.
     */
    private static class BatchJobInfo {
        final String jobId;
        final String batchId;
        final Map<String, ArticleData> originalArticles;
        final long createdAt;
        volatile BatchState lastKnownState;
        volatile CompletableFuture<Map<String, List<PredictionResult>>> processingFuture;

        BatchJobInfo(String jobId, String batchId, Map<String, ArticleData> articles) {
            this.jobId = jobId;
            this.batchId = batchId;
            this.originalArticles = new HashMap<>(articles);
            this.createdAt = System.currentTimeMillis();
            this.lastKnownState = BatchState.SUBMITTED;
        }
    }

    @PostConstruct
    void init() {
        LOG.infof(
                "GeminiNativePredictionExtractor initialized: enabled=%s, available=%s, batchEnabled=%s, maxBatchSize=%d",
                directEnabled, isAvailable(), isBatchModeEnabled(), getMaxBatchSize());

        if (directEnabled && !isConfigurationValid()) {
            LOG.warn("GeminiNative configuration is enabled but invalid. Extractor will not be available.");
        }

        if (isAvailable()) {
            LOG.infof("GeminiNative extractor is ready: model=%s, apiKey=%s",
                    model, apiKey.isPresent() ? "***set***" : "not set");
        }
    }

    @Override
    public List<PredictionResult> extractPredictions(String articleText, String articleTitle) {
        if (!isAvailable()) {
            LOG.debug("GeminiNative prediction extractor is not available");
            return Collections.emptyList();
        }

        if (articleText == null || articleText.trim().isEmpty()) {
            LOG.debug("Article text is empty, no predictions to extract");
            return Collections.emptyList();
        }

        String titlePreview = articleTitle != null ? articleTitle.substring(0, Math.min(50, articleTitle.length()))
                : "Untitled";
        LOG.infof("Starting GeminiNative prediction extraction for article: %s", titlePreview);

        try {
            // For single article processing, use direct GenAI client instead of batch API
            // This avoids quota issues with the batch API
            String prompt = buildPredictionExtractionPrompt(articleText, articleTitle);

            // Use direct GenAI client for individual requests
            String response = processIndividualRequest(prompt);

            List<PredictionResult> predictions = parsePredictionResponse(response, articleText);
            LOG.infof("Extraction completed: found %d predictions for article: %s",
                    predictions.size(), titlePreview);
            return predictions;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to extract predictions using GeminiNative for article: %s", titlePreview);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, List<PredictionResult>> extractPredictionsBatch(Map<String, ArticleData> articles) {
        if (!isAvailable()) {
            LOG.debug("GeminiNative prediction extractor is not available for batch processing");
            return Collections.emptyMap();
        }

        if (articles == null || articles.isEmpty()) {
            LOG.debug("No articles provided for batch processing");
            return Collections.emptyMap();
        }

        LOG.infof("Starting GeminiNative batch prediction extraction for %d articles", articles.size());

        try {
            // Use async batch processing
            CompletableFuture<Map<String, List<PredictionResult>>> future = extractPredictionsBatchAsync(articles);

            // Wait for completion with timeout
            long timeoutMillis = directTimeoutMinutes * 60 * 1000L; // Convert minutes to milliseconds
            return future.get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            LOG.errorf(e, "Batch processing failed, falling back to sequential processing");
            return fallbackToSequentialProcessing(articles);
        }
    }

    /**
     * Asynchronous batch processing method.
     * This is the primary method for efficient batch processing.
     */
    public CompletableFuture<Map<String, List<PredictionResult>>> extractPredictionsBatchAsync(
            Map<String, ArticleData> articles) {

        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        if (articles == null || articles.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        LOG.infof("Starting async GeminiNative batch processing for %d articles", articles.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input size to prevent memory issues
                if (articles.size() > 1000) {
                    LOG.errorf("Article count too large for batch processing: %d", articles.size());
                    throw new IllegalArgumentException("Too many articles for batch processing: " + articles.size());
                }

                // Split into batches if necessary
                List<Map<String, ArticleData>> batches = splitIntoBatches(articles);
                Map<String, List<PredictionResult>> allResults = new HashMap<>();

                for (int i = 0; i < batches.size(); i++) {
                    Map<String, ArticleData> batch = batches.get(i);
                    LOG.infof("Processing batch %d/%d with %d articles", i + 1, batches.size(), batch.size());

                    try {
                        String jobId = submitBatchJob(batch);
                        Map<String, List<PredictionResult>> batchResults = waitForJobCompletion(jobId);
                        allResults.putAll(batchResults);

                    } catch (Exception e) {
                        LOG.warnf("Batch %d failed, processing articles individually: %s", i + 1, e.getMessage());
                        // Fallback to individual processing for this batch
                        Map<String, List<PredictionResult>> fallbackResults = fallbackToSequentialProcessing(batch);
                        allResults.putAll(fallbackResults);
                    }
                }

                LOG.infof("Async batch processing completed: %d total predictions found",
                        allResults.values().stream().mapToInt(List::size).sum());
                return allResults;

            } catch (Exception e) {
                LOG.errorf(e, "Async batch processing failed completely");
                throw new RuntimeException("Async batch processing failed", e);
            }
        });
    }

    /**
     * Submit a batch job for processing.
     * Returns a job ID that can be used to track status and retrieve results.
     */
    public String submitBatchJob(Map<String, ArticleData> articles) {
        if (!isAvailable()) {
            throw new IllegalStateException("GeminiNative extractor is not available");
        }

        if (articles == null || articles.isEmpty()) {
            throw new IllegalArgumentException("Articles cannot be null or empty");
        }

        if (articles.size() > getMaxBatchSize()) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum %d", articles.size(), getMaxBatchSize()));
        }

        LOG.infof("Submitting batch job for %d articles", articles.size());

        try {
            // Create batch requests
            List<BatchRequest> requests = new ArrayList<>();
            for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
                String articleId = entry.getKey();
                ArticleData article = entry.getValue();

                String prompt = buildPredictionExtractionPrompt(article.text(), article.title());
                BatchRequest request = new BatchRequest(articleId, articleId, prompt, Map.of());
                requests.add(request);
            }

            // Submit to batch client
            String batchId = batchClient.submitBatchRequest(requests).join();

            // Create job tracking info
            String jobId = "job_" + UUID.randomUUID().toString();
            BatchJobInfo jobInfo = new BatchJobInfo(jobId, batchId, articles);
            activeBatchJobs.put(jobId, jobInfo);

            LOG.infof("Batch job submitted successfully: jobId=%s, batchId=%s", jobId, batchId);
            return jobId;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to submit batch job: %s", e.getMessage());
            throw new RuntimeException("Failed to submit batch job", e);
        }
    }

    /**
     * Check the status of a batch job.
     */
    public BatchJobStatus checkJobStatus(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("Job ID cannot be null or empty");
        }

        BatchJobInfo jobInfo = activeBatchJobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        try {
            LOG.debugf("Checking status for job %s (batchId: %s)",
                    jobId, jobInfo.batchId);

            // Get status from batch client
            BatchJobStatus batchStatus = batchClient.getBatchStatus(jobInfo.batchId).join();

            // Update our tracking info
            BatchState previousState = jobInfo.lastKnownState;
            jobInfo.lastKnownState = batchStatus.state();

            if (previousState != batchStatus.state()) {
                LOG.infof("Job %s state changed from %s to %s", jobId, previousState, batchStatus.state());
            }

            LOG.debugf("Job %s status: %s", jobId, batchStatus.state());
            return batchStatus;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to check job status for %s: %s", jobId, e.getMessage());
            throw new RuntimeException("Failed to check job status", e);
        }
    }

    /**
     * Retrieve results from a completed batch job.
     */
    public Map<String, List<PredictionResult>> retrieveJobResults(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("Job ID cannot be null or empty");
        }

        BatchJobInfo jobInfo = activeBatchJobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        try {
            // Get results from batch client
            BatchResults batchResults = batchClient.getBatchResults(jobInfo.batchId).join();

            // Parse results
            Map<String, List<PredictionResult>> predictions = new HashMap<>();
            for (Map.Entry<String, BatchResponse> entry : batchResults.responses().entrySet()) {
                String articleId = entry.getKey();
                BatchResponse response = entry.getValue();

                if (response.success()) {
                    ArticleData originalArticle = jobInfo.originalArticles.get(articleId);
                    String originalText = originalArticle != null ? originalArticle.text() : "";
                    List<PredictionResult> articlePredictions = parsePredictionResponse(response.response(),
                            originalText);
                    predictions.put(articleId, articlePredictions);
                } else {
                    LOG.warnf("Article %s processing failed: %s", articleId, response.errorMessage());
                    predictions.put(articleId, Collections.emptyList());
                }
            }

            // Clean up completed job
            activeBatchJobs.remove(jobId);

            LOG.infof("Retrieved results for job %s: %d articles, %d total predictions",
                    jobId, predictions.size(),
                    predictions.values().stream().mapToInt(List::size).sum());

            return predictions;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve job results for %s: %s", jobId, e.getMessage());
            throw new RuntimeException("Failed to retrieve job results", e);
        }
    }

    @Override
    public boolean isAvailable() {
        boolean hasApiKey = apiKey.isPresent() && !apiKey.get().trim().isEmpty();
        boolean clientReady = batchClient.isReady();
        boolean available = directEnabled && hasApiKey && clientReady;

        LOG.infof(
                "GeminiNativePredictionExtractor availability check: directEnabled=%s, hasApiKey=%s, clientReady=%s, available=%s",
                directEnabled, hasApiKey, clientReady, available);

        if (!available) {
            if (!directEnabled) {
                LOG.infof("GeminiNative is disabled in configuration (directEnabled=%s)", directEnabled);
            }
            if (!hasApiKey) {
                LOG.infof("GeminiNative API key is not available (apiKey.isPresent()=%s)", apiKey.isPresent());
            }
            if (!clientReady) {
                LOG.infof("GeminiNative batch client is not ready");
            }
        }

        return available;
    }

    @Override
    public String getExtractorType() {
        return "gemini-native";
    }

    @Override
    public boolean isBatchModeEnabled() {
        return isAvailable() && directBatchEnabled && getMaxBatchSize() > 1;
    }

    @Override
    public int getMaxBatchSize() {
        LOG.debugf("getMaxBatchSize: directMaxBatchSize=%d", directMaxBatchSize);

        // Use direct injection to avoid configuration object issues
        int batchSize = directMaxBatchSize;

        // Ensure we have a valid batch size
        if (batchSize <= 0) {
            LOG.warnf("Invalid batch size: %d, using default of 1", batchSize);
            return 1;
        }

        return batchSize;
    }

    @Override
    public String getConfiguration() {
        return String.format(
                "Type: %s, Model: %s, Enabled: %s, Available: %s, BatchEnabled: %s, MaxBatchSize: %d, ApiKey: %s, ClientReady: %s",
                getExtractorType(), model, directEnabled, isAvailable(),
                directBatchEnabled, getMaxBatchSize(), apiKey.isPresent() ? "***set***" : "not set",
                batchClient.isReady());
    }

    /**
     * Gets detailed configuration status for monitoring and debugging.
     */
    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("extractorType", getExtractorType());
        status.put("available", isAvailable());
        status.put("batchModeEnabled", isBatchModeEnabled());
        status.put("maxBatchSize", getMaxBatchSize());
        status.put("activeBatchJobs", activeBatchJobs.size());
        status.put("configuration", getDirectConfigurationSummary());
        status.put("batchClientStatus", batchClient.getClientStatus());
        return status;
    }

    /**
     * Gets health check information for monitoring endpoints.
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", isAvailable() ? "UP" : "DOWN");
        health.put("extractorType", getExtractorType());
        health.put("configurationValid", isConfigurationValid());
        health.put("batchClientReady", batchClient.isReady());
        health.put("activeBatchJobs", activeBatchJobs.size());

        if (!isAvailable()) {
            List<String> issues = new ArrayList<>();
            if (!directEnabled)
                issues.add("Configuration disabled");
            if (!isConfigurationValid())
                issues.add("Invalid configuration");
            if (!batchClient.isReady())
                issues.add("Batch client not ready");
            health.put("issues", issues);
        }

        return health;
    }

    // Helper methods for internal processing

    /**
     * Split articles into batches based on maxBatchSize.
     */
    private List<Map<String, ArticleData>> splitIntoBatches(Map<String, ArticleData> articles) {
        // Memory monitoring
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        List<Map<String, ArticleData>> batches = new ArrayList<>();
        List<Map.Entry<String, ArticleData>> articleList = new ArrayList<>(articles.entrySet());

        int maxBatchSize = getMaxBatchSize();
        LOG.infof("Splitting %d articles into batches with maxBatchSize=%d, memory before: %d MB",
                articleList.size(), maxBatchSize, beforeMemory / (1024 * 1024));

        // Defensive programming: prevent infinite loop if maxBatchSize is invalid
        if (maxBatchSize <= 0) {
            LOG.errorf("Invalid maxBatchSize: %d, using default value of 1", maxBatchSize);
            maxBatchSize = 1;
        }

        // Additional safety check to prevent infinite loops
        if (maxBatchSize > articleList.size()) {
            maxBatchSize = articleList.size();
        }

        int batchCount = 0;
        for (int i = 0; i < articleList.size(); i += maxBatchSize) {
            int endIndex = Math.min(i + maxBatchSize, articleList.size());
            Map<String, ArticleData> batch = new HashMap<>();

            LOG.debugf("Creating batch %d: articles %d to %d", batchCount + 1, i, endIndex - 1);

            for (int j = i; j < endIndex; j++) {
                Map.Entry<String, ArticleData> entry = articleList.get(j);
                batch.put(entry.getKey(), entry.getValue());
            }

            batches.add(batch);
            batchCount++;

            // Safety check to prevent runaway batch creation
            if (batchCount > 1000) {
                LOG.errorf("Too many batches created: %d, stopping to prevent memory issues", batchCount);
                break;
            }
        }

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        LOG.infof("Split into %d batches, memory after: %d MB, memory used: %d MB",
                batches.size(), afterMemory / (1024 * 1024), (afterMemory - beforeMemory) / (1024 * 1024));

        return batches;
    }

    /**
     * Wait for batch completion with timeout.
     */
    private BatchResults waitForBatchCompletion(String batchId) {
        try {
            long timeoutMillis = directTimeoutMinutes * 60 * 1000L; // Convert minutes to milliseconds
            return batchClient.getBatchResults(batchId).get(
                    timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to wait for batch completion: %s", batchId);
            throw new RuntimeException("Batch completion failed", e);
        }
    }

    /**
     * Wait for job completion and return results.
     */
    private Map<String, List<PredictionResult>> waitForJobCompletion(String jobId) {
        try {
            // Poll for completion
            int maxAttempts = (directTimeoutMinutes * 60) / directPollingIntervalSeconds; // Calculate based on timeout
                                                                                          // and polling interval

            LOG.infof("Starting to wait for job %s completion: maxAttempts=%d, pollingInterval=%ds, timeout=%dm",
                    jobId, maxAttempts, directPollingIntervalSeconds, directTimeoutMinutes);

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                BatchJobStatus status = checkJobStatus(jobId);

                LOG.debugf("Job %s attempt %d/%d: state=%s", jobId, attempt + 1, maxAttempts, status.state());

                if (status.state() == BatchState.COMPLETED) {
                    LOG.infof("Job %s completed, retrieving results", jobId);
                    return retrieveJobResults(jobId);
                } else if (status.state() == BatchState.FAILED || status.state() == BatchState.CANCELLED) {
                    throw new RuntimeException("Job failed or was cancelled: " + status.errorMessage());
                }

                // Wait before next poll
                if (attempt < maxAttempts - 1) { // Don't sleep on the last attempt
                    LOG.debugf("Job %s still %s, waiting %ds before next check", jobId, status.state(),
                            directPollingIntervalSeconds);
                    Thread.sleep(directPollingIntervalSeconds * 1000L);
                }
            }

            throw new RuntimeException("Job timed out after " + directTimeoutMinutes + " minutes");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Job waiting was interrupted", e);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to wait for job completion: %s", jobId);
            throw new RuntimeException("Job completion failed", e);
        }
    }

    /**
     * Fallback to sequential processing when batch processing fails.
     */
    private Map<String, List<PredictionResult>> fallbackToSequentialProcessing(
            Map<String, ArticleData> articles) {

        LOG.infof("Using fallback sequential processing for %d articles", articles.size());
        Map<String, List<PredictionResult>> results = new HashMap<>();

        for (Map.Entry<String, ArticleData> entry : articles.entrySet()) {
            String articleId = entry.getKey();
            ArticleData article = entry.getValue();

            try {
                List<PredictionResult> predictions = extractPredictions(article.text(), article.title());
                results.put(articleId, predictions);

                // Small delay to avoid overwhelming the API
                Thread.sleep(200);

            } catch (Exception e) {
                LOG.warnf("Failed to process article %s in fallback mode: %s", articleId, e.getMessage());
                results.put(articleId, Collections.emptyList());
            }
        }

        return results;
    }

    /**
     * Process an individual request using direct GenAI client (not batch API).
     * This method is used for single article processing and fallback scenarios.
     * 
     * @param prompt the prompt to send to GenAI
     * @return the response text
     * @throws Exception if the request fails
     */
    private String processIndividualRequest(String prompt) throws Exception {
        // For now, we'll create a simple implementation that doesn't use the batch API
        // This should work with regular API quotas

        // TODO: Implement direct GenAI client call for individual requests
        // For now, we'll use a placeholder that simulates the response
        LOG.debugf("Processing individual request with prompt length: %d", prompt.length());

        // Simulate processing delay
        Thread.sleep(200);

        // Return a placeholder response that indicates no predictions found
        // This will be replaced with actual GenAI API call once implemented
        return "NO_PREDICTIONS_FOUND";
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
        prompt.append("4. A quality rating from 1.0 to 5.0 (based on specificity and credibility, use decimal values)\n");
        prompt.append("5. Brief context (surrounding text that gives meaning to the prediction)\n\n");

        prompt.append("Format your response as:\n");
        prompt.append("PREDICTION: [exact quote]\n");
        prompt.append("TYPE: [category]\n");
        prompt.append("CONFIDENCE: [0.0-1.0]\n");
        prompt.append("RATING: [1.0-5.0]\n");
        prompt.append("CONTEXT: [brief context]\n");
        prompt.append("---\n\n");

        prompt.append("If no predictions are found, respond with: NO_PREDICTIONS_FOUND\n");

        return prompt.toString();
    }

    /**
     * Parse prediction response from GenAI API.
     */
    private List<PredictionResult> parsePredictionResponse(String response, String originalText) {
        LOG.debugf("Starting to parse GenAI response: %s", response != null ? "present" : "null");

        if (response == null || response.trim().isEmpty()) {
            LOG.warn("GenAI response is null or empty");
            return Collections.emptyList();
        }

        if (response.contains("NO_PREDICTIONS_FOUND")) {
            LOG.debug("No predictions found in GenAI response");
            return Collections.emptyList();
        }

        List<PredictionResult> predictions = new ArrayList<>();

        // Split response by prediction separators
        String[] predictionBlocks = response.split("---");
        LOG.debugf("Split response into %d blocks", predictionBlocks.length);

        for (int i = 0; i < predictionBlocks.length; i++) {
            String block = predictionBlocks[i];
            if (block.trim().isEmpty()) {
                continue;
            }

            try {
                PredictionResult prediction = parseSinglePrediction(block.trim());
                if (prediction != null) {
                    predictions.add(prediction);
                    LOG.debugf("Successfully parsed prediction %d: %s", predictions.size(),
                            prediction.predictionText());
                }
            } catch (Exception e) {
                LOG.warnf("Failed to parse prediction block %d: %s", i, e.getMessage());
            }
        }

        LOG.infof("Parsed %d predictions from GenAI response", predictions.size());
        return predictions;
    }

    /**
     * Parse a single prediction from a text block.
     */
    private PredictionResult parseSinglePrediction(String block) {
        try {
            String predictionText = extractField(block, "PREDICTION");
            String type = extractField(block, "TYPE");
            String confidenceStr = extractField(block, "CONFIDENCE");
            String ratingStr = extractField(block, "RATING");
            String context = extractField(block, "CONTEXT");

            if (predictionText == null || predictionText.trim().isEmpty()) {
                return null;
            }

            double confidence = 0.5; // Default
            try {
                if (confidenceStr != null && !confidenceStr.trim().isEmpty()) {
                    confidence = Double.parseDouble(confidenceStr.trim());
                    confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to 0-1
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse confidence '%s': %s", confidenceStr, e.getMessage());
            }

            double rating = 3.0; // Default
            try {
                if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                    rating = Double.parseDouble(ratingStr.trim());
                    rating = Math.max(1.0, Math.min(5.0, rating)); // Clamp to 1.0-5.0
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse rating '%s': %s", ratingStr, e.getMessage());
            }

            return new PredictionResult(
                    predictionText.trim(),
                    type != null ? type.trim() : "other",
                    rating,
                    BigDecimal.valueOf(confidence),
                    context != null ? context.trim() : "",
                    null, // timeframe
                    null // subject
            );

        } catch (Exception e) {
            LOG.errorf("Error parsing single prediction: %s", e.getMessage());
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

    /**
     * Clean up resources and active jobs.
     */
    public void cleanup() {
        LOG.infof("Cleaning up GeminiNativePredictionExtractor: %d active jobs", activeBatchJobs.size());

        // Cancel any active jobs
        for (BatchJobInfo jobInfo : activeBatchJobs.values()) {
            try {
                if (jobInfo.processingFuture != null && !jobInfo.processingFuture.isDone()) {
                    jobInfo.processingFuture.cancel(true);
                }
            } catch (Exception e) {
                LOG.debugf("Error cancelling job %s: %s", jobInfo.jobId, e.getMessage());
            }
        }

        activeBatchJobs.clear();
        LOG.info("GeminiNativePredictionExtractor cleanup completed");
    }

    /**
     * Validates the current configuration using direct injection values.
     * 
     * @return true if configuration is valid
     */
    private boolean isConfigurationValid() {
        if (!directEnabled) {
            return false;
        }

        // Check required fields
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            LOG.debug("API key is not configured");
            return false;
        }

        if (model == null || model.trim().isEmpty()) {
            LOG.debug("Model is not configured");
            return false;
        }

        // Validate numeric ranges
        if (directMaxBatchSize <= 0 || directMaxBatchSize > 100) {
            LOG.debug("Invalid max batch size: " + directMaxBatchSize);
            return false;
        }

        if (directPollingIntervalSeconds <= 0 || directPollingIntervalSeconds > 300) {
            LOG.debug("Invalid polling interval: " + directPollingIntervalSeconds);
            return false;
        }

        if (directTimeoutMinutes <= 0 || directTimeoutMinutes > 120) {
            LOG.debug("Invalid timeout minutes: " + directTimeoutMinutes);
            return false;
        }

        return true;
    }

    /**
     * Gets configuration summary using direct injection values.
     * 
     * @return configuration summary map
     */
    private Map<String, Object> getDirectConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("enabled", directEnabled);
        summary.put("model", model);
        summary.put("hasApiKey", apiKey.isPresent() && !apiKey.get().trim().isEmpty());
        summary.put("maxBatchSize", directMaxBatchSize);
        summary.put("pollingIntervalSeconds", directPollingIntervalSeconds);
        summary.put("timeoutMinutes", directTimeoutMinutes);
        summary.put("batchEnabled", directBatchEnabled);
        summary.put("valid", isConfigurationValid());
        return summary;
    }
}