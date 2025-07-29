package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.JobRecord;
import ai.falsify.crawlers.model.JobStatus;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;
import io.quarkus.runtime.StartupEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for tracking crawler job lifecycle and managing job history.
 * Provides functionality to start, update, complete jobs and maintain job records.
 */
@ApplicationScoped
public class JobTrackerService {
    
    @Inject
    WebSocketNotificationService webSocketService;
    
    // In-memory cache for active jobs to avoid frequent DB queries
    private final ConcurrentHashMap<String, JobRecord> activeJobs = new ConcurrentHashMap<>();
    
    /**
     * Starts tracking a new crawler job.
     */
    @Transactional
    public JobRecord trackJobStart(String crawlerId, String requestId) {
        String jobId = generateJobId();
        
        JobRecord jobRecord = new JobRecord(crawlerId, jobId, requestId);
        jobRecord.persist();
        
        // Cache the active job
        activeJobs.put(jobId, jobRecord);
        
        Log.infof("Started tracking job %s for crawler %s", jobId, crawlerId);
        
        // Notify WebSocket clients
        JobStatus jobStatus = JobStatus.fromJobRecord(jobRecord);
        webSocketService.broadcastJobStarted(jobStatus);
        
        return jobRecord;
    }
    
    /**
     * Updates the progress of a running job.
     */
    @Transactional
    public void trackJobProgress(String jobId, int articlesProcessed, int articlesSkipped, 
                                int articlesFailed, String currentActivity) {
        // Always fetch from database to ensure we have a managed entity
        JobRecord jobRecord = JobRecord.findByJobId(jobId);
        if (jobRecord == null) {
            Log.warnf("Attempted to update progress for unknown job: %s", jobId);
            return;
        }
        
        if (!jobRecord.isRunning()) {
            Log.warnf("Attempted to update progress for non-running job: %s (status: %s)", 
                     jobId, jobRecord.status);
            return;
        }
        
        jobRecord.updateProgress(articlesProcessed, articlesSkipped, articlesFailed, currentActivity);
        // No need to call persist() - the entity is managed and changes will be automatically persisted
        
        // Update cache with the managed entity
        activeJobs.put(jobId, jobRecord);
        
        Log.debugf("Updated progress for job %s: processed=%d, skipped=%d, failed=%d, activity=%s",
                  jobId, articlesProcessed, articlesSkipped, articlesFailed, currentActivity);
        
        // Notify WebSocket clients
        JobStatus jobStatus = JobStatus.fromJobRecord(jobRecord);
        webSocketService.broadcastJobProgress(jobStatus);
    }
    
    /**
     * Marks a job as completed successfully.
     */
    @Transactional
    public void trackJobCompletion(String jobId, int finalArticlesProcessed, 
                                  int finalArticlesSkipped, int finalArticlesFailed) {
        // Always fetch from database to ensure we have a managed entity
        JobRecord jobRecord = JobRecord.findByJobId(jobId);
        if (jobRecord == null) {
            Log.warnf("Attempted to complete unknown job: %s", jobId);
            return;
        }
        
        jobRecord.markCompleted(finalArticlesProcessed, finalArticlesSkipped, finalArticlesFailed);
        // No need to call persist() - the entity is managed and changes will be automatically persisted
        
        // Remove from active jobs cache
        activeJobs.remove(jobId);
        
        Log.infof("Completed job %s for crawler %s: processed=%d, skipped=%d, failed=%d, duration=%dms",
                 jobId, jobRecord.crawlerId, finalArticlesProcessed, finalArticlesSkipped, 
                 finalArticlesFailed, jobRecord.getDurationMs());
        
        // Notify WebSocket clients
        JobStatus jobStatus = JobStatus.fromJobRecord(jobRecord);
        webSocketService.broadcastJobCompleted(jobStatus);
    }
    
    /**
     * Marks a job as failed with an error message.
     */
    @Transactional
    public void trackJobFailure(String jobId, String errorMessage) {
        // Always fetch from database to ensure we have a managed entity
        JobRecord jobRecord = JobRecord.findByJobId(jobId);
        if (jobRecord == null) {
            Log.warnf("Attempted to fail unknown job: %s", jobId);
            return;
        }
        
        jobRecord.markFailed(errorMessage);
        // No need to call persist() - the entity is managed and changes will be automatically persisted
        
        // Remove from active jobs cache
        activeJobs.remove(jobId);
        
        Log.warnf("Failed job %s for crawler %s: %s", jobId, jobRecord.crawlerId, errorMessage);
        
        // Notify WebSocket clients
        JobStatus jobStatus = JobStatus.fromJobRecord(jobRecord);
        webSocketService.broadcastJobFailed(jobStatus);
    }
    
    /**
     * Cancels a running job.
     */
    @Transactional
    public void cancelJob(String jobId) {
        // Always fetch from database to ensure we have a managed entity
        JobRecord jobRecord = JobRecord.findByJobId(jobId);
        if (jobRecord == null) {
            Log.warnf("Attempted to cancel unknown job: %s", jobId);
            return;
        }
        
        if (!jobRecord.isRunning()) {
            Log.warnf("Attempted to cancel non-running job: %s (status: %s)", jobId, jobRecord.status);
            return;
        }
        
        jobRecord.markCancelled();
        // No need to call persist() - the entity is managed and changes will be automatically persisted
        
        // Remove from active jobs cache
        activeJobs.remove(jobId);
        
        Log.infof("Cancelled job %s for crawler %s", jobId, jobRecord.crawlerId);
        
        // Notify WebSocket clients
        JobStatus jobStatus = JobStatus.fromJobRecord(jobRecord);
        webSocketService.broadcastJobFailed(jobStatus); // Use failed event for cancelled jobs
    }
    
