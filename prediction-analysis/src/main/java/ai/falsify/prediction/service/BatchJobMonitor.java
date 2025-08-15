package ai.falsify.prediction.service;

import ai.falsify.prediction.config.GeminiNativeConfiguration;
import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.BatchState;
import ai.falsify.prediction.model.BatchResults;
import ai.falsify.prediction.model.PredictionResult;
import ai.falsify.prediction.service.BatchPredictionExtractor.ArticleData;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service for monitoring GenAI batch job status.
 * 
 * This service provides:
 * - Scheduled polling of active batch jobs
 * - Job registration and tracking methods
 * - Concurrent job status checking
 * - Job state transition management
 * - Automatic timeout detection and handling
 * - Job completion and failure handling
 * - Resource cleanup and deallocation
 * 
 * The monitor runs as a background service that polls the GenAI API
 * for job status updates and manages the lifecycle of batch jobs.
 */
@ApplicationScoped
public class BatchJobMonitor {

    private static final Logger LOG = Logger.getLogger(BatchJobMonitor.class);

    @Inject
    GeminiNativeConfiguration config;

    @Inject
    GenAIBatchClient batchClient;

    @Inject
    AnalysisNotificationService notificationService;

    private ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Track active batch jobs for monitoring
    private final Map<String, MonitoredBatchJob> activeBatchJobs = new ConcurrentHashMap<>();

    /**
     * Internal class to track monitored batch job information.
     */
    private static class MonitoredBatchJob {
        final String jobId;
        final String batchId;
        final String analysisJobId;
        final Instant createdAt;
        final Instant timeoutAt;
        final Map<String, ArticleData> originalArticles;
        
        volatile BatchState lastKnownState;
        volatile Instant lastStatusCheck;
        volatile Instant lastStateChange;
        volatile int consecutiveFailures;
        volatile String lastError;
        volatile CompletableFuture<Map<String, List<PredictionResult>>> completionFuture;

        MonitoredBatchJob(String jobId, String batchId, String analysisJobId, 
                         Map<String, ArticleData> articles, Duration timeout) {
            this.jobId = jobId;
            this.batchId = batchId;
            this.analysisJobId = analysisJobId;
            this.originalArticles = Map.copyOf(articles);
            this.createdAt = Instant.now();
            this.timeoutAt = createdAt.plus(timeout);
            this.lastKnownState = BatchState.SUBMITTED;
            this.lastStatusCheck = createdAt;
            this.lastStateChange = createdAt;
            this.consecutiveFailures = 0;
        }

        boolean isTimedOut() {
            return Instant.now().isAfter(timeoutAt);
        }

        Duration getAge() {
            return Duration.between(createdAt, Instant.now());
        }

        Duration getTimeSinceLastCheck() {
            return Duration.between(lastStatusCheck, Instant.now());
        }

        void updateState(BatchState newState) {
            if (this.lastKnownState != newState) {
                this.lastKnownState = newState;
                this.lastStateChange = Instant.now();
            }
            this.lastStatusCheck = Instant.now();
        }

        void recordFailure(String error) {
            this.consecutiveFailures++;
            this.lastError = error;
            this.lastStatusCheck = Instant.now();
        }

        void resetFailures() {
            this.consecutiveFailures = 0;
            this.lastError = null;
        }
    }

    @PostConstruct
    void initialize() {
        if (!config.enabled || !config.monitoringEnabled) {
            LOG.info("BatchJobMonitor is disabled by configuration");
            return;
        }

        LOG.info("Initializing BatchJobMonitor");

        // Create scheduled executor for monitoring tasks
        scheduledExecutor = Executors.newScheduledThreadPool(
            Math.max(1, config.maxConcurrentJobs / 2),
            r -> {
                Thread t = new Thread(r, "batch-job-monitor");
                t.setDaemon(true);
                return t;
            }
        );

        // Start the monitoring loop
        startMonitoring();

        LOG.infof("BatchJobMonitor initialized: pollingInterval=%ds, maxConcurrentJobs=%d, timeout=%dm",
                config.pollingIntervalSeconds, config.maxConcurrentJobs, config.timeoutMinutes);
    }

