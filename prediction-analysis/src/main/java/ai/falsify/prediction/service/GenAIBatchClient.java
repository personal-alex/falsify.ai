package ai.falsify.prediction.service;

import ai.falsify.prediction.config.GeminiNativeConfiguration;
import ai.falsify.prediction.model.BatchRequest;
import ai.falsify.prediction.model.BatchResponse;
import ai.falsify.prediction.model.BatchResults;
import ai.falsify.prediction.model.BatchMetrics;
import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.BatchState;
// RetryService and CrawlingException removed due to CDI context issues in custom thread pool

import com.google.genai.Client;
import com.google.genai.types.BatchJob;
import com.google.genai.types.BatchJobSource;
import com.google.genai.types.CreateBatchJobConfig;
import com.google.genai.types.ListBatchJobsConfig;
import com.google.genai.types.InlinedRequest;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import java.util.Optional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.UUID;

/**
 * Low-level client for GenAI API batch operations using Google GenAI SDK.
 * Provides wrapper methods for BatchJob operations with proper authentication,
 * connection management, and error handling.
 * 
 * This client handles:
 * - Google GenAI Client initialization and lifecycle management
 * - Batch job submission, status checking, and result retrieval
 * - Connection pooling and resource management
 * - Authentication using API key from configuration
 */
@ApplicationScoped
public class GenAIBatchClient {

    private static final Logger LOG = Logger.getLogger(GenAIBatchClient.class);

    @Inject
    GeminiNativeConfiguration config;

    // Note: RetryService removed due to CDI context issues in custom thread pool

    // Use the same configuration approach as GeminiPredictionExtractor
    @ConfigProperty(name = "prediction.llm.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "prediction.llm.model", defaultValue = "gemini-1.5-flash")
    String model;

    // Direct injection to bypass potential configuration object issues
    @ConfigProperty(name = "prediction.gemini-native.max-batch-size", defaultValue = "20")
    int directMaxBatchSize;

    @ConfigProperty(name = "prediction.gemini-native.batch.max-concurrent-jobs", defaultValue = "3")
    int directMaxConcurrentJobs;

    @ConfigProperty(name = "prediction.gemini-native.timeout-minutes", defaultValue = "30")
    int directTimeoutMinutes;

    private Client genaiClient;
    private ScheduledExecutorService executorService;
    private final Map<String, BatchJobTracker> activeBatches = new ConcurrentHashMap<>();

    /**
     * Tracks batch job information for monitoring and cleanup.
     */
    private static class BatchJobTracker {
        final String batchId;
        final Instant createdAt;
        final int requestCount;
        final List<BatchRequest> requests;
        volatile Instant lastStatusCheck;
        volatile BatchState lastKnownState;
        volatile Map<String, BatchResponse> responses;
        volatile CompletableFuture<BatchResults> processingFuture;
        volatile String genaiJobName; // GenAI API job name for cancellation

        BatchJobTracker(String batchId, List<BatchRequest> requests) {
            this.batchId = batchId;
            this.requests = requests;
            this.requestCount = requests.size();
            this.createdAt = Instant.now();
            this.lastStatusCheck = Instant.now();
            this.lastKnownState = BatchState.SUBMITTED;
            this.responses = new ConcurrentHashMap<>();
        }
    }

    @PostConstruct
    void initialize() {
        LOG.info("Initializing GenAI Batch Client");

        // Check if we have the required API key
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            LOG.warn("GenAI API key is not configured, client will not be functional");
            return;
        }

        try {
            // Initialize Google GenAI Client with API key authentication
            // The client automatically uses the API key from environment variable
            // GOOGLE_API_KEY
            // or we can set it programmatically
            System.setProperty("GOOGLE_API_KEY", apiKey.get());
            genaiClient = new Client();

            // Initialize executor service for async operations
            int maxConcurrentJobs = directMaxConcurrentJobs > 0 ? directMaxConcurrentJobs : 3; // Default fallback
            LOG.infof("Initializing executor with maxConcurrentJobs: config=%d, direct=%d, using=%d",
                    config.maxConcurrentJobs, directMaxConcurrentJobs, maxConcurrentJobs);

            executorService = Executors.newScheduledThreadPool(
                    Math.max(2, maxConcurrentJobs),
                    r -> {
                        Thread t = new Thread(r, "genai-batch-client");
                        t.setDaemon(true);
                        return t;
                    });

            LOG.infof("GenAI Batch Client initialized successfully with model: %s, apiKey: %s",
                    model, apiKey.isPresent() ? "***set***" : "not set");

            // Test connectivity
            testConnectivity();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize GenAI Batch Client: %s", e.getMessage());
            throw new RuntimeException("GenAI Batch Client initialization failed", e);
        }
    }

