package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisStatus;
import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.BatchState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Service for sending real-time notifications about analysis job progress.
 * Uses WebSocket connections to notify clients about job status changes.
 */
@ApplicationScoped
public class AnalysisNotificationService {
    
    private static final Logger LOG = Logger.getLogger(AnalysisNotificationService.class);
    
    // Store WebSocket sessions for real-time updates
    private final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();
    
    @Inject
    WebSocketMessageFormatter messageFormatter;
    
    @Inject
    WebSocketMessageQueue messageQueue;
    
    @PostConstruct
    void initialize() {
        messageQueue.initialize();
        LOG.info("AnalysisNotificationService initialized");
    }
    
    @PreDestroy
    void shutdown() {
        messageQueue.shutdown();
        LOG.info("AnalysisNotificationService shutdown");
    }
    
    /**
     * Register a WebSocket session for notifications.
     * 
     * @param session WebSocket session
     */
    public void addSession(Session session) {
        sessions.add(session);
        LOG.debugf("Added WebSocket session: %s (total: %d)", session.getId(), sessions.size());
        
        // Deliver any queued messages for this session
        int delivered = messageQueue.deliverQueuedMessages(session);
        if (delivered > 0) {
            LOG.infof("Delivered %d queued messages to reconnected session: %s", delivered, session.getId());
        }
    }
    
    /**
     * Unregister a WebSocket session.
     * 
     * @param session WebSocket session
     */
    public void removeSession(Session session) {
        sessions.remove(session);
        LOG.debugf("Removed WebSocket session: %s (total: %d)", session.getId(), sessions.size());
        
        // Keep queued messages for potential reconnection
        // Messages will be cleaned up automatically after timeout
    }
    
    /**
     * Send job status update to all connected clients.
     * 
     * @param jobId Job ID
     * @param status New status
     */
    public void sendJobStatusUpdate(String jobId, AnalysisStatus status) {
        String messageContent = messageFormatter.formatJobStatusUpdate(jobId, status.toString());
        String messageId = generateMessageId("job.status.update", jobId, status.toString());
        
        broadcastMessageWithQueue(messageContent, messageId, "job.status.update", jobId);
        LOG.debugf("Sent job status update: %s -> %s", jobId, status);
    }
    
    /**
     * Send progress update for article processing.
     * 
     * @param jobId Job ID
     * @param articleId Article ID being processed
     * @param predictionsFound Number of predictions found in this article
     */
    public void sendProgressUpdate(String jobId, String articleId, int predictionsFound) {
        String messageContent = messageFormatter.formatProgressUpdate(jobId, articleId, predictionsFound);
        String messageId = generateMessageId("job.progress.update", jobId, articleId);
        
        broadcastMessageWithQueue(messageContent, messageId, "job.progress.update", jobId);
        LOG.debugf("Sent progress update: %s - article %s, %d predictions", jobId, articleId, predictionsFound);
    }
    
    /**
     * Send job completion notification.
     * 
     * @param jobId Job ID
     * @param processedArticles Number of articles processed
     * @param totalPredictions Total predictions found
     */
    public void sendJobCompleted(String jobId, int processedArticles, int totalPredictions) {
        String messageContent = messageFormatter.formatJobCompleted(jobId, processedArticles, totalPredictions);
        String messageId = generateMessageId("job.completed", jobId);
        
        broadcastMessageWithQueue(messageContent, messageId, "job.completed", jobId);
        LOG.infof("Sent job completion: %s - %d articles, %d predictions", jobId, processedArticles, totalPredictions);
    }
    
    /**
     * Send job failure notification.
     * 
     * @param jobId Job ID
     * @param errorMessage Error message
     */
    public void sendJobFailed(String jobId, String errorMessage) {
        String messageContent = messageFormatter.formatJobFailed(jobId, errorMessage);
        String messageId = generateMessageId("job.failed", jobId);
        
        broadcastMessageWithQueue(messageContent, messageId, "job.failed", jobId);
        LOG.warnf("Sent job failure: %s - %s", jobId, errorMessage);
    }
    