    @PreDestroy
    void shutdown() {
        LOG.info("Shutting down BatchJobMonitor");
        
        isRunning.set(false);

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Force cleanup of all active jobs
        forceCleanupAllJobs();
        LOG.info("BatchJobMonitor shutdown complete");
    }

    /**
     * Registers a batch job for monitoring.
     * 
     * @param jobId the job identifier from the extractor
     * @param batchId the batch identifier from the GenAI API
     * @param analysisJobId the analysis job identifier for notifications
     * @param articles the original articles being processed
     * @return CompletableFuture that completes when the job finishes
     */
    public CompletableFuture<Map<String, List<PredictionResult>>> registerBatchJob(
            String jobId, String batchId, String analysisJobId, Map<String, ArticleData> articles) {
        
        if (!isRunning.get()) {
            throw new IllegalStateException("BatchJobMonitor is not running");
        }

        LOG.infof("Registering batch job for monitoring: jobId=%s, batchId=%s, analysisJobId=%s, articles=%d",
                jobId, batchId, analysisJobId, articles.size());

        MonitoredBatchJob monitoredJob = new MonitoredBatchJob(
            jobId, batchId, analysisJobId, articles, config.getTimeoutDuration());

        // Create completion future
        CompletableFuture<Map<String, List<PredictionResult>>> completionFuture = new CompletableFuture<>();
        monitoredJob.completionFuture = completionFuture;

        // Register the job
        activeBatchJobs.put(jobId, monitoredJob);

        // Send initial notification
        sendBatchSubmittedNotification(analysisJobId, batchId, articles.size());

        LOG.infof("Batch job registered successfully: %s (total active: %d)", 
                jobId, activeBatchJobs.size());

        return completionFuture;
    }