    /**
     * Gets the recent jobs for a specific crawler (last 5 jobs).
     */
    public List<JobStatus> getRecentJobs(String crawlerId) {
        List<JobRecord> jobRecords = JobRecord.find("crawlerId = ?1 ORDER BY startTime DESC", crawlerId)
                .page(0, 5)
                .list();
        
        return jobRecords.stream()
                .map(JobStatus::fromJobRecord)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all jobs for a specific crawler with pagination.
     */
    public List<JobStatus> getJobHistory(String crawlerId, int page, int size) {
        List<JobRecord> jobRecords = JobRecord.find("crawlerId = ?1 ORDER BY startTime DESC", crawlerId)
                .page(page, size)
                .list();
        
        return jobRecords.stream()
                .map(JobStatus::fromJobRecord)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a specific job by ID.
     */
    public Optional<JobStatus> getJob(String jobId) {
        JobRecord jobRecord = JobRecord.findByJobId(jobId);
        return jobRecord != null ? Optional.of(JobStatus.fromJobRecord(jobRecord)) : Optional.empty();
    }
    
    /**
     * Gets all currently running jobs.
     */
    public List<JobStatus> getRunningJobs() {
        List<JobRecord> runningJobs = JobRecord.find("status = ?1", JobRecord.JobStatus.RUNNING).list();
        return runningJobs.stream()
                .map(JobStatus::fromJobRecord)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all currently running jobs for a specific crawler.
     */
    public List<JobStatus> getRunningJobs(String crawlerId) {
        List<JobRecord> runningJobs = JobRecord.find("crawlerId = ?1 AND status = ?2", 
                                                    crawlerId, JobRecord.JobStatus.RUNNING).list();
        return runningJobs.stream()
                .map(JobStatus::fromJobRecord)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a crawler has any running jobs.
     */
    public boolean hasRunningJobs(String crawlerId) {
        return JobRecord.count("crawlerId = ?1 AND status = ?2", crawlerId, JobRecord.JobStatus.RUNNING) > 0;
    }
    
    /**
     * Scheduled cleanup of old job records.
     * Runs daily and removes job records older than the configured retention period.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupOldJobs() {
        // Get retention days from configuration (default 30 days)
        int retentionDays = 30; // TODO: Make this configurable
        
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        long deletedCount = JobRecord.delete("startTime < ?1", cutoffDate);
        
        if (deletedCount > 0) {
            Log.infof("Cleaned up %d old job records older than %d days", deletedCount, retentionDays);
        }
    }
    
    /**
     * Scheduled check for stale running jobs.
     * Runs every 10 minutes and marks jobs as failed if they haven't been updated recently.
     */
    @Scheduled(every = "10m")
    @Transactional
    public void checkStaleJobs() {
        // Jobs are considered stale if they haven't been updated in the last hour
        Instant staleThreshold = Instant.now().minus(1, ChronoUnit.HOURS);
        
        List<JobRecord> staleJobs = JobRecord.find("status = ?1 AND lastUpdated < ?2", 
                                                  JobRecord.JobStatus.RUNNING, staleThreshold).list();
        
        for (JobRecord job : staleJobs) {
            job.markFailed("Job timed out - no updates received from crawler");
            // Remove from active jobs cache
            activeJobs.remove(job.jobId);
            
            Log.warnf("Marked stale job %s as failed (last updated: %s)", job.jobId, job.lastUpdated);
            
            // Notify WebSocket clients
            JobStatus jobStatus = JobStatus.fromJobRecord(job);
            webSocketService.broadcastJobFailed(jobStatus);
        }
        
        if (!staleJobs.isEmpty()) {
            Log.infof("Marked %d stale jobs as failed", staleJobs.size());
        }
    }
    
    /**
     * Recovers jobs that were running when the service was restarted.
     * Marks them as failed with a specific message.
     */
    @Transactional
    public void recoverOrphanedJobs() {
        List<JobRecord> orphanedJobs = JobRecord.find("status = ?1", JobRecord.JobStatus.RUNNING).list();
        
        for (JobRecord job : orphanedJobs) {
            job.markFailed("Job was interrupted by service restart");
            // No need to call persist() - the entity is managed and changes will be automatically persisted
            Log.warnf("Recovered orphaned job %s for crawler %s", job.jobId, job.crawlerId);
        }
        
        if (!orphanedJobs.isEmpty()) {
            Log.infof("Recovered %d orphaned jobs after service restart", orphanedJobs.size());
        }
        
        // Clear the active jobs cache
        activeJobs.clear();
    }
    

    
    /**
     * Handles application startup to recover orphaned jobs.
     */
    void onStart(@Observes StartupEvent ev) {
        Log.info("JobTrackerService starting up, recovering orphaned jobs...");
        recoverOrphanedJobs();
    }
    
    private String generateJobId() {
        return "job-" + UUID.randomUUID().toString();
    }
}