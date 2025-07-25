package ai.falsify.crawlers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructuredLogFormatterTest {

    private StructuredLogFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new StructuredLogFormatter();
    }

    @Test
    void testCreateMessage() {
        String message = formatter.createMessage("TEST_OPERATION")
                .message("Test message")
                .field("key1", "value1")
                .field("key2", 42)
                .build();
        
        assertTrue(message.startsWith("TEST_OPERATION: Test message"));
        assertTrue(message.contains("key1=value1"));
        assertTrue(message.contains("key2=42"));
    }

    @Test
    void testCrawlSessionMessage() {
        String message = formatter.crawlSession(
            "STARTED", 
            "https://example.com", 
            10, 5, 2, 5000
        );
        
        assertTrue(message.contains("CRAWL_SESSION: STARTED"));
        assertTrue(message.contains("url=https://example.com"));
        assertTrue(message.contains("processed_count=10"));
        assertTrue(message.contains("skipped_count=5"));
        assertTrue(message.contains("failed_count=2"));
        assertTrue(message.contains("duration_ms=5000"));
    }

    @Test
    void testArticleProcessingMessage() {
        String message = formatter.articleProcessing(
            "COMPLETED", 
            "https://example.com/article", 
            "Test Article", 
            1500, 
            true, 
            2000
        );
        
        assertTrue(message.contains("ARTICLE_PROCESSING: COMPLETED"));
        assertTrue(message.contains("url=https://example.com/article"));
        assertTrue(message.contains("title=Test Article"));
        assertTrue(message.contains("content_length=1500"));
        assertTrue(message.contains("success=true"));
        assertTrue(message.contains("duration_ms=2000"));
    }

    @Test
    void testNetworkOperationMessage() {
        String message = formatter.networkOperation(
            "HTTP_GET", 
            "https://example.com", 
            true, 
            1000, 
            null
        );
        
        assertTrue(message.contains("NETWORK_OPERATION: HTTP_GET"));
        assertTrue(message.contains("url=https://example.com"));
        assertTrue(message.contains("success=true"));
        assertTrue(message.contains("duration_ms=1000"));
        assertFalse(message.contains("error="));
    }

    @Test
    void testNetworkOperationWithError() {
        String message = formatter.networkOperation(
            "HTTP_GET", 
            "https://example.com", 
            false, 
            1000, 
            "Connection timeout"
        );
        
        assertTrue(message.contains("NETWORK_OPERATION: HTTP_GET"));
        assertTrue(message.contains("success=false"));
        assertTrue(message.contains("error=Connection timeout"));
    }

    @Test
    void testDatabaseOperationMessage() {
        String message = formatter.databaseOperation(
            "INSERT", 
            "ArticleEntity", 
            true, 
            500, 
            null
        );
        
        assertTrue(message.contains("DATABASE_OPERATION: INSERT"));
        assertTrue(message.contains("entity=ArticleEntity"));
        assertTrue(message.contains("success=true"));
        assertTrue(message.contains("duration_ms=500"));
    }

    @Test
    void testRedisOperationMessage() {
        String message = formatter.redisOperation(
            "SETNX", 
            "drucker:url:123", 
            true, 
            50, 
            null
        );
        
        assertTrue(message.contains("REDIS_OPERATION: SETNX"));
        assertTrue(message.contains("key=drucker:url:123"));
        assertTrue(message.contains("success=true"));
        assertTrue(message.contains("duration_ms=50"));
    }

    @Test
    void testPerformanceSummaryMessage() {
        String message = formatter.performanceSummary(
            "ARTICLE_FETCH", 
            100, 95, 5, 
            150.5, 95.0, 15000
        );
        
        assertTrue(message.contains("PERFORMANCE_SUMMARY: ARTICLE_FETCH performance summary"));
        assertTrue(message.contains("total_operations_count=100"));
        assertTrue(message.contains("successes_count=95"));
        assertTrue(message.contains("failures_count=5"));
        assertTrue(message.contains("avg_duration_ms=150.5"));
        assertTrue(message.contains("success_rate_pct=95.0"));
        assertTrue(message.contains("total_elapsed_ms=15000"));
    }

    @Test
    void testLogMessageBuilderWithNullValues() {
        String message = formatter.createMessage("TEST")
                .message("Test message")
                .field("null_field", null)
                .field("valid_field", "value")
                .build();
        
        assertTrue(message.contains("valid_field=value"));
        assertFalse(message.contains("null_field"));
    }
}