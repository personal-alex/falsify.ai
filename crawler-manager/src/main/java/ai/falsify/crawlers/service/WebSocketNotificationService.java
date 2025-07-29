package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.JobStatus;
import ai.falsify.crawlers.model.CrawlerMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for broadcasting WebSocket notifications about job status changes.
 * Manages WebSocket sessions and sends real-time updates to connected clients.
 */
@ApplicationScoped
public class WebSocketNotificationService {
    
    @Inject
    ObjectMapper objectMapper;
    
    // Store active WebSocket sessions
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    
    /**
     * Registers a new WebSocket session.
     */
    public void addSession(String sessionId, Session session) {
        sessions.put(sessionId, session);
        Log.debugf("Added WebSocket session: %s", sessionId);
    }
    
    /**
     * Removes a WebSocket session.
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        Log.debugf("Removed WebSocket session: %s", sessionId);
    }
    
    /**
     * Broadcasts job started event to all connected clients.
     */
    public void broadcastJobStarted(JobStatus jobStatus) {
        WebSocketMessage message = new WebSocketMessage("job.started", jobStatus);
        broadcast(message);
    }
    
    /**
     * Broadcasts job progress update to all connected clients.
     */
    public void broadcastJobProgress(JobStatus jobStatus) {
        WebSocketMessage message = new WebSocketMessage("job.progress", jobStatus);
        broadcast(message);
    }
    
    /**
     * Broadcasts job completed event to all connected clients.
     */
    public void broadcastJobCompleted(JobStatus jobStatus) {
        WebSocketMessage message = new WebSocketMessage("job.completed", jobStatus);
        broadcast(message);
    }
    
    /**
     * Broadcasts job failed event to all connected clients.
     */
    public void broadcastJobFailed(JobStatus jobStatus) {
        WebSocketMessage message = new WebSocketMessage("job.failed", jobStatus);
        broadcast(message);
    }
    
    /**
     * Broadcasts metrics updated event to all connected clients.
     */
    public void broadcastMetricsUpdated(CrawlerMetrics metrics) {
        WebSocketMessage message = new WebSocketMessage("metrics.updated", metrics);
        broadcast(message);
    }
    
    /**
     * Broadcasts a generic notification message.
     */
    public void broadcastNotification(String type, String message, String crawlerId) {
        NotificationMessage notification = new NotificationMessage(type, message, crawlerId);
        WebSocketMessage wsMessage = new WebSocketMessage("notification", notification);
        broadcast(wsMessage);
    }
    
    /**
     * Sends a message to a specific session.
     */
    public void sendToSession(String sessionId, WebSocketMessage message) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.getAsyncRemote().sendText(json);
            } catch (Exception e) {
                Log.errorf(e, "Failed to send WebSocket message to session %s", sessionId);
                // Remove invalid session
                sessions.remove(sessionId);
            }
        }
    }
    
    /**
     * Broadcasts a message to all connected sessions.
     */
    private void broadcast(WebSocketMessage message) {
        if (sessions.isEmpty()) {
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            
            // Send to all active sessions
            sessions.entrySet().removeIf(entry -> {
                Session session = entry.getValue();
                if (!session.isOpen()) {
                    Log.debugf("Removing closed WebSocket session: %s", entry.getKey());
                    return true;
                }
                
                try {
                    session.getAsyncRemote().sendText(json);
                    return false;
                } catch (Exception e) {
                    Log.warnf(e, "Failed to send WebSocket message to session %s", entry.getKey());
                    return true; // Remove failed session
                }
            });
            
            Log.debugf("Broadcasted WebSocket message of type %s to %d sessions", 
                      message.type, sessions.size());
            
        } catch (Exception e) {
            Log.errorf(e, "Failed to serialize WebSocket message: %s", message);
        }
    }
    
    /**
     * Gets the number of active WebSocket sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * WebSocket message wrapper.
     */
    public static class WebSocketMessage {
        public String type;
        public Object data;
        public long timestamp;
        
        public WebSocketMessage() {
            // Default constructor for Jackson
        }
        
        public WebSocketMessage(String type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Notification message for general alerts.
     */
    public static class NotificationMessage {
        public String type;
        public String message;
        public String crawlerId;
        public long timestamp;
        
        public NotificationMessage() {
            // Default constructor for Jackson
        }
        
        public NotificationMessage(String type, String message, String crawlerId) {
            this.type = type;
            this.message = message;
            this.crawlerId = crawlerId;
            this.timestamp = System.currentTimeMillis();
        }
    }
}