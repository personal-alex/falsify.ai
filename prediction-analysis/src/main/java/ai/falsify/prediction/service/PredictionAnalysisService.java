package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;
import ai.falsify.crawlers.common.model.AnalysisStatus;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.PredictionInstanceEntity;
import ai.falsify.prediction.model.PredictionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;

/**
 * Service for managing prediction analysis jobs.
 * This service handles the orchestration of prediction extraction from
 * articles,
 * job lifecycle management, and result storage.
 */
@ApplicationScoped
public class PredictionAnalysisService {

    private static final Logger LOG = Logger.getLogger(PredictionAnalysisService.class);

    @Inject
    PredictionAnalysisExtractorFactory extractorFactory;

    @Inject
    AnalysisNotificationService notificationService;

    @ConfigProperty(name = "prediction.analysis.max-concurrent-jobs", defaultValue = "3")
    int maxConcurrentJobs;

    @ConfigProperty(name = "prediction.analysis.job-timeout-minutes", defaultValue = "30")
    int jobTimeoutMinutes;

    // Track running jobs for cancellation support
    private final Map<String, CompletableFuture<Void>> runningJobs = new ConcurrentHashMap<>();

    // CDI event for triggering processing after transaction commit
    @Inject
    Event<AnalysisJobCreatedEvent> jobCreatedEvent;

    /**
     * Start a new prediction analysis job for the given articles.
     * 
     * The async processing is scheduled to start AFTER the current transaction
     * commits
     * to avoid transaction conflicts and ensure the job is properly persisted.
     * 
     * @param articleIds   List of article IDs to analyze
     * @param analysisType Type of analysis ("mock" or "llm")
     * @return Created analysis job entity
     */
    @Transactional
    public AnalysisJobEntity startAnalysis(List<Long> articleIds, String analysisType) {
        LOG.infof("Starting prediction analysis for %d articles with type: %s", articleIds.size(), analysisType);

        // Check for duplicate running jobs with the same articles
        List<AnalysisJobEntity> runningJobs = AnalysisJobEntity.find(
                "SELECT j FROM AnalysisJobEntity j LEFT JOIN FETCH j.analyzedArticles WHERE j.status in (?1, ?2)", 
                AnalysisStatus.PENDING, AnalysisStatus.RUNNING).list();
        
        for (AnalysisJobEntity runningJob : runningJobs) {
            if (runningJob.analyzedArticles != null && 
                runningJob.analyzedArticles.size() == articleIds.size()) {
                
                Set<Long> runningArticleIds = runningJob.analyzedArticles.stream()
                    .map(article -> article.id)
                    .collect(Collectors.toSet());
                Set<Long> requestedArticleIds = new HashSet<>(articleIds);
                
                if (runningArticleIds.equals(requestedArticleIds)) {
                    LOG.warnf("Duplicate analysis request detected. Job %s is already processing the same articles", 
                             runningJob.jobId);
                    throw new IllegalStateException(
                        String.format("Analysis is already running for these articles. " +
                                     "Job ID: %s, Status: %s, Started: %s", 
                                     runningJob.jobId, runningJob.status, runningJob.startedAt));
                }
            }
        }

        // Validate articles exist
        List<ArticleEntity> articles = ArticleEntity.list("id in ?1", articleIds);
        if (articles.size() != articleIds.size()) {
            throw new IllegalArgumentException("Some articles not found in database");
        }

        // Create analysis job
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.jobId = UUID.randomUUID().toString();
        job.status = AnalysisStatus.PENDING;
        job.startedAt = Instant.now();
        job.totalArticles = articles.size();
        job.processedArticles = 0;
        job.predictionsFound = 0;
        job.analysisType = analysisType;
        job.analyzedArticles = articles;

        job.persist();

        LOG.infof("Created analysis job: %s for %d articles", job.jobId, job.totalArticles);

        // Store the job ID for async processing after transaction commits
        String jobId = job.jobId;

        // Fire CDI event that will be processed AFTER transaction commits
        // This is the proper Quarkus way to handle post-transaction processing
        jobCreatedEvent.fire(new AnalysisJobCreatedEvent(jobId));

        return job;
    }

