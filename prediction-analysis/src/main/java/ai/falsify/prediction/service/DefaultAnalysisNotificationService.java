package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default implementation of AnalysisNotificationInterface that logs notifications.
 * This is used when no WebSocket-based notification service is available.
 */
@ApplicationScoped
public class DefaultAnalysisNotificationService implements AnalysisNotificationInterface {

    @Override
    public void notifyJobStarted(AnalysisJobEntity job) {
        Log.infof("Analysis job started: %s (Type: %s, Articles: %d)", 
                 job.jobId, job.analysisType, job.totalArticles);
    }

    @Override
    public void notifyJobProgress(AnalysisJobEntity job) {
        Log.infof("Analysis job progress: %s - %.1f%% complete (%d/%d articles, %d predictions)", 
                 job.jobId, job.getProgressPercentage(), 
                 job.processedArticles, job.totalArticles, job.predictionsFound);
    }

    @Override
    public void notifyJobCompleted(AnalysisJobEntity job) {
        Log.infof("Analysis job completed: %s - Processed %d articles, found %d predictions in %dms", 
                 job.jobId, job.processedArticles, job.predictionsFound, job.getDurationMillis());
    }

    @Override
    public void notifyJobFailed(AnalysisJobEntity job) {
        Log.errorf("Analysis job failed: %s - %s", job.jobId, job.errorMessage);
    }

    @Override
    public void notifyJobCancelled(AnalysisJobEntity job) {
        Log.infof("Analysis job cancelled: %s", job.jobId);
    }

    @Override
    public void notifyPredictionExtracted(String jobId, String predictionText, String articleTitle) {
        Log.debugf("Prediction extracted in job %s from article '%s': %s", 
                  jobId, articleTitle, predictionText.substring(0, Math.min(100, predictionText.length())));
    }
}