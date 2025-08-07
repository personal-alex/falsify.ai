package ai.falsify.prediction.service;

import ai.falsify.crawlers.common.model.AnalysisStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    /**
     * Register a WebSocket session for notifications.
     * 
     * @param session WebSocket session
     */
    public void addSession(Session session) {
        sessions.add(session);
        LOG.debugf("Added WebSocket session: %s (total: %d)", session.getId(), sessions.size());
    }
    
    /**
     * Unregister a WebSocket session.
     * 
     * @param session WebSocket session
     */
    public void removeSession(Session session) {
        sessions.remove(session);
        LOG.debugf("Removed WebSocket session: %s (total: %d)", session.getId(), sessions.size());
    }
    
    /**
     * Send job status update to all connected clients.
     * 
     * @param jobId Job ID
     * @param status New status
     */
    public void sendJobStatusUpdate(String jobId, AnalysisStatus status) {
        Map<String, Object> message = Map.of(
            "type", "job.status.update",
            "jobId", jobId,
            "status", status.toString(),
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
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
        Map<String, Object> message = Map.of(
            "type", "job.progress.update",
            "jobId", jobId,
            "articleId", articleId,
            "predictionsFound", predictionsFound,
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
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
        Map<String, Object> message = Map.of(
            "type", "job.completed",
            "jobId", jobId,
            "processedArticles", processedArticles,
            "totalPredictions", totalPredictions,
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
        LOG.infof("Sent job completion: %s - %d articles, %d predictions", jobId, processedArticles, totalPredictions);
    }
    
    /**
     * Send job failure notification.
     * 
     * @param jobId Job ID
     * @param errorMessage Error message
     */
    public void sendJobFailed(String jobId, String errorMessage) {
        Map<String, Object> message = Map.of(
            "type", "job.failed",
            "jobId", jobId,
            "error", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
        LOG.warnf("Sent job failure: %s - %s", jobId, errorMessage);
    }
    
    /**
     * Send job cancellation notification.
     * 
     * @param jobId Job ID
     */
    public void sendJobCancelled(String jobId) {
        Map<String, Object> message = Map.of(
            "type", "job.cancelled",
            "jobId", jobId,
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
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
        Map<String, Object> message = Map.of(
            "type", "prediction.extracted",
            "jobId", jobId,
            "articleId", articleId,
            "predictionText", predictionText,
            "rating", rating,
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage(message);
        LOG.debugf("Sent prediction extracted: %s - %s", jobId, predictionText.substring(0, Math.min(50, predictionText.length())));
    }
    
    /**
     * Broadcast a message to all connected WebSocket sessions.
     * 
     * @param message Message to broadcast
     */
    private void broadcastMessage(Map<String, Object> message) {
        if (sessions.isEmpty()) {
            LOG.debug("No WebSocket sessions to broadcast to");
            return;
        }
        
        String jsonMessage = convertToJson(message);
        
        // Remove closed sessions and send to active ones
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(jsonMessage);
                    return false; // Keep session
                } else {
                    return true; // Remove closed session
                }
            } catch (Exception e) {
                LOG.debugf("Error sending message to session %s: %s", session.getId(), e.getMessage());
                return true; // Remove problematic session
            }
        });
        
        LOG.debugf("Broadcasted message to %d sessions", sessions.size());
    }
    
    /**
     * Convert message map to JSON string.
     * Simple implementation - in production, use Jackson or similar.
     * 
     * @param message Message map
     * @return JSON string
     */
    private String convertToJson(Map<String, Object> message) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : message.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Get the number of active WebSocket connections.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}