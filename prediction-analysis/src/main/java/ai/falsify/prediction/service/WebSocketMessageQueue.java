package ai.falsify.prediction.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for queuing WebSocket messages for offline clients and handling message deduplication.
 * Provides reliable message delivery and prevents duplicate notifications.
 */
@ApplicationScoped
public class WebSocketMessageQueue {
    
    private static final Logger LOG = Logger.getLogger(WebSocketMessageQueue.class);
    
    // Maximum number of messages to queue per session
    private static final int MAX_QUEUE_SIZE = 100;
    
    // Maximum age of queued messages in minutes
    private static final int MAX_MESSAGE_AGE_MINUTES = 30;
    
    // Message queues per session ID
    private final Map<String, Queue<QueuedMessage>> sessionQueues = new ConcurrentHashMap<>();
    
    // Message deduplication cache (messageId -> timestamp)
    private final Map<String, Instant> messageDeduplicationCache = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Queued message with metadata.
     */
    public record QueuedMessage(
        String messageId,
        String content,
        Instant timestamp,
        String messageType,
        String jobId
    ) {}
    
    /**
     * Initialize the message queue service.
     */
    public void initialize() {
        // Schedule cleanup of old messages and deduplication cache
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldMessages, 5, 5, TimeUnit.MINUTES);
        cleanupExecutor.scheduleAtFixedRate(this::cleanupDeduplicationCache, 10, 10, TimeUnit.MINUTES);
        
        LOG.info("WebSocket message queue initialized");
    }
    
    /**
     * Queue a message for a specific session.
     * 
     * @param sessionId Session ID
     * @param messageId Unique message ID for deduplication
     * @param content Message content
     * @param messageType Type of message
     * @param jobId Associated job ID
     * @return true if message was queued, false if duplicate or queue full
     */
    public boolean queueMessage(String sessionId, String messageId, String content, String messageType, String jobId) {
        // Check for duplicate message
        if (isDuplicateMessage(messageId)) {
            LOG.debugf("Skipping duplicate message: %s", messageId);
            return false;
        }
        
        // Mark message as seen
        messageDeduplicationCache.put(messageId, Instant.now());
        
        // Get or create queue for session
        Queue<QueuedMessage> queue = sessionQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        
        // Check queue size limit
        if (queue.size() >= MAX_QUEUE_SIZE) {
            // Remove oldest message to make room
            QueuedMessage removed = queue.poll();
            if (removed != null) {
                LOG.debugf("Removed oldest message from queue for session %s: %s", sessionId, removed.messageId());
            }
        }
        
        // Add new message
        QueuedMessage queuedMessage = new QueuedMessage(messageId, content, Instant.now(), messageType, jobId);
        queue.offer(queuedMessage);
        
        LOG.debugf("Queued message for session %s: %s (queue size: %d)", sessionId, messageId, queue.size());
        return true;
    }
    
    /**
     * Deliver all queued messages to a session when it reconnects.
     * 
     * @param session WebSocket session
     * @return number of messages delivered
     */
    public int deliverQueuedMessages(Session session) {
        if (session == null) {
            LOG.debug("Cannot deliver messages to null session");
            return 0;
        }
        
        String sessionId = session.getId();
        Queue<QueuedMessage> queue = sessionQueues.get(sessionId);
        
        if (queue == null || queue.isEmpty()) {
            LOG.debugf("No queued messages for session: %s", sessionId);
            return 0;
        }
        
        int delivered = 0;
        QueuedMessage message;
        
        while ((message = queue.poll()) != null) {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message.content());
                    delivered++;
                    LOG.debugf("Delivered queued message to session %s: %s", sessionId, message.messageId());
                } else {
                    // Session closed, put message back
                    queue.offer(message);
                    break;
                }
            } catch (Exception e) {
                LOG.warnf("Failed to deliver queued message to session %s: %s", sessionId, e.getMessage());
                // Don't put message back, consider it lost
            }
        }
        
        LOG.infof("Delivered %d queued messages to session: %s", delivered, sessionId);
        return delivered;
    }
    
    /**
     * Get queued messages for a specific job.
     * 
     * @param sessionId Session ID
     * @param jobId Job ID
     * @return list of queued messages for the job
     */
    public List<QueuedMessage> getQueuedMessagesForJob(String sessionId, String jobId) {
        Queue<QueuedMessage> queue = sessionQueues.get(sessionId);
        if (queue == null) {
            return Collections.emptyList();
        }
        
        return queue.stream()
            .filter(msg -> jobId.equals(msg.jobId()))
            .toList();
    }
    
    /**
     * Remove all queued messages for a session.
     * 
     * @param sessionId Session ID
     */
    public void clearSessionQueue(String sessionId) {
        Queue<QueuedMessage> queue = sessionQueues.remove(sessionId);
        if (queue != null) {
            int size = queue.size();
            queue.clear();
            LOG.debugf("Cleared %d queued messages for session: %s", size, sessionId);
        }
    }
    
    /**
     * Get the number of queued messages for a session.
     * 
     * @param sessionId Session ID
     * @return number of queued messages
     */
    public int getQueueSize(String sessionId) {
        Queue<QueuedMessage> queue = sessionQueues.get(sessionId);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Get total number of queued messages across all sessions.
     * 
     * @return total queued messages
     */
    public int getTotalQueuedMessages() {
        return sessionQueues.values().stream()
            .mapToInt(Queue::size)
            .sum();
    }
    
    /**
     * Check if a message is a duplicate based on message ID.
     * 
     * @param messageId Message ID
     * @return true if duplicate
     */
    private boolean isDuplicateMessage(String messageId) {
        return messageDeduplicationCache.containsKey(messageId);
    }
    
    /**
     * Clean up old messages from queues.
     */
    private void cleanupOldMessages() {
        Instant cutoff = Instant.now().minus(MAX_MESSAGE_AGE_MINUTES, ChronoUnit.MINUTES);
        int totalRemoved = 0;
        
        for (Map.Entry<String, Queue<QueuedMessage>> entry : sessionQueues.entrySet()) {
            String sessionId = entry.getKey();
            Queue<QueuedMessage> queue = entry.getValue();
            
            int removed = 0;
            Iterator<QueuedMessage> iterator = queue.iterator();
            while (iterator.hasNext()) {
                QueuedMessage message = iterator.next();
                if (message.timestamp().isBefore(cutoff)) {
                    iterator.remove();
                    removed++;
                    totalRemoved++;
                }
            }
            
            if (removed > 0) {
                LOG.debugf("Removed %d old messages from session %s queue", removed, sessionId);
            }
            
            // Remove empty queues
            if (queue.isEmpty()) {
                sessionQueues.remove(sessionId);
            }
        }
        
        if (totalRemoved > 0) {
            LOG.infof("Cleaned up %d old messages from queues", totalRemoved);
        }
    }
    
    /**
     * Clean up old entries from deduplication cache.
     */
    private void cleanupDeduplicationCache() {
        Instant cutoff = Instant.now().minus(MAX_MESSAGE_AGE_MINUTES, ChronoUnit.MINUTES);
        int removed = 0;
        
        Iterator<Map.Entry<String, Instant>> iterator = messageDeduplicationCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (entry.getValue().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            LOG.debugf("Cleaned up %d old entries from deduplication cache", removed);
        }
    }
    
    /**
     * Shutdown the message queue service.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        sessionQueues.clear();
        messageDeduplicationCache.clear();
        LOG.info("WebSocket message queue shutdown");
    }
}