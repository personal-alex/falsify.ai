package ai.falsify.prediction.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WebSocketMessageQueueTest {
    
    private WebSocketMessageQueue messageQueue;
    
    @BeforeEach
    void setUp() {
        messageQueue = new WebSocketMessageQueue();
        messageQueue.initialize();
    }
    
    @Test
    void testQueueMessage() {
        boolean result = messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        
        assertTrue(result);
        assertEquals(1, messageQueue.getQueueSize("session123"));
    }
    
    @Test
    void testQueueDuplicateMessage() {
        // Queue first message
        boolean result1 = messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        assertTrue(result1);
        
        // Try to queue duplicate
        boolean result2 = messageQueue.queueMessage("session123", "msg1", "content2", "batch.submitted", "job123");
        assertFalse(result2);
        
        // Should still have only one message
        assertEquals(1, messageQueue.getQueueSize("session123"));
    }
    
    @Test
    void testQueueSizeLimit() {
        // Queue messages up to the limit (100)
        for (int i = 0; i < 105; i++) {
            messageQueue.queueMessage("session123", "msg" + i, "content" + i, "test", "job123");
        }
        
        // Should not exceed the limit
        assertEquals(100, messageQueue.getQueueSize("session123"));
    }
    
    @Test
    void testDeliverQueuedMessagesWithNullSession() {
        // Queue some messages
        messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        messageQueue.queueMessage("session123", "msg2", "content2", "batch.progress", "job123");
        messageQueue.queueMessage("session123", "msg3", "content3", "batch.completed", "job123");
        
        assertEquals(3, messageQueue.getQueueSize("session123"));
        
        // Try to deliver messages with null session (simulates closed session)
        int delivered = messageQueue.deliverQueuedMessages(null);
        
        assertEquals(0, delivered);
        assertEquals(3, messageQueue.getQueueSize("session123")); // Messages should remain queued
    }
    
    @Test
    void testGetQueuedMessagesForJob() {
        // Queue messages for different jobs
        messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        messageQueue.queueMessage("session123", "msg2", "content2", "batch.progress", "job456");
        messageQueue.queueMessage("session123", "msg3", "content3", "batch.completed", "job123");
        
        List<WebSocketMessageQueue.QueuedMessage> job123Messages = 
            messageQueue.getQueuedMessagesForJob("session123", "job123");
        
        assertEquals(2, job123Messages.size());
        assertTrue(job123Messages.stream().allMatch(msg -> "job123".equals(msg.jobId())));
        
        List<WebSocketMessageQueue.QueuedMessage> job456Messages = 
            messageQueue.getQueuedMessagesForJob("session123", "job456");
        
        assertEquals(1, job456Messages.size());
        assertEquals("job456", job456Messages.get(0).jobId());
    }
    
    @Test
    void testClearSessionQueue() {
        // Queue some messages
        messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        messageQueue.queueMessage("session123", "msg2", "content2", "batch.progress", "job123");
        
        assertEquals(2, messageQueue.getQueueSize("session123"));
        
        messageQueue.clearSessionQueue("session123");
        
        assertEquals(0, messageQueue.getQueueSize("session123"));
    }
    
    @Test
    void testGetTotalQueuedMessages() {
        messageQueue.queueMessage("session1", "msg1", "content1", "batch.submitted", "job123");
        messageQueue.queueMessage("session1", "msg2", "content2", "batch.progress", "job123");
        messageQueue.queueMessage("session2", "msg3", "content3", "batch.completed", "job456");
        
        assertEquals(3, messageQueue.getTotalQueuedMessages());
    }
    
    @Test
    void testEmptyQueueOperations() {
        assertEquals(0, messageQueue.getQueueSize("nonexistent"));
        assertEquals(0, messageQueue.getTotalQueuedMessages());
        
        List<WebSocketMessageQueue.QueuedMessage> messages = 
            messageQueue.getQueuedMessagesForJob("nonexistent", "job123");
        assertTrue(messages.isEmpty());
        
        int delivered = messageQueue.deliverQueuedMessages(null);
        assertEquals(0, delivered);
    }
    
    @Test
    void testQueuedMessageRecord() {
        messageQueue.queueMessage("session123", "msg1", "content1", "batch.submitted", "job123");
        
        List<WebSocketMessageQueue.QueuedMessage> messages = 
            messageQueue.getQueuedMessagesForJob("session123", "job123");
        
        assertEquals(1, messages.size());
        
        WebSocketMessageQueue.QueuedMessage message = messages.get(0);
        assertEquals("msg1", message.messageId());
        assertEquals("content1", message.content());
        assertEquals("batch.submitted", message.messageType());
        assertEquals("job123", message.jobId());
        assertNotNull(message.timestamp());
    }
    
    @Test
    void testMessageDeduplicationAcrossSessions() {
        // Same message ID should be deduplicated across different sessions
        boolean result1 = messageQueue.queueMessage("session1", "msg1", "content1", "batch.submitted", "job123");
        boolean result2 = messageQueue.queueMessage("session2", "msg1", "content2", "batch.submitted", "job123");
        
        assertTrue(result1);
        assertFalse(result2); // Should be deduplicated
        
        assertEquals(1, messageQueue.getQueueSize("session1"));
        assertEquals(0, messageQueue.getQueueSize("session2"));
    }
}