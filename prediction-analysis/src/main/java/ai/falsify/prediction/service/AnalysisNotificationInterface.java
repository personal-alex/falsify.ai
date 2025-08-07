package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;

/**
 * Interface for sending analysis job notifications.
 * This allows the common module to send notifications without depending on specific implementations.
 */
public interface AnalysisNotificationInterface {
    
    /**
     * Notifies that an analysis job has started.
     * 
     * @param job the analysis job that started
     */
    void notifyJobStarted(AnalysisJobEntity job);
    
    /**
     * Notifies about analysis job progress updates.
     * 
     * @param job the analysis job with updated progress
     */
    void notifyJobProgress(AnalysisJobEntity job);
    
    /**
     * Notifies that an analysis job has completed successfully.
     * 
     * @param job the completed analysis job
     */
    void notifyJobCompleted(AnalysisJobEntity job);
    
    /**
     * Notifies that an analysis job has failed.
     * 
     * @param job the failed analysis job
     */
    void notifyJobFailed(AnalysisJobEntity job);
    
    /**
     * Notifies that an analysis job has been cancelled.
     * 
     * @param job the cancelled analysis job
     */
    void notifyJobCancelled(AnalysisJobEntity job);
    
    /**
     * Notifies about a new prediction being extracted.
     * 
     * @param jobId the job ID
     * @param predictionText the prediction text
     * @param articleTitle the source article title
     */
    void notifyPredictionExtracted(String jobId, String predictionText, String articleTitle);
}