    /**
     * Unregisters a batch job from monitoring.
     * 
     * @param jobId the job identifier
     */
    public void unregisterBatchJob(String jobId) {
        MonitoredBatchJob job = activeBatchJobs.remove(jobId);
        if (job != null) {
            LOG.infof("Unregistered batch job: %s (remaining active: %d)", 
                    jobId, activeBatchJobs.size());
            
            // Cancel completion future if still pending
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.cancel(true);
            }
        }
    }

    /**
     * Gets the current status of a monitored batch job.
     * 
     * @param jobId the job identifier
     * @return job status information or null if not found
     */
    public BatchJobStatus getJobStatus(String jobId) {
        MonitoredBatchJob job = activeBatchJobs.get(jobId);
        if (job == null) {
            return null;
        }

        return new BatchJobStatus(
            job.jobId,
            job.batchId,
            job.lastKnownState,
            job.originalArticles.size(),
            0, // Will be updated during monitoring
            job.consecutiveFailures,
            job.createdAt,
            job.lastStatusCheck,
            job.lastError
        );
    }

    /**
     * Gets information about all active batch jobs.
     * 
     * @return map of job ID to status information
     */
    public Map<String, String> getActiveBatchJobInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, MonitoredBatchJob> entry : activeBatchJobs.entrySet()) {
            MonitoredBatchJob job = entry.getValue();
            String status = String.format(
                "State: %s, Age: %s, LastCheck: %s, Failures: %d",
                job.lastKnownState,
                formatDuration(job.getAge()),
                formatDuration(job.getTimeSinceLastCheck()),
                job.consecutiveFailures
            );
            info.put(entry.getKey(), status);
        }
        
        return info;
    }

    /**
     * Gets the number of active batch jobs being monitored.
     * 
     * @return number of active jobs
     */
    public int getActiveBatchJobCount() {
        return activeBatchJobs.size();
    }

    /**
     * Checks if the monitor is currently running.
     * 
     * @return true if monitoring is active
     */
    public boolean isMonitoringActive() {
        return isRunning.get() && scheduledExecutor != null && !scheduledExecutor.isShutdown();
    }

    // Private helper methods

    /**
     * Starts the monitoring loop.
     */
    private void startMonitoring() {
        isRunning.set(true);
        
        // Schedule the main monitoring task
        scheduledExecutor.scheduleWithFixedDelay(
            this::monitorActiveBatchJobs,
            config.pollingIntervalSeconds, // Initial delay
            config.pollingIntervalSeconds, // Period
            TimeUnit.SECONDS
        );

        // Schedule timeout cleanup task (runs less frequently)
        scheduledExecutor.scheduleWithFixedDelay(
            this::cleanupTimedOutJobs,
            60, // Initial delay (1 minute)
            60, // Period (1 minute)
            TimeUnit.SECONDS
        );

        LOG.info("BatchJobMonitor monitoring started");
    }

    /**
     * Main monitoring method that checks status of all active batch jobs.
     * This method is called periodically by the scheduled executor.
     */
    private void monitorActiveBatchJobs() {
        if (!isRunning.get() || activeBatchJobs.isEmpty()) {
            return;
        }

        LOG.debugf("Monitoring %d active batch jobs", activeBatchJobs.size());

        // Process each job concurrently
        List<CompletableFuture<Void>> monitoringTasks = activeBatchJobs.values().stream()
            .map(this::monitorSingleBatchJob)
            .toList();

        // Wait for all monitoring tasks to complete (with timeout)
        try {
            CompletableFuture.allOf(monitoringTasks.toArray(new CompletableFuture[0]))
                .get(config.pollingIntervalSeconds * 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.warnf("Some batch job monitoring tasks failed or timed out: %s", e.getMessage());
        }

        LOG.debugf("Completed monitoring cycle for %d jobs", activeBatchJobs.size());
    }

    /**
     * Monitors a single batch job with exponential backoff for failures.
     * 
     * @param job the monitored batch job
     * @return CompletableFuture that completes when monitoring is done
     */
    private CompletableFuture<Void> monitorSingleBatchJob(MonitoredBatchJob job) {
        return CompletableFuture.runAsync(() -> {
            try {
                LOG.debugf("Checking status for batch job: %s", job.jobId);

                // Calculate timeout with exponential backoff for failed jobs
                int timeoutSeconds = calculateStatusCheckTimeout(job);

                // Get current status from batch client
                BatchJobStatus status = batchClient.getBatchStatus(job.batchId).get(
                    timeoutSeconds, TimeUnit.SECONDS);

                // Update job state
                BatchState previousState = job.lastKnownState;
                job.updateState(status.state());
                job.resetFailures();

                // Send progress notification if state changed
                if (previousState != status.state()) {
                    sendBatchProgressNotification(job.analysisJobId, job.batchId, 
                        status.state(), status.completedRequests(), status.totalRequests());
                }

                // Handle state-specific logic
                switch (status.state()) {
                    case COMPLETED -> handleJobCompletion(job);
                    case FAILED -> handleJobFailure(job, status.errorMessage());
                    case CANCELLED -> handleJobCancellation(job);
                    case TIMEOUT -> handleJobTimeout(job);
                    default -> {
                        // Job is still processing, continue monitoring
                        LOG.debugf("Job %s is still %s (%d/%d completed)", 
                                job.jobId, status.state(), 
                                status.completedRequests(), status.totalRequests());
                    }
                }

            } catch (Exception e) {
                LOG.warnf("Failed to check status for batch job %s (attempt %d): %s", 
                        job.jobId, job.consecutiveFailures + 1, e.getMessage());
                job.recordFailure(e.getMessage());

                // Apply exponential backoff for next check
                scheduleDelayedStatusCheck(job);

                // If too many consecutive failures, mark job as failed
                if (job.consecutiveFailures >= config.maxRetries) {
                    LOG.errorf("Job %s has failed %d consecutive status checks, marking as failed", 
                            job.jobId, job.consecutiveFailures);
                    handleJobFailure(job, "Too many consecutive status check failures: " + e.getMessage());
                }
            }
        }, scheduledExecutor);
    }

    /**
     * Calculates the timeout for status checks with exponential backoff.
     * 
     * @param job the monitored batch job
     * @return timeout in seconds
     */
    private int calculateStatusCheckTimeout(MonitoredBatchJob job) {
        if (job.consecutiveFailures == 0) {
            return config.pollingIntervalSeconds;
        }

        // Exponential backoff: base delay * 2^failures, capped at max delay
        int baseDelay = config.retryDelaySeconds;
        int exponentialDelay = baseDelay * (int) Math.pow(2, Math.min(job.consecutiveFailures, 5));
        int maxDelay = config.maxRetryDelaySeconds;

        return Math.min(exponentialDelay, maxDelay);
    }

    /**
     * Schedules a delayed status check for a job that has failed.
     * 
     * @param job the monitored batch job
     */
    private void scheduleDelayedStatusCheck(MonitoredBatchJob job) {
        if (!isRunning.get() || job.consecutiveFailures >= config.maxRetries) {
            return;
        }

        int delaySeconds = calculateStatusCheckTimeout(job);
        
        LOG.debugf("Scheduling delayed status check for job %s in %d seconds (failure #%d)", 
                job.jobId, delaySeconds, job.consecutiveFailures);

        scheduledExecutor.schedule(() -> {
            if (isRunning.get() && activeBatchJobs.containsKey(job.jobId)) {
                monitorSingleBatchJob(job);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Cleans up jobs that have exceeded their timeout or become stale.
     */
    private void cleanupTimedOutJobs() {
        if (!isRunning.get()) {
            return;
        }

        Instant now = Instant.now();
        List<MonitoredBatchJob> problematicJobs = activeBatchJobs.values().stream()
            .filter(job -> isJobTimedOut(job, now) || isJobStale(job, now))
            .toList();

        if (!problematicJobs.isEmpty()) {
            LOG.infof("Found %d problematic batch jobs (timed out or stale), cleaning up", 
                    problematicJobs.size());
            
            for (MonitoredBatchJob job : problematicJobs) {
                if (isJobTimedOut(job, now)) {
                    LOG.warnf("Job %s has exceeded maximum timeout (%s old)", 
                            job.jobId, formatDuration(job.getAge()));
                    handleJobTimeout(job);
                } else if (isJobStale(job, now)) {
                    LOG.warnf("Job %s appears stale (no status check for %s)", 
                            job.jobId, formatDuration(job.getTimeSinceLastCheck()));
                    // Try one more status check before timing out
                    monitorSingleBatchJob(job);
                }
            }
        }
    }

    /**
     * Checks if a job has exceeded its maximum timeout.
     * 
     * @param job the monitored batch job
     * @param now current time
     * @return true if job is timed out
     */
    private boolean isJobTimedOut(MonitoredBatchJob job, Instant now) {
        return now.isAfter(job.timeoutAt);
    }

    /**
     * Checks if a job appears stale (no status updates for too long).
     * 
     * @param job the monitored batch job
     * @param now current time
     * @return true if job appears stale
     */
    private boolean isJobStale(MonitoredBatchJob job, Instant now) {
        Duration timeSinceLastCheck = Duration.between(job.lastStatusCheck, now);
        Duration staleThreshold = Duration.ofSeconds(config.pollingIntervalSeconds * 5); // 5x polling interval
        
        return timeSinceLastCheck.compareTo(staleThreshold) > 0 && 
               job.lastKnownState != BatchState.COMPLETED &&
               job.lastKnownState != BatchState.FAILED &&
               job.lastKnownState != BatchState.CANCELLED;
    }

    /**
     * Sends batch submitted notification.
     */
    private void sendBatchSubmittedNotification(String analysisJobId, String batchId, int articleCount) {
        try {
            // Use existing notification service to send custom message
            // We'll extend this when we implement the enhanced notification system
            LOG.infof("Batch submitted: analysisJobId=%s, batchId=%s, articles=%d", 
                    analysisJobId, batchId, articleCount);
        } catch (Exception e) {
            LOG.warnf("Failed to send batch submitted notification: %s", e.getMessage());
        }
    }

    /**
     * Sends batch progress notification.
     */
    private void sendBatchProgressNotification(String analysisJobId, String batchId, 
                                             BatchState state, int completed, int total) {
        try {
            LOG.infof("Batch progress: analysisJobId=%s, batchId=%s, state=%s, progress=%d/%d", 
                    analysisJobId, batchId, state, completed, total);
        } catch (Exception e) {
            LOG.warnf("Failed to send batch progress notification: %s", e.getMessage());
        }
    }

    /**
     * Formats a duration for human-readable display.
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    // Job completion and failure handling methods

    /**
     * Handles successful job completion.
     * 
     * @param job the completed batch job
     */
    private void handleJobCompletion(MonitoredBatchJob job) {
        LOG.infof("Handling completion for batch job: %s", job.jobId);

        try {
            // Retrieve results from batch client
            BatchResults batchResults = batchClient.getBatchResults(job.batchId).get(
                config.pollingIntervalSeconds, TimeUnit.SECONDS);

            // Parse results into prediction format
            Map<String, List<PredictionResult>> predictions = parseResultsToPredictions(
                batchResults, job.originalArticles);

            // Complete the future with results
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.complete(predictions);
            }

            // Send completion notification
            sendBatchCompletedNotification(job.analysisJobId, job.batchId, 
                predictions.size(), getTotalPredictionCount(predictions));

            // Clean up resources
            cleanupJobResources(job);

            LOG.infof("Successfully completed batch job %s: %d articles, %d predictions", 
                    job.jobId, predictions.size(), getTotalPredictionCount(predictions));

        } catch (Exception e) {
            LOG.errorf(e, "Failed to handle completion for batch job %s: %s", job.jobId, e.getMessage());
            handleJobFailure(job, "Failed to retrieve results: " + e.getMessage());
        }
    }

    /**
     * Handles job failure.
     * 
     * @param job the failed batch job
     * @param errorMessage the error message
     */
    private void handleJobFailure(MonitoredBatchJob job, String errorMessage) {
        LOG.warnf("Handling failure for batch job %s: %s", job.jobId, errorMessage);

        try {
            // Try to recover partial results if possible
            Map<String, List<PredictionResult>> partialResults = recoverPartialResults(job);

            // Complete the future with partial results or empty map
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                if (!partialResults.isEmpty()) {
                    LOG.infof("Recovered %d partial results for failed job %s", 
                            partialResults.size(), job.jobId);
                    job.completionFuture.complete(partialResults);
                } else {
                    job.completionFuture.completeExceptionally(
                        new RuntimeException("Batch job failed: " + errorMessage));
                }
            }

            // Send failure notification
            sendBatchFailedNotification(job.analysisJobId, job.batchId, errorMessage);

            // Clean up resources
            cleanupJobResources(job);

        } catch (Exception e) {
            LOG.errorf(e, "Error during failure handling for job %s: %s", job.jobId, e.getMessage());
            
            // Ensure future is completed
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.completeExceptionally(
                    new RuntimeException("Batch job failed with additional errors", e));
            }
            
            cleanupJobResources(job);
        }
    }

    /**
     * Handles job cancellation.
     * 
     * @param job the cancelled batch job
     */
    private void handleJobCancellation(MonitoredBatchJob job) {
        LOG.infof("Handling cancellation for batch job: %s", job.jobId);

        try {
            // Try to recover any partial results
            Map<String, List<PredictionResult>> partialResults = recoverPartialResults(job);

            // Complete the future with cancellation
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                if (!partialResults.isEmpty()) {
                    LOG.infof("Recovered %d partial results for cancelled job %s", 
                            partialResults.size(), job.jobId);
                    job.completionFuture.complete(partialResults);
                } else {
                    job.completionFuture.cancel(true);
                }
            }

            // Send cancellation notification
            sendBatchCancelledNotification(job.analysisJobId, job.batchId);

            // Clean up resources
            cleanupJobResources(job);

        } catch (Exception e) {
            LOG.errorf(e, "Error during cancellation handling for job %s: %s", job.jobId, e.getMessage());
            
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.cancel(true);
            }
            
            cleanupJobResources(job);
        }
    }

    /**
     * Handles job timeout.
     * 
     * @param job the timed out batch job
     */
    private void handleJobTimeout(MonitoredBatchJob job) {
        LOG.warnf("Handling timeout for batch job %s (age: %s)", job.jobId, formatDuration(job.getAge()));

        try {
            // Try to cancel the batch job
            batchClient.cancelBatch(job.batchId).get(10, TimeUnit.SECONDS);
            
            // Try to recover partial results
            Map<String, List<PredictionResult>> partialResults = recoverPartialResults(job);

            // Complete the future with timeout exception or partial results
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                if (!partialResults.isEmpty()) {
                    LOG.infof("Recovered %d partial results for timed out job %s", 
                            partialResults.size(), job.jobId);
                    job.completionFuture.complete(partialResults);
                } else {
                    job.completionFuture.completeExceptionally(
                        new RuntimeException("Batch job timed out after " + formatDuration(job.getAge())));
                }
            }

            // Send timeout notification
            sendBatchTimeoutNotification(job.analysisJobId, job.batchId, job.getAge());

            // Clean up resources
            cleanupJobResources(job);

        } catch (Exception e) {
            LOG.errorf(e, "Error during timeout handling for job %s: %s", job.jobId, e.getMessage());
            
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.completeExceptionally(
                    new RuntimeException("Batch job timed out with additional errors", e));
            }
            
            cleanupJobResources(job);
        }
    }

    /**
     * Attempts to recover partial results from a failed or cancelled job.
     * 
     * @param job the batch job
     * @return map of partial results, may be empty
     */
    private Map<String, List<PredictionResult>> recoverPartialResults(MonitoredBatchJob job) {
        try {
            LOG.debugf("Attempting to recover partial results for job: %s", job.jobId);

            // Try to get whatever results are available
            BatchResults batchResults = batchClient.getBatchResults(job.batchId).get(
                5, TimeUnit.SECONDS); // Short timeout for partial recovery

            Map<String, List<PredictionResult>> partialResults = parseResultsToPredictions(
                batchResults, job.originalArticles);

            LOG.infof("Recovered %d partial results for job %s", partialResults.size(), job.jobId);
            return partialResults;

        } catch (Exception e) {
            LOG.debugf("Could not recover partial results for job %s: %s", job.jobId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * Parses batch results into prediction format.
     * 
     * @param batchResults the batch results from GenAI API
     * @param originalArticles the original articles for context
     * @return map of article ID to predictions
     */
    private Map<String, List<PredictionResult>> parseResultsToPredictions(
            BatchResults batchResults, Map<String, ArticleData> originalArticles) {
        
        LOG.debugf("Parsing batch results: %d responses", batchResults.responses().size());
        
        Map<String, List<PredictionResult>> predictions = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, ai.falsify.prediction.model.BatchResponse> entry : 
             batchResults.responses().entrySet()) {
            
            String articleId = entry.getKey();
            ai.falsify.prediction.model.BatchResponse response = entry.getValue();
            
            try {
                if (response.success()) {
                    ArticleData originalArticle = originalArticles.get(articleId);
                    String originalText = originalArticle != null ? originalArticle.text() : "";
                    
                    // Parse the response using the same logic as GeminiNativePredictionExtractor
                    List<PredictionResult> articlePredictions = 
                        parsePredictionResponse(response.response(), originalText);
                    
                    predictions.put(articleId, articlePredictions);
                    
                    LOG.debugf("Parsed %d predictions for article %s", 
                            articlePredictions.size(), articleId);
                } else {
                    LOG.warnf("Article %s processing failed: %s", articleId, response.errorMessage());
                    predictions.put(articleId, List.of());
                }
                
            } catch (Exception e) {
                LOG.warnf("Failed to parse predictions for article %s: %s", articleId, e.getMessage());
                predictions.put(articleId, List.of());
            }
        }
        
        int totalPredictions = predictions.values().stream().mapToInt(List::size).sum();
        LOG.infof("Parsed batch results: %d articles, %d total predictions", 
                predictions.size(), totalPredictions);
        
        return predictions;
    }

    /**
     * Parses prediction response from GenAI API using the same logic as GeminiNativePredictionExtractor.
     * This is a simplified version - in production, this logic should be shared.
     * 
     * @param response the GenAI response text
     * @param originalText the original article text
     * @return list of parsed predictions
     */
    private List<PredictionResult> parsePredictionResponse(String response, String originalText) {
        if (response == null || response.trim().isEmpty()) {
            LOG.debug("GenAI response is null or empty");
            return List.of();
        }
        
        if (response.contains("NO_PREDICTIONS_FOUND")) {
            LOG.debug("No predictions found in GenAI response");
            return List.of();
        }
        
        List<PredictionResult> predictions = new java.util.ArrayList<>();
        
        // Split response by prediction separators
        String[] predictionBlocks = response.split("---");
        
        for (String block : predictionBlocks) {
            if (block.trim().isEmpty()) {
                continue;
            }
            
            try {
                PredictionResult prediction = parseSinglePrediction(block.trim());
                if (prediction != null) {
                    predictions.add(prediction);
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse prediction block: %s", e.getMessage());
            }
        }
        
        return predictions;
    }

    /**
     * Parses a single prediction from a text block.
     * This is a simplified version of the logic from GeminiNativePredictionExtractor.
     * 
     * @param block the prediction text block
     * @return parsed prediction result or null if parsing fails
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
                    java.math.BigDecimal.valueOf(confidence),
                    context != null ? context.trim() : "",
                    null, // timeframe
                    null  // subject
            );
            
        } catch (Exception e) {
            LOG.debugf("Error parsing single prediction: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a field value from a prediction block.
     * 
     * @param block the text block
     * @param fieldName the field name to extract
     * @return the field value or null if not found
     */
    private String extractField(String block, String fieldName) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            fieldName + ":\\s*(.+?)(?=\\n[A-Z_]+:|\\n---|$)", 
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Gets the total number of predictions across all articles.
     * 
     * @param predictions map of predictions
     * @return total prediction count
     */
    private int getTotalPredictionCount(Map<String, List<PredictionResult>> predictions) {
        return predictions.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    // Notification helper methods

    /**
     * Sends batch completed notification.
     */
    private void sendBatchCompletedNotification(String analysisJobId, String batchId, 
                                              int articleCount, int predictionCount) {
        try {
            LOG.infof("Batch completed: analysisJobId=%s, batchId=%s, articles=%d, predictions=%d", 
                    analysisJobId, batchId, articleCount, predictionCount);
        } catch (Exception e) {
            LOG.warnf("Failed to send batch completed notification: %s", e.getMessage());
        }
    }

    /**
     * Sends batch failed notification.
     */
    private void sendBatchFailedNotification(String analysisJobId, String batchId, String errorMessage) {
        try {
            LOG.warnf("Batch failed: analysisJobId=%s, batchId=%s, error=%s", 
                    analysisJobId, batchId, errorMessage);
        } catch (Exception e) {
            LOG.warnf("Failed to send batch failed notification: %s", e.getMessage());
        }
    }

    /**
     * Sends batch cancelled notification.
     */
    private void sendBatchCancelledNotification(String analysisJobId, String batchId) {
        try {
            LOG.infof("Batch cancelled: analysisJobId=%s, batchId=%s", analysisJobId, batchId);
        } catch (Exception e) {
            LOG.warnf("Failed to send batch cancelled notification: %s", e.getMessage());
        }
    }

    /**
     * Sends batch timeout notification.
     */
    private void sendBatchTimeoutNotification(String analysisJobId, String batchId, Duration age) {
        try {
            LOG.warnf("Batch timed out: analysisJobId=%s, batchId=%s, age=%s", 
                    analysisJobId, batchId, formatDuration(age));
        } catch (Exception e) {
            LOG.warnf("Failed to send batch timeout notification: %s", e.getMessage());
        }
    }

    // Resource management and cleanup methods

    /**
     * Performs comprehensive cleanup of job resources.
     * 
     * @param job the batch job to clean up
     */
    private void cleanupJobResources(MonitoredBatchJob job) {
        try {
            LOG.debugf("Cleaning up resources for job: %s", job.jobId);

            // Cancel any pending completion future
            if (job.completionFuture != null && !job.completionFuture.isDone()) {
                job.completionFuture.cancel(true);
            }

            // Try to cancel the batch job if it's still active
            if (job.lastKnownState == BatchState.PROCESSING || 
                job.lastKnownState == BatchState.SUBMITTED) {
                try {
                    batchClient.cancelBatch(job.batchId).get(5, TimeUnit.SECONDS);
                    LOG.debugf("Cancelled batch %s during cleanup", job.batchId);
                } catch (Exception e) {
                    LOG.debugf("Could not cancel batch %s during cleanup: %s", 
                            job.batchId, e.getMessage());
                }
            }

            // Remove from active jobs
            activeBatchJobs.remove(job.jobId);

            LOG.debugf("Completed resource cleanup for job: %s", job.jobId);

        } catch (Exception e) {
            LOG.warnf("Error during resource cleanup for job %s: %s", job.jobId, e.getMessage());
        }
    }

    /**
     * Forces cleanup of all active batch jobs (used during shutdown).
     */
    public void forceCleanupAllJobs() {
        LOG.infof("Force cleaning up %d active batch jobs", activeBatchJobs.size());

        List<MonitoredBatchJob> jobsToCleanup = new java.util.ArrayList<>(activeBatchJobs.values());
        
        for (MonitoredBatchJob job : jobsToCleanup) {
            try {
                // Complete futures with cancellation
                if (job.completionFuture != null && !job.completionFuture.isDone()) {
                    job.completionFuture.cancel(true);
                }

                // Send cancellation notification
                sendBatchCancelledNotification(job.analysisJobId, job.batchId);

            } catch (Exception e) {
                LOG.debugf("Error during force cleanup of job %s: %s", job.jobId, e.getMessage());
            }
        }

        activeBatchJobs.clear();
        LOG.info("Force cleanup of all batch jobs completed");
    }

    /**
     * Gets comprehensive monitoring statistics.
     * 
     * @return monitoring statistics map
     */
    public Map<String, Object> getMonitoringStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("isRunning", isRunning.get());
        stats.put("activeBatchJobs", activeBatchJobs.size());
        stats.put("monitoringEnabled", config.monitoringEnabled);
        stats.put("pollingIntervalSeconds", config.pollingIntervalSeconds);
        stats.put("maxConcurrentJobs", config.maxConcurrentJobs);
        stats.put("timeoutMinutes", config.timeoutMinutes);
        
        // Job state distribution
        Map<String, Long> stateDistribution = activeBatchJobs.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                job -> job.lastKnownState.toString(),
                java.util.stream.Collectors.counting()
            ));
        stats.put("jobStateDistribution", stateDistribution);
        
        // Age statistics
        if (!activeBatchJobs.isEmpty()) {
            List<Duration> ages = activeBatchJobs.values().stream()
                .map(MonitoredBatchJob::getAge)
                .sorted()
                .toList();
            
            stats.put("oldestJobAge", formatDuration(ages.get(ages.size() - 1)));
            stats.put("newestJobAge", formatDuration(ages.get(0)));
            stats.put("averageJobAge", formatDuration(Duration.ofMillis(
                (long) ages.stream().mapToLong(Duration::toMillis).average().orElse(0)
            )));
        }
        
        return stats;
    }
}