    /**
     * Process an analysis job asynchronously.
     * Uses granular transactions for database operations only.
     * 
     * @param jobId The job ID to process
     */
    void processAnalysisJob(String jobId) {
        try {
            // Get job and articles (short transaction)
            AnalysisJobEntity job = getJobForProcessing(jobId);
            if (job == null) {
                LOG.errorf("Job not found: %s", jobId);
                return;
            }

            // Update job status to running (short transaction)
            updateJobStatusTransactional(jobId, AnalysisStatus.RUNNING);
            notificationService.sendJobStatusUpdate(jobId, AnalysisStatus.RUNNING);
            LOG.debugf("Updated job %s status to: %s", jobId, AnalysisStatus.RUNNING);

            // Get prediction extractor (no transaction needed)
            BatchPredictionExtractor extractor = extractorFactory.getBatchExtractor(job.analysisType);
            if (extractor == null || !extractor.isAvailable()) {
                throw new IllegalStateException("Prediction extractor not available for type: " + job.analysisType);
            }

            // Prepare articles for batch processing (no transaction needed)
            Map<String, BatchPredictionExtractor.ArticleData> articleData = new HashMap<>();
            for (ArticleEntity article : job.analyzedArticles) {
                articleData.put(
                        article.id.toString(),
                        new BatchPredictionExtractor.ArticleData(
                                article.text,
                                article.title,
                                Map.of("url", article.url, "crawlerSource", article.crawlerSource)));
            }

            int articleCount = articleData.size();
            LOG.infof("Processing %d articles for job: %s", articleCount, jobId);

            // Extract predictions in batches (NO TRANSACTION - this is the long-running
            // operation)
            Map<String, List<PredictionResult>> results = extractor.extractPredictionsBatch(articleData);

            // Store results (transactional)
            int totalPredictions = storePredictionResultsTransactional(jobId, results);

            // Update job completion (short transaction)
            completeJob(jobId, articleCount, totalPredictions);

            notificationService.sendJobCompleted(jobId, articleCount, totalPredictions);
            LOG.infof("Completed analysis job: %s - processed %d articles, found %d predictions",
                    jobId, articleCount, totalPredictions);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing analysis job: %s", jobId);
            // Mark job as failed (short transaction)
            markJobFailed(jobId, e.getMessage());
            notificationService.sendJobFailed(jobId, e.getMessage());
            LOG.errorf("Job %s failed: %s", jobId, e.getMessage());
            throw e; // Re-throw to trigger transaction rollback, then handle in outer catch
        }
    }

    /**
     * Store prediction results in the database.
     * 
     * @param job     The analysis job
     * @param results Map of article ID to prediction results
     * @return Total number of predictions stored
     */
    public int storePredictionResults(AnalysisJobEntity job, Map<String, List<PredictionResult>> results) {
        int totalPredictions = 0;

        for (Map.Entry<String, List<PredictionResult>> entry : results.entrySet()) {
            Long articleId = Long.parseLong(entry.getKey());
            List<PredictionResult> predictions = entry.getValue();

            ArticleEntity article = ArticleEntity.findById(articleId);
            if (article == null) {
                LOG.warnf("Article not found for ID: %s", articleId);
                continue;
            }

            for (PredictionResult predictionResult : predictions) {
                // Find or create prediction entity
                var predictionEntity = ai.falsify.crawlers.common.model.PredictionEntity.findOrCreate(
                        predictionResult.predictionText(),
                        predictionResult.predictionType());

                // Create prediction instance
                PredictionInstanceEntity instance = new PredictionInstanceEntity();
                instance.prediction = predictionEntity;
                instance.article = article;
                instance.analysisJob = job;
                instance.confidenceScore = predictionResult.confidenceScore();
                instance.rating = predictionResult.rating();
                instance.context = predictionResult.context();
                instance.extractedAt = Instant.now();

                instance.persist();
                totalPredictions++;
            }

            // Send progress update
            notificationService.sendProgressUpdate(job.jobId, entry.getKey(), predictions.size());
        }

        return totalPredictions;
    }

    /**
     * Update job status.
     * 
     * @param jobId  Job ID
     * @param status New status
     */
    public void updateJobStatus(String jobId, AnalysisStatus status) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = status;
            job.persist();

