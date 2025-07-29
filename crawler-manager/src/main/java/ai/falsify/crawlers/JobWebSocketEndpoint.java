package ai.falsify.crawlers;

import ai.falsify.crawlers.service.WebSocketNotificationService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;

/**
 * WebSocket endpoint for real-time job status updates.
 * Provides live notifications about job progress, completion, and failures.
 */
@ServerEndpoint("/websocket/jobs")
@ApplicationScoped
public class JobWebSocketEndpoint {
    
    @Inject
    WebSocketNotificationService webSocketService;
    
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        webSocketService.addSession(sessionId, session);
        Log.infof("WebSocket connection opened: %s", sessionId);
        
        // Send welcome message
        WebSocketNotificationService.WebSocketMessage welcomeMessage = 
            new WebSocketNotificationService.WebSocketMessage("connection.opened", 
                Map.of("sessionId", sessionId, "message", "Connected to job updates"));
        webSocketService.sendToSession(sessionId, welcomeMessage);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String sessionId = session.getId();
        webSocketService.removeSession(sessionId);
        Log.infof("WebSocket connection closed: %s, reason: %s", sessionId, closeReason.getReasonPhrase());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        String sessionId = session.getId();
        Log.errorf(throwable, "WebSocket error for session %s", sessionId);
        webSocketService.removeSession(sessionId);
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        String sessionId = session.getId();
        Log.debugf("Received WebSocket message from %s: %s", sessionId, message);
        
        // For now, we don't process incoming messages, but this could be extended
        // to handle client requests like subscribing to specific crawler updates
        
        // Send acknowledgment
        WebSocketNotificationService.WebSocketMessage ackMessage = 
            new WebSocketNotificationService.WebSocketMessage("message.received", 
                Map.of("originalMessage", message, "timestamp", System.currentTimeMillis()));
        webSocketService.sendToSession(sessionId, ackMessage);
    }
}