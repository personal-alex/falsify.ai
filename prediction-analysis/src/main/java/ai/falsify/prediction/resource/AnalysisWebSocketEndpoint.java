package ai.falsify.prediction.resource;

import ai.falsify.prediction.service.AnalysisNotificationService;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

/**
 * WebSocket endpoint for real-time analysis job updates.
 * Clients can connect to receive live updates about job progress and completion.
 */
@ServerEndpoint("/ws/analysis")
public class AnalysisWebSocketEndpoint {
    
    private static final Logger LOG = Logger.getLogger(AnalysisWebSocketEndpoint.class);
    
    @Inject
    AnalysisNotificationService notificationService;
    
    /**
     * Handle new WebSocket connection.
     * 
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        LOG.infof("WebSocket connection opened: %s", session.getId());
        notificationService.addSession(session);
        
        // Send welcome message
        try {
            session.getAsyncRemote().sendText("{\"type\":\"connection.established\",\"sessionId\":\"" + session.getId() + "\"}");
        } catch (Exception e) {
            LOG.warnf("Failed to send welcome message to session %s: %s", session.getId(), e.getMessage());
        }
    }
    
    /**
     * Handle WebSocket connection close.
     * 
     * @param session WebSocket session
     * @param closeReason Close reason
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.infof("WebSocket connection closed: %s - %s", session.getId(), closeReason.getReasonPhrase());
        notificationService.removeSession(session);
    }
    
    /**
     * Handle incoming WebSocket messages.
     * Currently, this endpoint is primarily for sending updates to clients,
     * but we can handle client messages for features like subscribing to specific jobs.
     * 
     * @param session WebSocket session
     * @param message Incoming message
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        LOG.debugf("Received WebSocket message from %s: %s", session.getId(), message);
        
        try {
            // Parse message and handle client requests
            // For now, just echo back a confirmation
            session.getAsyncRemote().sendText("{\"type\":\"message.received\",\"original\":\"" + message + "\"}");
        } catch (Exception e) {
            LOG.warnf("Error handling WebSocket message from %s: %s", session.getId(), e.getMessage());
        }
    }
    
    /**
     * Handle WebSocket errors.
     * 
     * @param session WebSocket session
     * @param throwable Error that occurred
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "WebSocket error for session %s", session.getId());
        
        // Remove the problematic session
        notificationService.removeSession(session);
        
        // Try to close the session gracefully
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
            }
        } catch (Exception e) {
            LOG.debugf("Error closing WebSocket session %s: %s", session.getId(), e.getMessage());
        }
    }
}