    @PreDestroy
    void cleanup() {
        LOG.info("Shutting down GenAI Batch Client");

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Clean up any remaining batch trackers
        activeBatches.clear();

        LOG.info("GenAI Batch Client shutdown complete");
    }

    /**
     * Tests connectivity to the GenAI API.
     * 
     * @throws RuntimeException if connectivity test fails
     */
    private void testConnectivity() {
        if (genaiClient == null) {
            throw new RuntimeException("GenAI client is not initialized");
        }

        try {
            LOG.debug("Testing GenAI API connectivity by listing batch jobs");

            // Test connectivity by attempting to list batch jobs (with small page size)
            genaiClient.batches.list(com.google.genai.types.ListBatchJobsConfig.builder()
                    .pageSize(1)
                    .build());

            LOG.info("GenAI API connectivity test successful");

        } catch (Exception e) {
            LOG.warnf("GenAI API connectivity test failed (this may be normal during startup): %s",
                    e.getMessage());
            // Don't throw here as the API might be temporarily unavailable
        }
    }

    /**
     * Submits a batch request to the GenAI API.
     * Since the GenAI library may not support true batch operations,
     * this implementation simulates batch processing using individual requests.
     * 
     * @param requests list of batch requests to process
     * @return CompletableFuture containing the batch ID
     * @throws IllegalArgumentException if requests are invalid
     * @throws IllegalStateException    if client is not properly initialized
     */
    public CompletableFuture<String> submitBatchRequest(List<BatchRequest> requests) {
        if (genaiClient == null) {
            throw new IllegalStateException("GenAI client is not initialized");
        }

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Batch requests cannot be null or empty");
        }

        // Use direct injection to avoid configuration object issues
        int maxBatchSize = directMaxBatchSize > 0 ? directMaxBatchSize : 20; // Default fallback

        LOG.infof("Batch size validation: requests=%d, config.maxBatchSize=%d, directMaxBatchSize=%d, using=%d",
                requests.size(), config.maxBatchSize, directMaxBatchSize, maxBatchSize);