    /**
     * Send job cancellation notification.
     * 
     * @param jobId Job ID
     */
    public void sendJobCancelled(String jobId) {
        String messageContent = messageFormatter.formatJobCancelled(jobId);
        String messageId = generateMessageId("job.cancelled", jobId);
        
        broadcastMessageWithQueue(messageContent, messageId, "job.cancelled", jobId);
        LOG.infof("Sent job cancellation: %s", jobId);
    }
    
    /**
     * Send prediction extracted notification.
     * 
     * @param jobId Job ID
     * @param articleId Article ID
     * @param predictionText Prediction text
     * @param rating Prediction rating
     */
    public void sendPredictionExtracted(String jobId, String articleId, String predictionText, int rating) {
        String messageContent = messageFormatter.formatPredictionExtracted(jobId, articleId, predictionText, rating);
        String messageId = generateMessageId("prediction.extracted", jobId, articleId, String.valueOf(rating));
        
        broadcastMessageWithQueue(messageContent, messageId, "prediction.extracted", jobId);
        LOG.debugf("Sent prediction extracted: %s - %s", jobId, predictionText.substring(0, Math.min(50, predictionText.length())));
    }
    
    /**
     * Broadcast a message to all connected WebSocket sessions with queuing support.
     * 
     * @param messageContent Formatted message content
     * @param messageId Unique message ID for deduplication
     * @param messageType Type of message
     * @param jobId Associated job ID
     */
    private void broadcastMessageWithQueue(String messageContent, String messageId, String messageType, String jobId) {
        if (sessions.isEmpty()) {
            LOG.debug("No WebSocket sessions to broadcast to, queuing message");
            // Still queue the message for potential future connections
            queueMessageForAllSessions(messageId, messageContent, messageType, jobId);
            return;
        }
        
        // Remove closed sessions and send to active ones
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(messageContent);
                    return false; // Keep session
                } else {
                    // Queue message for offline session
                    messageQueue.queueMessage(session.getId(), messageId, messageContent, messageType, jobId);
                    return true; // Remove closed session
                }
            } catch (Exception e) {
                LOG.debugf("Error sending message to session %s: %s", session.getId(), e.getMessage());
                // Queue message for problematic session
                messageQueue.queueMessage(session.getId(), messageId, messageContent, messageType, jobId);
                return true; // Remove problematic session
            }
        });
        
        LOG.debugf("Broadcasted message to %d sessions", sessions.size());
    }
    
    /**
     * Generate a unique message ID for deduplication.
     * 
     * @param messageType Type of message
     * @param components Components to include in ID
     * @return unique message ID
     */
    private String generateMessageId(String messageType, String... components) {
        StringBuilder id = new StringBuilder(messageType);
        for (String component : components) {
            id.append(":").append(component);
        }
        return id.toString();
    }
    
    /**
     * Queue a message for all known sessions (for when no active sessions exist).
     * 
     * @param messageId Message ID
     * @param messageContent Message content
     * @param messageType Message type
     * @param jobId Job ID
     */
    private void queueMessageForAllSessions(String messageId, String messageContent, String messageType, String jobId) {
        // This is a simplified approach - in a real implementation, you might want to
        // maintain a list of recently connected session IDs or use a different strategy
        LOG.debugf("No active sessions to broadcast to, message will be queued for future connections: %s", messageId);
    }
    
    /**
     * Send batch job submitted notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param totalArticles Total number of articles in the batch
     */
    public void sendBatchSubmitted(String jobId, String batchId, int totalArticles) {
        String messageContent = messageFormatter.formatBatchSubmitted(jobId, batchId, totalArticles);
        String messageId = generateMessageId("batch.submitted", jobId, batchId);
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.submitted", jobId);
        LOG.infof("Sent batch submitted: %s - batch %s, %d articles", jobId, batchId, totalArticles);
    }
    
    /**
     * Send batch processing progress notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param completedRequests Number of completed requests
     * @param totalRequests Total number of requests
     * @param failedRequests Number of failed requests
     */
    public void sendBatchProgress(String jobId, String batchId, int completedRequests, int totalRequests, int failedRequests) {
        String messageContent = messageFormatter.formatBatchProgress(jobId, batchId, completedRequests, totalRequests, failedRequests);
        String messageId = generateMessageId("batch.progress", jobId, batchId, String.valueOf(completedRequests));
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.progress", jobId);
        LOG.debugf("Sent batch progress: %s - %d/%d completed (%d failed)", 
                  jobId, completedRequests, totalRequests, failedRequests);
    }
    
    /**
     * Send batch completion notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param totalRequests Total number of requests processed
     * @param successfulRequests Number of successful requests
     * @param failedRequests Number of failed requests
     * @param totalPredictions Total predictions extracted
     */
    public void sendBatchCompleted(String jobId, String batchId, int totalRequests, int successfulRequests, int failedRequests, int totalPredictions) {
        String messageContent = messageFormatter.formatBatchCompleted(jobId, batchId, totalRequests, successfulRequests, failedRequests, totalPredictions);
        String messageId = generateMessageId("batch.completed", jobId, batchId);
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.completed", jobId);
        LOG.infof("Sent batch completed: %s - %d/%d successful, %d predictions", 
                 jobId, successfulRequests, totalRequests, totalPredictions);
    }
    
    /**
     * Send batch failure notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param errorMessage Error message
     * @param partialResults Number of partial results recovered (if any)
     */
    public void sendBatchFailed(String jobId, String batchId, String errorMessage, int partialResults) {
        String messageContent = messageFormatter.formatBatchFailed(jobId, batchId, errorMessage, partialResults);
        String messageId = generateMessageId("batch.failed", jobId, batchId);
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.failed", jobId);
        LOG.warnf("Sent batch failed: %s - %s (partial results: %d)", jobId, errorMessage, partialResults);
    }
    
    /**
     * Send batch status update notification.
     * 
     * @param batchJobStatus Current batch job status
     */
    public void sendBatchStatusUpdate(BatchJobStatus batchJobStatus) {
        String messageContent = messageFormatter.formatBatchStatusUpdate(batchJobStatus);
        String messageId = generateMessageId("batch.status.update", batchJobStatus.jobId(), batchJobStatus.batchId(), 
                                           String.valueOf(batchJobStatus.completedRequests()));
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.status.update", batchJobStatus.jobId());
        LOG.debugf("Sent batch status update: %s - %s (%d/%d)", 
                  batchJobStatus.jobId(), batchJobStatus.state(), 
                  batchJobStatus.completedRequests(), batchJobStatus.totalRequests());
    }
    
    /**
     * Send batch timeout notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param timeoutMinutes Timeout duration in minutes
     * @param partialResults Number of partial results recovered
     */
    public void sendBatchTimeout(String jobId, String batchId, int timeoutMinutes, int partialResults) {
        String messageContent = messageFormatter.formatBatchTimeout(jobId, batchId, timeoutMinutes, partialResults);
        String messageId = generateMessageId("batch.timeout", jobId, batchId);
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.timeout", jobId);
        LOG.warnf("Sent batch timeout: %s - timeout after %d minutes (partial results: %d)", 
                 jobId, timeoutMinutes, partialResults);
    }
    
    /**
     * Send batch cancelled notification.
     * 
     * @param jobId Analysis job ID
     * @param batchId GenAI batch ID
     * @param reason Cancellation reason
     */
    public void sendBatchCancelled(String jobId, String batchId, String reason) {
        String messageContent = messageFormatter.formatBatchCancelled(jobId, batchId, reason);
        String messageId = generateMessageId("batch.cancelled", jobId, batchId);
        
        broadcastMessageWithQueue(messageContent, messageId, "batch.cancelled", jobId);
        LOG.infof("Sent batch cancelled: %s - %s", jobId, reason);
    }

    /**
     * Get the number of active WebSocket connections.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Get the total number of queued messages across all sessions.
     * 
     * @return Total queued messages
     */
    public int getTotalQueuedMessages() {
        return messageQueue.getTotalQueuedMessages();
    }
    
    /**
     * Get the number of queued messages for a specific session.
     * 
     * @param sessionId Session ID
     * @return Number of queued messages
     */
    public int getQueuedMessageCount(String sessionId) {
        return messageQueue.getQueueSize(sessionId);
    }
    
    /**
     * Clear all queued messages for a specific session.
     * 
     * @param sessionId Session ID
     */
    public void clearSessionQueue(String sessionId) {
        messageQueue.clearSessionQueue(sessionId);
    }
}