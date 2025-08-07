package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;
import ai.falsify.crawlers.common.model.AnalysisStatus;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.PredictionInstanceEntity;
import ai.falsify.prediction.model.PredictionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for managing prediction analysis jobs.
 * This service handles the orchestration of prediction extraction from articles,
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
    
    // Thread pool for async analysis jobs
    private final Executor analysisExecutor = Executors.newFixedThreadPool(5);
    
    // Track running jobs
    private final Map<String, CompletableFuture<Void>> runningJobs = new ConcurrentHashMap<>();
    
    /**
     * Start a new prediction analysis job for the given articles.
     * 
     * @param articleIds List of article IDs to analyze
     * @param analysisType Type of analysis ("mock" or "llm")
     * @return Created analysis job entity
     */
    @Transactional
    public AnalysisJobEntity startAnalysis(List<Long> articleIds, String analysisType) {
        LOG.infof("Starting prediction analysis for %d articles with type: %s", articleIds.size(), analysisType);
        
        // Check concurrent job limit
        if (runningJobs.size() >= maxConcurrentJobs) {
            throw new IllegalStateException("Maximum concurrent analysis jobs limit reached: " + maxConcurrentJobs);
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
        
        // Schedule async processing to start after current transaction commits
        CompletableFuture<Void> future = CompletableFuture.runAsync(
            () -> startAsyncProcessing(jobId), 
            analysisExecutor
        );
        
        runningJobs.put(jobId, future);
        
        // Handle completion
        future.whenComplete((result, throwable) -> {
            runningJobs.remove(jobId);
            if (throwable != null) {
                LOG.errorf(throwable, "Analysis job failed: %s", jobId);
                markJobFailed(jobId, throwable.getMessage());
            }
        });
        
        return job;
    }
    
    /**
     * Start async processing with proper transaction handling.
     * This method runs in the async thread and ensures proper transaction context.
     * 
     * @param jobId The job ID to process
     */
    @Transactional
    public void startAsyncProcessing(String jobId) {
        try {
            // Small delay to ensure the creating transaction is committed
            Thread.sleep(200);
            
            // Verify the job exists
            AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
            if (job == null) {
                LOG.errorf("Job not found during async processing: %s", jobId);
                return;
            }
            
            LOG.infof("Starting async processing for job: %s", jobId);
            processAnalysisJob(jobId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.errorf("Async processing interrupted for job: %s", jobId);
        } catch (Exception e) {
            LOG.errorf(e, "Error starting async processing for job: %s", jobId);
            markJobFailed(jobId, e.getMessage());
        }
    }
    
    /**
     * Process an analysis job asynchronously.
     * 
     * @param jobId The job ID to process
     */
    private void processAnalysisJob(String jobId) {
        try {
            // Update job status to running
            updateJobStatus(jobId, AnalysisStatus.RUNNING);
            
            // Get job and articles
            AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
            if (job == null) {
                LOG.errorf("Job not found: %s", jobId);
                return;
            }
            
            // Get prediction extractor
            BatchPredictionExtractor extractor = extractorFactory.getBatchExtractor(job.analysisType);
            if (extractor == null || !extractor.isAvailable()) {
                throw new IllegalStateException("Prediction extractor not available for type: " + job.analysisType);
            }
            
            // Prepare articles for batch processing
            Map<String, BatchPredictionExtractor.ArticleData> articleData = new HashMap<>();
            for (ArticleEntity article : job.analyzedArticles) {
                articleData.put(
                    article.id.toString(),
                    new BatchPredictionExtractor.ArticleData(
                        article.text,
                        article.title,
                        Map.of("url", article.url, "crawlerSource", article.crawlerSource)
                    )
                );
            }
            
            LOG.infof("Processing %d articles for job: %s", articleData.size(), jobId);
            
            // Extract predictions in batches
            Map<String, List<PredictionResult>> results = extractor.extractPredictionsBatch(articleData);
            
            // Store results
            int totalPredictions = storePredictionResults(job, results);
            
            // Update job completion
            updateJobCompletion(jobId, job.analyzedArticles.size(), totalPredictions);
            
            LOG.infof("Completed analysis job: %s - processed %d articles, found %d predictions", 
                     jobId, job.analyzedArticles.size(), totalPredictions);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing analysis job: %s", jobId);
            markJobFailed(jobId, e.getMessage());
        }
    }
    
    /**
     * Store prediction results in the database.
     * 
     * @param job The analysis job
     * @param results Map of article ID to prediction results
     * @return Total number of predictions stored
     */
    @Transactional
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
                    predictionResult.predictionType()
                );
                
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
     * @param jobId Job ID
     * @param status New status
     */
    @Transactional
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
     * @param jobId Job ID
     * @param processedArticles Number of processed articles
     * @param predictionsFound Number of predictions found
     */
    @Transactional
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
     * 
     * @param jobId Job ID
     * @param errorMessage Error message
     */
    @Transactional
    public void markJobFailed(String jobId, String errorMessage) {
        AnalysisJobEntity job = AnalysisJobEntity.find("jobId", jobId).firstResult();
        if (job != null) {
            job.status = AnalysisStatus.FAILED;
            job.completedAt = Instant.now();
            job.errorMessage = errorMessage;
            job.persist();
            
            notificationService.sendJobFailed(jobId, errorMessage);
            LOG.errorf("Job %s failed: %s", jobId, errorMessage);
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
        Map<AnalysisStatus, Long> jobCounts = AnalysisJobEntity.find("SELECT status, COUNT(*) FROM AnalysisJobEntity GROUP BY status")
                .project(Object[].class)
                .stream()
                .collect(Collectors.toMap(
                    row -> (AnalysisStatus) row[0],
                    row -> (Long) row[1]
                ));
        
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
            "configuration", primaryExtractor.getConfiguration()
        ));
        
        // Get best available extractor info
        var bestExtractor = extractorFactory.getBestAvailableExtractor();
        status.put("bestAvailableExtractor", Map.of(
            "type", bestExtractor.getExtractorType(),
            "available", bestExtractor.isAvailable(),
            "configuration", bestExtractor.getConfiguration()
        ));
        
        return status;
    }
    
    /**
     * Test prediction extraction with sample text.
     * 
     * @param text Sample text to analyze
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
                "configuration", extractor.getConfiguration()
            ));
            
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
                "configuration", geminiExtractor.getConfiguration()
            ));
        }
        
        var mockExtractor = extractorFactory.getExtractorByType("mock");
        if (mockExtractor != null) {
            config.put("mockExtractor", Map.of(
                "type", mockExtractor.getExtractorType(),
                "available", mockExtractor.isAvailable(),
                "configuration", mockExtractor.getConfiguration()
            ));
        }
        
        return config;
    }
}