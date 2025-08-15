package ai.falsify.prediction.service;

import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.NotificationEvent;
import ai.falsify.prediction.model.BatchNotificationEvent;
import ai.falsify.prediction.model.NotificationEventSerializer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

/**
 * Service for formatting WebSocket messages with structured data and proper JSON serialization.
 * Handles complex batch data and provides consistent message formatting across all notification types.
 */
@ApplicationScoped
public class WebSocketMessageFormatter {
    
    private static final Logger LOG = Logger.getLogger(WebSocketMessageFormatter.class);
    
    /**
     * Format a batch submitted message.
     */
    public String formatBatchSubmitted(String jobId, String batchId, int totalArticles) {
        BatchNotificationEvent event = BatchNotificationEvent.batchSubmitted(jobId, batchId, totalArticles);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch progress message.
     */
    public String formatBatchProgress(String jobId, String batchId, int completedRequests, 
                                    int totalRequests, int failedRequests) {
        BatchNotificationEvent event = BatchNotificationEvent.batchProgress(
            jobId, batchId, completedRequests, failedRequests, totalRequests, null);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch completed message.
     */
    public String formatBatchCompleted(String jobId, String batchId, int totalRequests, 
                                     int successfulRequests, int failedRequests, int totalPredictions) {
        BatchNotificationEvent event = BatchNotificationEvent.batchCompleted(
            jobId, batchId, successfulRequests, failedRequests, totalRequests, null);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch failed message.
     */
    public String formatBatchFailed(String jobId, String batchId, String errorMessage, int partialResults) {
        // Use a reasonable approximation for failed batch - assume some completed requests
        BatchNotificationEvent event = BatchNotificationEvent.batchFailed(
            jobId, batchId, partialResults, 1, partialResults + 1, errorMessage);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch status update message.
     */
    public String formatBatchStatusUpdate(BatchJobStatus batchJobStatus) {
        BatchNotificationEvent event = BatchNotificationEvent.batchProgress(
            batchJobStatus.jobId(), 
            batchJobStatus.batchId(), 
            batchJobStatus.completedRequests(), 
            batchJobStatus.failedRequests(), 
            batchJobStatus.totalRequests(),
            Map.of("state", batchJobStatus.state().toString())
        );
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch timeout message.
     */
    public String formatBatchTimeout(String jobId, String batchId, int timeoutMinutes, int partialResults) {
        BatchNotificationEvent event = BatchNotificationEvent.batchTimeout(
            jobId, batchId, partialResults, 0, partialResults, timeoutMinutes);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a batch cancelled message.
     */
    public String formatBatchCancelled(String jobId, String batchId, String reason) {
        BatchNotificationEvent event = BatchNotificationEvent.batchCancelled(
            jobId, batchId, 0, 0, 0);
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a general job status update message.
     */
    public String formatJobStatusUpdate(String jobId, String status) {
        NotificationEvent event = new NotificationEvent(
            "job.status.update",
            jobId,
            Instant.now(),
            Map.of("status", status)
        );
        
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a job progress update message.
     */
    public String formatProgressUpdate(String jobId, String articleId, int predictionsFound) {
        NotificationEvent event = new NotificationEvent(
            "job.progress.update",
            jobId,
            Instant.now(),
            Map.of(
                "articleId", articleId,
                "predictionsFound", predictionsFound
            )
        );
        
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a job completed message.
     */
    public String formatJobCompleted(String jobId, int processedArticles, int totalPredictions) {
        NotificationEvent event = new NotificationEvent(
            "job.completed",
            jobId,
            Instant.now(),
            Map.of(
                "processedArticles", processedArticles,
                "totalPredictions", totalPredictions
            )
        );
        
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a job failed message.
     */
    public String formatJobFailed(String jobId, String errorMessage) {
        NotificationEvent event = new NotificationEvent(
            "job.failed",
            jobId,
            Instant.now(),
            Map.of("error", errorMessage)
        );
        
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a job cancelled message.
     */
    public String formatJobCancelled(String jobId) {
        NotificationEvent event = new NotificationEvent(
            "job.cancelled",
            jobId,
            Instant.now(),
            Map.of()
        );
        
        return NotificationEventSerializer.toJson(event);
    }
    
    /**
     * Format a prediction extracted message.
     */
    public String formatPredictionExtracted(String jobId, String articleId, String predictionText, int rating) {
        NotificationEvent event = new NotificationEvent(
            "prediction.extracted",
            jobId,
            Instant.now(),
            Map.of(
                "articleId", articleId,
                "predictionText", predictionText,
                "rating", rating
            )
        );
        
        return NotificationEventSerializer.toJson(event);
    }
}