        if (requests.size() > maxBatchSize) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum allowed size %d",
                            requests.size(), maxBatchSize));
        }

        LOG.infof("Submitting batch request with %d items to GenAI Batch API", requests.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate a unique batch ID for internal tracking
                String batchId = "batch_" + UUID.randomUUID().toString();

                // Create tracker for this batch
                BatchJobTracker tracker = new BatchJobTracker(batchId, requests);
                activeBatches.put(batchId, tracker);

                // Start processing the batch asynchronously using GenAI Batch API
                tracker.processingFuture = processBatchAsync(tracker);
                tracker.lastKnownState = BatchState.SUBMITTED;

                LOG.infof("GenAI batch job submitted successfully with internal ID: %s", batchId);
                return batchId;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to submit batch request: %s", e.getMessage());
                throw GenAIException.batchSubmissionFailed(e, requests.size());
            }
        }, executorService);
    }

    /**
     * Processes a batch of requests asynchronously using the GenAI Batch API.
     * 
     * @param tracker the batch job tracker
     * @return CompletableFuture containing the batch results
     */
    private CompletableFuture<BatchResults> processBatchAsync(BatchJobTracker tracker) {
        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();

            try {
                LOG.infof("Starting to process batch %s with %d requests using GenAI Batch API",
                        tracker.batchId, tracker.requestCount);

                // Convert BatchRequests to InlinedRequests for GenAI API
                List<InlinedRequest> inlinedRequests = tracker.requests.stream()
                        .map(this::convertToInlinedRequest)
                        .collect(Collectors.toList());

                // Create batch job source with inlined requests
                BatchJobSource batchJobSource = BatchJobSource.builder()
                        .inlinedRequests(inlinedRequests.toArray(new InlinedRequest[0]))
                        .build();

                // Create batch job configuration
                CreateBatchJobConfig config = CreateBatchJobConfig.builder()
                        .displayName("prediction-analysis-batch-" + tracker.batchId)
                        .build();

                // Submit the batch job to GenAI API
                LOG.debugf("Submitting batch of %s requests to model %s", tracker.requestCount, model);
                BatchJob batchJob = genaiClient.batches.create(model, batchJobSource, config);
                String genaiJobName = batchJob.name().orElse(tracker.batchId);

                LOG.infof("GenAI batch job created: %s (internal ID: %s)", genaiJobName, tracker.batchId);

                // Store the GenAI job name for tracking and cancellation
                tracker.genaiJobName = genaiJobName;
                tracker.lastStatusCheck = Instant.now();
                tracker.lastKnownState = BatchState.PROCESSING;

                // Poll for completion
                BatchJob completedJob = pollForCompletion(genaiJobName, tracker);

                // Process the results
                BatchResults results = processCompletedBatchJob(completedJob, tracker, startTime);
                tracker.lastKnownState = BatchState.COMPLETED;

                LOG.infof("Completed processing batch %s: %s", tracker.batchId, results.getSummary());
                return results;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to process batch %s: %s", tracker.batchId, e.getMessage());
                tracker.lastKnownState = BatchState.FAILED;
                
                // Check if this is a quota/rate limit error that should trigger fallback
                if (isQuotaOrRateLimitError(e)) {
                    LOG.warnf("Batch %s failed due to quota/rate limit, fallback should be triggered: %s", 
                             tracker.batchId, e.getMessage());
                    throw new GenAIException("Batch processing failed due to quota limits", e, 
                                           "processBatch", tracker.batchId, true);
                } else {
                    throw new RuntimeException("Batch processing failed", e);
                }
            }
        }, executorService);
    }

    /**
     * Converts a BatchRequest to an InlinedRequest for the GenAI API.
     * 
     * @param request the batch request to convert
     * @return the inlined request
     */
    private InlinedRequest convertToInlinedRequest(BatchRequest request) {
        return InlinedRequest.builder()
                .contents(Content.builder()
                        .parts(Part.fromText(request.prompt()))
                        .build())
                .build();
    }

    /**
     * Polls for batch job completion.
     * 
     * @param genaiJobName the GenAI job name
     * @param tracker      the batch job tracker
     * @return the completed batch job
     * @throws RuntimeException if polling fails or times out
     */
    private BatchJob pollForCompletion(String genaiJobName, BatchJobTracker tracker) {
        long timeoutMillis = directTimeoutMinutes * 60 * 1000L;
        long pollIntervalMs = 5000; // 5 seconds
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                BatchJob job = genaiClient.batches.get(genaiJobName, null);
                String state = job.state().map(s -> s.toString()).orElse("UNKNOWN");

                LOG.debugf("Batch job %s state: %s", genaiJobName, state);
                tracker.lastStatusCheck = Instant.now();

                // Update tracker state
                BatchState mappedState = mapGenAIStateToBatchState(state);
                tracker.lastKnownState = mappedState;

                // Handle different possible state values from GenAI API
                if (state.contains("SUCCEEDED") || state.contains("COMPLETED")) {
                    LOG.infof("Batch job %s completed successfully", genaiJobName);
                    tracker.lastKnownState = BatchState.COMPLETED;
                    return job;
                } else if (state.contains("FAILED") || state.contains("ERROR")) {
                    String error = job.error().map(e -> e.message().orElse("Unknown error")).orElse("Batch job failed");
                    tracker.lastKnownState = BatchState.FAILED;
                    throw new RuntimeException("Batch job failed: " + error);
                } else if (state.contains("CANCELLED")) {
                    tracker.lastKnownState = BatchState.CANCELLED;
                    throw new RuntimeException("Batch job was cancelled");
                } else if (state.contains("PENDING") || state.contains("RUNNING") || state.contains("PROCESSING")) {
                    // Continue polling
                    tracker.lastKnownState = BatchState.PROCESSING;
                    LOG.debugf("Batch job %s still processing, state: %s", genaiJobName, state);
                } else {
                    LOG.warnf("Unknown batch job state: %s, continuing to poll", state);
                }

                Thread.sleep(pollIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted", e);
            } catch (Exception e) {
                LOG.warnf("Error polling batch job %s: %s", genaiJobName, e.getMessage());
                // Continue polling unless it's a fatal error
                if (e instanceof RuntimeException && e.getMessage().contains("failed")) {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Batch job polling timed out after " + directTimeoutMinutes + " minutes");
    }

    /**
     * Processes the results from a completed batch job.
     * 
     * @param completedJob the completed batch job
     * @param tracker      the batch job tracker
     * @param startTime    the processing start time
     * @return the batch results
     */
    private BatchResults processCompletedBatchJob(BatchJob completedJob, BatchJobTracker tracker, Instant startTime) {
        try {
            Map<String, BatchResponse> responses = new ConcurrentHashMap<>();

            LOG.infof("Processing completed batch job for batch %s", tracker.batchId);

            // Use the correct API: BatchJob.dest() -> BatchJobDestination.inlinedResponses()
            if (completedJob.dest().isPresent()) {
                var destination = completedJob.dest().get();
                LOG.infof("Processing batch job destination for batch %s", tracker.batchId);
                
                if (destination.inlinedResponses().isPresent()) {
                    var inlinedResponses = destination.inlinedResponses().get();
                    LOG.infof("Found %d inlined responses", inlinedResponses.size());
                    
                    // Process each inlined response
                    for (int i = 0; i < inlinedResponses.size() && i < tracker.requests.size(); i++) {
                        var inlinedResponse = inlinedResponses.get(i);
                        BatchRequest originalRequest = tracker.requests.get(i);
                        String requestId = originalRequest.requestId();
                        
                        try {
                            if (inlinedResponse.response().isPresent()) {
                                var response = inlinedResponse.response().get();
                                
                                // Extract the text content from the response
                                String responseText = extractTextFromResponse(response);
                                
                                responses.put(requestId, BatchResponse.success(requestId, responseText));
                                LOG.debugf("Successfully processed response for request %s: %d chars", requestId, responseText.length());
                                
                            } else {
                                LOG.warnf("No response found for request %s", requestId);
                                responses.put(requestId, BatchResponse.failure(requestId, "No response content"));
                            }
                        } catch (Exception e) {
                            LOG.warnf("Failed to process response for request %s: %s", requestId, e.getMessage());
                            responses.put(requestId, BatchResponse.failure(requestId, "Response processing failed: " + e.getMessage()));
                        }
                    }
                } else {
                    LOG.warn("No inlined responses found in batch job destination");
                    // Create failure responses for all requests
                    for (BatchRequest request : tracker.requests) {
                        responses.put(request.requestId(), 
                                    BatchResponse.failure(request.requestId(), "No inlined responses in batch destination"));
                    }
                }
            } else {
                LOG.warnf("No destination found in completed batch job for batch %s", tracker.batchId);
                LOG.debugf("BatchJob details: name=%s, state=%s", 
                          completedJob.name().orElse("unknown"), 
                          completedJob.state().map(s -> s.toString()).orElse("unknown"));
                // Create failure responses for all requests
                for (BatchRequest request : tracker.requests) {
                    responses.put(request.requestId(), 
                                BatchResponse.failure(request.requestId(), "No destination in completed batch job"));
                }
            }

            tracker.responses = responses;

            // Create metrics
            Instant endTime = Instant.now();
            Duration processingTime = Duration.between(startTime, endTime);

            int successCount = (int) responses.values().stream().filter(BatchResponse::success).count();
            int failureCount = responses.size() - successCount;

            BatchMetrics metrics = new BatchMetrics(
                    startTime,
                    endTime,
                    tracker.requestCount,
                    successCount,
                    failureCount,
                    calculateTotalTokens(responses),
                    0L, // Total cost in cents - would need to be extracted from job metadata
                    processingTime.toMillis() / (double) tracker.requestCount,
                    tracker.requestCount / Math.max(processingTime.toSeconds(), 1.0));

            return BatchResults.completed(tracker.batchId, responses, metrics);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process completed batch job results: %s", e.getMessage());
            throw new RuntimeException("Failed to process batch results", e);
        }
    }

    /**
     * Extracts text content from a GenAI response.
     * 
     * @param response the GenAI response
     * @return the extracted text content
     */
    private String extractTextFromResponse(com.google.genai.types.GenerateContentResponse response) {
        try {
            if (response.candidates().isEmpty()) {
                return "NO_PREDICTIONS_FOUND";
            }
            
            var candidate = response.candidates().get().get(0);
            if (candidate.content().get().parts().isEmpty()) {
                return "NO_PREDICTIONS_FOUND";
            }
            
            // Extract text from all parts and concatenate
            StringBuilder textBuilder = new StringBuilder();
            for (var part : candidate.content().get().parts().get()) {
                if (part.text().isPresent()) {
                    textBuilder.append(part.text().get());
                }
            }
            
            String extractedText = textBuilder.toString().trim();
            return extractedText.isEmpty() ? "NO_PREDICTIONS_FOUND" : extractedText;
            
        } catch (Exception e) {
            LOG.warnf("Failed to extract text from response: %s", e.getMessage());
            return "NO_PREDICTIONS_FOUND";
        }
    }

    /**
     * Gets the status of a batch job.
     * 
     * @param batchId the batch identifier
     * @return CompletableFuture containing the batch job status
     * @throws IllegalArgumentException if batchId is invalid
     * @throws IllegalStateException    if client is not properly initialized
     */
    public CompletableFuture<BatchJobStatus> getBatchStatus(String batchId) {
        if (genaiClient == null) {
            throw new IllegalStateException("GenAI client is not initialized");
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }

        LOG.debugf("Checking status for GenAI batch: %s", batchId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                BatchJobTracker tracker = activeBatches.get(batchId);

                if (tracker == null) {
                    throw new IllegalArgumentException("Batch not found: " + batchId);
                }

                // Update tracker status check time
                tracker.lastStatusCheck = Instant.now();

                // Check actual GenAI API status if we have the job name
                if (tracker.genaiJobName != null && tracker.lastKnownState != BatchState.COMPLETED && tracker.lastKnownState != BatchState.FAILED) {
                    try {
                        BatchJob job = genaiClient.batches.get(tracker.genaiJobName, null);
                        String genaiState = job.state().map(s -> s.toString()).orElse("UNKNOWN");
                        
                        LOG.debugf("GenAI job %s actual state: %s", tracker.genaiJobName, genaiState);
                        
                        // Update our internal state based on GenAI state
                        BatchState newState = mapGenAIStateToBatchState(genaiState);
                        if (newState != tracker.lastKnownState) {
                            LOG.infof("Updating batch %s state from %s to %s", batchId, tracker.lastKnownState, newState);
                            tracker.lastKnownState = newState;
                            
                            // If completed, trigger result processing
                            if (newState == BatchState.COMPLETED && tracker.processingFuture != null && !tracker.processingFuture.isDone()) {
                                LOG.infof("GenAI job completed, triggering result processing for batch %s", batchId);
                                // The processingFuture should complete naturally when it next polls
                            }
                        }
                    } catch (Exception e) {
                        LOG.warnf("Failed to check GenAI job status for %s: %s", tracker.genaiJobName, e.getMessage());
                        // Continue with cached state
                    }
                }

                // Calculate completed and failed counts
                int completedCount = tracker.responses.size();
                int failedCount = (int) tracker.responses.values().stream()
                        .filter(response -> !response.success()).count();

                // Build status response
                BatchJobStatus status = new BatchJobStatus(
                        batchId,
                        tracker.genaiJobName != null ? tracker.genaiJobName : batchId,
                        tracker.lastKnownState,
                        tracker.requestCount,
                        completedCount,
                        failedCount,
                        tracker.createdAt,
                        Instant.now(),
                        tracker.lastKnownState == BatchState.FAILED ? "Batch processing failed" : null);

                LOG.debugf("Batch %s status: %s (%d/%d completed)",
                        batchId, tracker.lastKnownState, completedCount, tracker.requestCount);

                return status;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to get batch status for %s: %s", batchId, e.getMessage());
                throw GenAIException.batchStatusFailed(e, batchId);
            }
        }, executorService);
    }

    /**
     * Maps GenAI API state strings to our internal BatchState enum.
     * 
     * @param genaiState the GenAI API state string
     * @return corresponding BatchState
     */
    private BatchState mapGenAIStateToBatchState(String genaiState) {
        if (genaiState == null) {
            return BatchState.PROCESSING;
        }
        
        String state = genaiState.toUpperCase();
        
        if (state.contains("SUCCEEDED") || state.contains("COMPLETED")) {
            return BatchState.COMPLETED;
        } else if (state.contains("FAILED") || state.contains("ERROR")) {
            return BatchState.FAILED;
        } else if (state.contains("CANCELLED")) {
            return BatchState.CANCELLED;
        } else if (state.contains("PENDING") || state.contains("RUNNING") || state.contains("PROCESSING")) {
            return BatchState.PROCESSING;
        } else {
            LOG.warnf("Unknown GenAI state: %s, defaulting to PROCESSING", genaiState);
            return BatchState.PROCESSING;
        }
    }

    /**
     * Processes results directly from a completed BatchJob.
     * This is used when we detect a job is completed outside of the normal polling flow.
     * 
     * @param completedJob the completed batch job
     * @param tracker the batch job tracker
     * @return the batch results
     */
    private BatchResults processCompletedBatchJobDirect(BatchJob completedJob, BatchJobTracker tracker) {
        Instant startTime = tracker.createdAt;
        return processCompletedBatchJob(completedJob, tracker, startTime);
    }

    /**
     * Retrieves the results of a completed batch job.
     * 
     * @param batchId the batch identifier
     * @return CompletableFuture containing the batch results
     * @throws IllegalArgumentException if batchId is invalid
     * @throws IllegalStateException    if client is not properly initialized
     */
    public CompletableFuture<BatchResults> getBatchResults(String batchId) {
        if (genaiClient == null) {
            throw new IllegalStateException("GenAI client is not initialized");
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }

        LOG.infof("Retrieving results for GenAI batch: %s", batchId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                BatchJobTracker tracker = activeBatches.get(batchId);

                if (tracker == null) {
                    throw new IllegalArgumentException("Batch not found: " + batchId);
                }

                // Check if we need to update the status from GenAI API
                if (tracker.lastKnownState != BatchState.COMPLETED && tracker.genaiJobName != null) {
                    try {
                        BatchJob job = genaiClient.batches.get(tracker.genaiJobName, null);
                        String genaiState = job.state().map(s -> s.toString()).orElse("UNKNOWN");
                        
                        LOG.infof("Checking GenAI job %s for results: state=%s", tracker.genaiJobName, genaiState);
                        
                        if (genaiState.contains("SUCCEEDED") || genaiState.contains("COMPLETED")) {
                            LOG.infof("GenAI job %s is completed, processing results immediately", tracker.genaiJobName);
                            LOG.debugf("BatchJob has dest object: %s", job.dest().isPresent());
                            tracker.lastKnownState = BatchState.COMPLETED;
                            
                            // Process results directly since the job is completed
                            BatchResults results = processCompletedBatchJobDirect(job, tracker);
                            
                            // Clean up tracker
                            activeBatches.remove(batchId);
                            
                            return results;
                        } else if (genaiState.contains("FAILED") || genaiState.contains("ERROR")) {
                            tracker.lastKnownState = BatchState.FAILED;
                            String error = job.error().map(e -> e.message().orElse("Unknown error")).orElse("Batch job failed");
                            throw new IllegalStateException("Batch job failed: " + error);
                        }
                    } catch (Exception e) {
                        LOG.warnf("Failed to check GenAI job status for results: %s", e.getMessage());
                        // Continue with existing logic
                    }
                }

                if (tracker.lastKnownState != BatchState.COMPLETED) {
                    // If processing is still ongoing, wait for it to complete
                    if (tracker.processingFuture != null && !tracker.processingFuture.isDone()) {
                        try {
                            long timeoutMillis = directTimeoutMinutes * 60 * 1000L; // Convert minutes to milliseconds
                            return tracker.processingFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    String.format("Batch %s is not completed (state: %s)",
                                            batchId, tracker.lastKnownState));
                        }
                    } else {
                        throw new IllegalStateException(
                                String.format("Batch %s is not completed (state: %s)",
                                        batchId, tracker.lastKnownState));
                    }
                }

                // Create metrics
                Instant startTime = tracker.createdAt;
                Instant endTime = Instant.now();
                Duration processingTime = Duration.between(startTime, endTime);

                int successCount = (int) tracker.responses.values().stream()
                        .filter(BatchResponse::success).count();
                int failureCount = tracker.responses.size() - successCount;

                LOG.debugf("Batch %s metrics: requestCount=%d, responseCount=%d, successCount=%d, failureCount=%d", 
                          batchId, tracker.requestCount, tracker.responses.size(), successCount, failureCount);

                BatchMetrics metrics = new BatchMetrics(
                        startTime,
                        endTime,
                        tracker.requestCount,
                        successCount,
                        failureCount,
                        calculateTotalTokens(tracker.responses),
                        0L, // Total cost in cents
                        processingTime.toMillis() / (double) tracker.requestCount, // Average latency
                        tracker.requestCount / Math.max(processingTime.toSeconds(), 1.0) // Requests per second
                );

                BatchResults results = BatchResults.completed(batchId, tracker.responses, metrics);

                // Clean up tracker for completed batch
                activeBatches.remove(batchId);

                LOG.infof("Retrieved results for batch %s: %s", batchId, results.getSummary());
                return results;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to get batch results for %s: %s", batchId, e.getMessage());
                throw GenAIException.batchResultsFailed(e, batchId);
            }
        }, executorService);
    }

    /**
     * Cancels a batch job.
     * 
     * @param batchId the batch identifier
     * @return CompletableFuture that completes when cancellation is done
     * @throws IllegalArgumentException if batchId is invalid
     * @throws IllegalStateException    if client is not properly initialized
     */
    public CompletableFuture<Void> cancelBatch(String batchId) {
        if (genaiClient == null) {
            throw new IllegalStateException("GenAI client is not initialized");
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }

        LOG.infof("Cancelling GenAI batch: %s", batchId);

        return CompletableFuture.runAsync(() -> {
            try {
                BatchJobTracker tracker = activeBatches.get(batchId);

                if (tracker == null) {
                    throw new IllegalArgumentException("Batch not found: " + batchId);
                }

                // Cancel the GenAI batch job if we have the job name
                if (tracker.genaiJobName != null) {
                    try {
                        genaiClient.batches.cancel(tracker.genaiJobName, null);
                        LOG.infof("GenAI batch job %s cancelled successfully", tracker.genaiJobName);
                    } catch (Exception e) {
                        LOG.warnf("Failed to cancel GenAI batch job %s: %s", tracker.genaiJobName, e.getMessage());
                        // Continue with local cancellation even if GenAI cancellation fails
                    }
                }

                // Cancel the processing future if it's still running
                if (tracker.processingFuture != null && !tracker.processingFuture.isDone()) {
                    tracker.processingFuture.cancel(true);
                }

                // Update tracker state
                tracker.lastKnownState = BatchState.CANCELLED;
                tracker.lastStatusCheck = Instant.now();

                LOG.infof("Batch %s cancelled successfully", batchId);

            } catch (Exception e) {
                LOG.errorf(e, "Failed to cancel batch %s: %s", batchId, e.getMessage());
                throw GenAIException.batchCancellationFailed(e, batchId);
            }
        }, executorService);
    }

    /**
     * Checks if the client is properly initialized and ready for use.
     * 
     * @return true if client is ready
     */
    public boolean isReady() {
        boolean clientInitialized = genaiClient != null;
        boolean hasApiKey = apiKey.isPresent() && !apiKey.get().trim().isEmpty();
        boolean ready = clientInitialized && hasApiKey;

        LOG.infof("GenAIBatchClient readiness check: clientInitialized=%s, hasApiKey=%s, ready=%s",
                clientInitialized, hasApiKey, ready);

        if (!ready) {
            if (!clientInitialized) {
                LOG.infof("GenAI client is not initialized");
            }
            if (!hasApiKey) {
                LOG.infof("GenAI API key is not available");
            }
        }

        return ready;
    }

    /**
     * Gets the number of active batches being tracked.
     * 
     * @return number of active batches
     */
    public int getActiveBatchCount() {
        return activeBatches.size();
    }

    /**
     * Gets information about active batches.
     * 
     * @return map of batch ID to tracker information
     */
    public Map<String, String> getActiveBatchInfo() {
        return activeBatches.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            BatchJobTracker tracker = entry.getValue();
                            return String.format("State: %s, Requests: %d, Age: %s",
                                    tracker.lastKnownState,
                                    tracker.requestCount,
                                    Duration.between(tracker.createdAt, Instant.now()));
                        }));
    }

    /**
     * Calculates total tokens from batch responses (approximate).
     * 
     * @param responses the batch responses
     * @return estimated total tokens
     */
    private int calculateTotalTokens(Map<String, BatchResponse> responses) {
        return responses.values().stream()
                .mapToInt(response -> {
                    // Rough estimation: 1 token ≈ 4 characters
                    return response.getResponseLength() / 4;
                })
                .sum();
    }

    /**
     * Checks if requests are currently allowed.
     * Since we removed RetryService, requests are always allowed.
     * 
     * @return true (always allows requests)
     */
    public boolean isRequestAllowed() {
        return true;
    }

    /**
     * Checks if an exception is related to quota or rate limiting.
     * 
     * @param e the exception to check
     * @return true if this is a quota/rate limit error
     */
    private boolean isQuotaOrRateLimitError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("429") || 
               lowerMessage.contains("quota") || 
               lowerMessage.contains("rate limit") ||
               lowerMessage.contains("resource has been exhausted") ||
               lowerMessage.contains("too many requests");
    }

    /**
     * Gets comprehensive client status information.
     * 
     * @return status information map
     */
    public Map<String, Object> getClientStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("ready", isReady());
        status.put("activeBatches", getActiveBatchCount());
        status.put("requestsAllowed", true); // Always allow requests now
        status.put("configuration", config.getConfigurationSummary());
        status.put("activeBatchInfo", getActiveBatchInfo());

        // Remove circuit breaker stats since we no longer use RetryService
        status.put("retryImplementation", "simple");

        return status;
    }
} 