            notificationService.sendJobStatusUpdate(jobId, status);
            LOG.debugf("Updated job %s status to: %s", jobId, status);
        }
    }

    /**
     * Mark job as completed.
     * 
     * @param jobId             Job ID
     * @param processedArticles Number of processed articles
     * @param predictionsFound  Number of predictions found
     */
    public void updateJobCompletion(String jobId, int processedArticles, int predictionsFound) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = AnalysisStatus.COMPLETED;
            job.completedAt = Instant.now();
            job.processedArticles = processedArticles;
            job.predictionsFound = predictionsFound;
            job.persist();

            notificationService.sendJobCompleted(jobId, processedArticles, predictionsFound);
            LOG.infof("Job %s completed: %d articles processed, %d predictions found",
                    jobId, processedArticles, predictionsFound);
        }
    }

    /**
     * Mark job as failed.
     * Runs in its own transaction - used for async error handling.
     * 
     * @param jobId        Job ID
     * @param errorMessage Error message
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markJobFailed(String jobId, String errorMessage) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = AnalysisStatus.FAILED;
            job.completedAt = Instant.now();
            job.errorMessage = errorMessage;
            job.persist();
        }
    }

    /**
     * Cancel a running analysis job.
     * 
     * @param jobId Job ID to cancel
     * @return true if job was cancelled, false if not found or not running
     */
    @Transactional
    public boolean cancelJob(String jobId) {
        CompletableFuture<Void> future = runningJobs.get(jobId);
        if (future != null) {
            future.cancel(true);
            runningJobs.remove(jobId);

            AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
            if (job != null) {
                job.status = AnalysisStatus.CANCELLED;
                job.completedAt = Instant.now();
                job.persist();

                notificationService.sendJobCancelled(jobId);
                LOG.infof("Job %s cancelled", jobId);
                return true;
            }
        }
        return false;
    }

    /**
     * Get analysis job by ID.
     * 
     * @param jobId Job ID
     * @return Analysis job entity or null if not found
     */
    public AnalysisJobEntity getJob(String jobId) {
        return AnalysisJobEntity.find("jobId", jobId).firstResult();
    }

    /**
     * Get analysis job history with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of analysis jobs
     */
    public List<AnalysisJobEntity> getJobHistory(int page, int size) {
        return AnalysisJobEntity.find("ORDER BY startedAt DESC")
                .page(page, size)
                .list();
    }

    /**
     * Get prediction results for a completed job.
     * 
     * @param jobId Job ID
     * @return List of prediction instances
     */
    public List<PredictionInstanceEntity> getJobResults(String jobId) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job == null) {
            return Collections.emptyList();
        }

        return PredictionInstanceEntity.find("analysisJob", job).list();
    }

    /**
     * Get current system status.
     * 
     * @return Status information
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("runningJobs", runningJobs.size());
        status.put("maxConcurrentJobs", maxConcurrentJobs);
        status.put("availableSlots", maxConcurrentJobs - runningJobs.size());

        // Get job counts by status
        Map<AnalysisStatus, Long> jobCounts = AnalysisJobEntity
                .find("SELECT status, COUNT(*) FROM AnalysisJobEntity GROUP BY status")
                .project(Object[].class)
                .stream()
                .collect(Collectors.toMap(
                        row -> (AnalysisStatus) row[0],
                        row -> (Long) row[1]));

        status.put("jobCounts", jobCounts);

        return status;
    }

    /**
     * Get extractor status for debugging.
     * 
     * @return Extractor status information
     */
    public Map<String, Object> getExtractorStatus() {
        Map<String, Object> status = new HashMap<>();

        // Get extractor factory status
        status.put("extractors", extractorFactory.getExtractorStatus());

        // Get primary extractor info
        var primaryExtractor = extractorFactory.getPrimaryExtractor();
        status.put("primaryExtractor", Map.of(
                "type", primaryExtractor.getExtractorType(),
                "available", primaryExtractor.isAvailable(),
                "configuration", primaryExtractor.getConfiguration()));

        // Get best available extractor info
        var bestExtractor = extractorFactory.getBestAvailableExtractor();
        status.put("bestAvailableExtractor", Map.of(
                "type", bestExtractor.getExtractorType(),
                "available", bestExtractor.isAvailable(),
                "configuration", bestExtractor.getConfiguration()));

        return status;
    }

    /**
     * Test prediction extraction with sample text.
     * 
     * @param text  Sample text to analyze
     * @param title Sample title
     * @return Test results
     */
    public Map<String, Object> testPredictionExtraction(String text, String title) {
        LOG.infof("Testing prediction extraction with text: %s", title);

        Map<String, Object> result = new HashMap<>();

        try {
            // Get the best available extractor
            var extractor = extractorFactory.getBestAvailableExtractor();
            result.put("extractorUsed", Map.of(
                    "type", extractor.getExtractorType(),
                    "available", extractor.isAvailable(),
                    "configuration", extractor.getConfiguration()));

            // Extract predictions
            long startTime = System.currentTimeMillis();
            var predictions = extractor.extractPredictions(text, title);
            long duration = System.currentTimeMillis() - startTime;

            result.put("predictions", predictions);
            result.put("predictionCount", predictions.size());
            result.put("processingTimeMs", duration);
            result.put("success", true);

            LOG.infof("Test extraction completed: found %d predictions in %d ms using %s extractor",
                    predictions.size(), duration, extractor.getExtractorType());

        } catch (Exception e) {
            LOG.errorf(e, "Test extraction failed");
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * Get configuration details for debugging.
     * 
     * @return Configuration details
     */
    public Map<String, Object> getConfigurationDetails() {
        Map<String, Object> config = new HashMap<>();

        // Get extractor factory details
        config.put("extractorFactory", extractorFactory.getExtractorStatus());

        // Add preferred analysis type from configuration
        config.put("preferredAnalysisType", "llm"); // Based on prediction.extractor.prefer-llm=true
        config.put("defaultExtractorType", "gemini"); // Based on prediction.extractor.type=gemini

        // Get individual extractor details
        var geminiExtractor = extractorFactory.getExtractorByType("gemini");
        if (geminiExtractor != null) {
            config.put("geminiExtractor", Map.of(
                    "type", geminiExtractor.getExtractorType(),
                    "available", geminiExtractor.isAvailable(),
                    "configuration", geminiExtractor.getConfiguration()));
        }

        var mockExtractor = extractorFactory.getExtractorByType("mock");
        if (mockExtractor != null) {
            config.put("mockExtractor", Map.of(
                    "type", mockExtractor.getExtractorType(),
                    "available", mockExtractor.isAvailable(),
                    "configuration", mockExtractor.getConfiguration()));
        }

        return config;
    }

    /**
     * Event class for job creation notifications.
     * This event is fired after a job is created and will be processed after
     * transaction commit.
     */
    public static class AnalysisJobCreatedEvent {
        private final String jobId;

        public AnalysisJobCreatedEvent(String jobId) {
            this.jobId = jobId;
        }

        public String getJobId() {
            return jobId;
        }
    }

    /**
     * CDI event observer that handles job processing after transaction commits.
     * The @Observes(during = TransactionPhase.AFTER_SUCCESS) ensures this runs
     * only after the transaction that created the job has successfully committed.
     * 
     * @param event The job created event
     */
    public void onJobCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) AnalysisJobCreatedEvent event) {
        String jobId = event.getJobId();
        LOG.infof("Starting processing for job after transaction commit: %s", jobId);

        try {
            // Process the job directly - no transaction needed here
            processAnalysisJob(jobId);
            LOG.infof("Analysis job completed successfully: %s", jobId);

        } catch (Exception e) {
            LOG.errorf(e, "Analysis job failed: %s", jobId);
            // Mark as failed in separate transaction since this one will rollback
            markJobFailed(jobId, e.getMessage());
        }
    }

    /**
     * Get job for processing (short transaction).
     * 
     * @param jobId Job ID
     * @return Job entity or null if not found
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    AnalysisJobEntity getJobForProcessing(String jobId) {
        // Use JOIN FETCH to eagerly load the analyzedArticles collection
        List<AnalysisJobEntity> jobs = AnalysisJobEntity.find(
            "SELECT j FROM AnalysisJobEntity j LEFT JOIN FETCH j.analyzedArticles WHERE j.jobId = ?1", 
            jobId).list();
        
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    /**
     * Update job status (short transaction).
     * 
     * @param jobId  Job ID
     * @param status New status
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void updateJobStatusTransactional(String jobId, AnalysisStatus status) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = status;
            job.persist();
        }
    }

    /**
     * Store prediction results (transactional).
     * 
     * @param jobId   Job ID
     * @param results Prediction results
     * @return Total predictions stored
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    int storePredictionResultsTransactional(String jobId, Map<String, List<PredictionResult>> results) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job == null) {
            LOG.warnf("Job not found when storing results: %s", jobId);
            return 0;
        }
        return storePredictionResults(job, results);
    }

    /**
     * Complete job (short transaction).
     * 
     * @param jobId             Job ID
     * @param processedArticles Number of processed articles
     * @param totalPredictions  Total predictions found
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void completeJob(String jobId, int processedArticles, int totalPredictions) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = AnalysisStatus.COMPLETED;
            job.completedAt = Instant.now();
            job.processedArticles = processedArticles;
            job.predictionsFound = totalPredictions;
            job.persist();
        }
    }

}