package ai.falsify.crawlers.service;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LoggingContextTest {

    private LoggingContext loggingContext;
    private Logger logger;

    @BeforeEach
    void setUp() {
        loggingContext = new LoggingContext();
        logger = Logger.getLogger(LoggingContextTest.class);
    }

    @Test
    void testContextManagement() {
        // Test setting and getting context
        loggingContext.setContext("url", "https://example.com");
        loggingContext.setContext("operation", "test");
        
        assertEquals("https://example.com", loggingContext.getContext("url"));
        assertEquals("test", loggingContext.getContext("operation"));
        
        // Test clearing context
        loggingContext.clearContext();
        assertNull(loggingContext.getContext("url"));
        assertNull(loggingContext.getContext("operation"));
    }

    @Test
    void testFormatMessage() {
        loggingContext.setContext("url", "https://example.com");
        loggingContext.setContext("count", 5);
        
        String formatted = loggingContext.formatMessage("TEST_OPERATION", "Processing %s", "article");
        
        assertTrue(formatted.startsWith("TEST_OPERATION: Processing article"));
        assertTrue(formatted.contains("url=https://example.com"));
        assertTrue(formatted.contains("count=5"));
    }

    @Test
    void testOperationTimer() {
        LoggingContext.OperationTimer timer = loggingContext.startOperation(
            logger, "TEST_OP", "Starting test operation"
        );
        
        assertNotNull(timer);
        
        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        timer.complete("Test operation completed successfully");
    }

    @Test
    void testPerformanceLogger() {
        LoggingContext.PerformanceLogger perfLogger = loggingContext.createPerformanceLogger(logger, "TEST_PERF");
        
        // Record some operations
        perfLogger.recordSuccess(Duration.ofMillis(100));
        perfLogger.recordSuccess(Duration.ofMillis(150));
        perfLogger.recordFailure(Duration.ofMillis(200));
        
        // Log summary
        perfLogger.logSummary();
    }
}