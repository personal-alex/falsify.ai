package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service responsible for cleaning up old analysis jobs to maintain database performance.
 * Runs on a scheduled basis to remove completed, failed, or cancelled jobs older than a configured threshold.
 */
@ApplicationScoped
public class AnalysisJobCleanupService {

    @ConfigProperty(name = "analysis.cleanup.enabled", defaultValue = "true")
    boolean cleanupEnabled;

    @ConfigProperty(name = "analysis.cleanup.retention-days", defaultValue = "30")
    int retentionDays;

    @ConfigProperty(name = "analysis.cleanup.batch-size", defaultValue = "100")
    int batchSize;

    /**
     * Scheduled cleanup job that runs daily at 2 AM.
     * Can be disabled by setting analysis.cleanup.enabled=false
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "analysis-job-cleanup")
    @Transactional
    public void cleanupOldJobs() {
        if (!cleanupEnabled) {
            Log.debug("Analysis job cleanup is disabled");
            return;
        }

        try {
            Log.info("Starting analysis job cleanup process...");
            
            // First, count how many jobs are eligible for cleanup
            long eligibleCount = AnalysisJobEntity.countJobsEligibleForCleanup(retentionDays);
            
            if (eligibleCount == 0) {
                Log.info("No analysis jobs eligible for cleanup");
                return;
            }

            Log.infof("Found %d analysis jobs eligible for cleanup (older than %d days)", 
                    eligibleCount, retentionDays);

            // Perform the cleanup
            long deletedCount = AnalysisJobEntity.deleteOldJobs(retentionDays);
            
            Log.infof("Successfully cleaned up %d old analysis jobs", deletedCount);
            
            if (deletedCount != eligibleCount) {
                Log.warnf("Expected to delete %d jobs but actually deleted %d. " +
                        "This might indicate concurrent modifications or constraint issues.", 
                        eligibleCount, deletedCount);
            }

        } catch (Exception e) {
            Log.error("Error during analysis job cleanup", e);
            // Don't rethrow - we don't want to break the scheduler
        }
    }

    /**
     * Manual cleanup method that can be called programmatically.
     * Useful for testing or manual maintenance operations.
     * 
     * @return the number of jobs deleted
     */
    @Transactional
    public long performManualCleanup() {
        return performManualCleanup(retentionDays);
    }

    /**
     * Manual cleanup method with custom retention period.
     * 
     * @param customRetentionDays number of days to retain jobs
     * @return the number of jobs deleted
     */
    @Transactional
    public long performManualCleanup(int customRetentionDays) {
        if (customRetentionDays <= 0) {
            throw new IllegalArgumentException("Retention days must be positive");
        }

        Log.infof("Performing manual cleanup of analysis jobs older than %d days", customRetentionDays);
        
        try {
            long deletedCount = AnalysisJobEntity.deleteOldJobs(customRetentionDays);
            Log.infof("Manual cleanup completed: %d jobs deleted", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            Log.error("Error during manual analysis job cleanup", e);
            throw new RuntimeException("Failed to perform manual cleanup", e);
        }
    }

    /**
     * Gets information about jobs eligible for cleanup without actually deleting them.
     * 
     * @return cleanup statistics
     */
    public CleanupStats getCleanupStats() {
        return getCleanupStats(retentionDays);
    }

    /**
     * Gets information about jobs eligible for cleanup with custom retention period.
     * 
     * @param customRetentionDays number of days to check
     * @return cleanup statistics
     */
    public CleanupStats getCleanupStats(int customRetentionDays) {
        if (customRetentionDays <= 0) {
            throw new IllegalArgumentException("Retention days must be positive");
        }

        long eligibleCount = AnalysisJobEntity.countJobsEligibleForCleanup(customRetentionDays);
        long totalJobs = AnalysisJobEntity.count();
        
        return new CleanupStats(eligibleCount, totalJobs, customRetentionDays, cleanupEnabled);
    }

    /**
     * Statistics about cleanup eligibility.
     */
    public static class CleanupStats {
        public final long eligibleForCleanup;
        public final long totalJobs;
        public final int retentionDays;
        public final boolean cleanupEnabled;

        public CleanupStats(long eligibleForCleanup, long totalJobs, int retentionDays, boolean cleanupEnabled) {
            this.eligibleForCleanup = eligibleForCleanup;
            this.totalJobs = totalJobs;
            this.retentionDays = retentionDays;
            this.cleanupEnabled = cleanupEnabled;
        }

        public double getCleanupPercentage() {
            if (totalJobs == 0) {
                return 0.0;
            }
            return (eligibleForCleanup * 100.0) / totalJobs;
        }

        @Override
        public String toString() {
            return String.format("CleanupStats{eligible=%d, total=%d, retention=%d days, enabled=%s, percentage=%.1f%%}",
                    eligibleForCleanup, totalJobs, retentionDays, cleanupEnabled, getCleanupPercentage());
        }
    }
}