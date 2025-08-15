package ai.falsify.prediction.service;

import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.model.BatchState;
import ai.falsify.prediction.model.NotificationEventSerializer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WebSocketMessageFormatterTest {
    
    @Inject
    WebSocketMessageFormatter formatter;
    

    
    @Test
    void testFormatBatchSubmitted() {
        String result = formatter.formatBatchSubmitted("job123", "batch456", 10);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.submitted"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("10"));
        
        // Verify it's valid JSON by checking format
        assertTrue(NotificationEventSerializer.isValidJson(result));
    }
    
    @Test
    void testFormatBatchProgress() {
        String result = formatter.formatBatchProgress("job123", "batch456", 5, 10, 1);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.progress"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("\"completedRequests\":5"));
        assertTrue(result.contains("\"totalRequests\":10"));
        assertTrue(result.contains("\"failedRequests\":1"));
        assertTrue(result.contains("\"progressPercentage\":60.0"));
        assertTrue(result.contains("\"status\":\"PROCESSING\""));
    }
    
    @Test
    void testFormatBatchCompleted() {
        String result = formatter.formatBatchCompleted("job123", "batch456", 10, 9, 1, 25);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.completed"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("\"totalRequests\":10"));
        assertTrue(result.contains("\"completedRequests\":9"));
        assertTrue(result.contains("\"failedRequests\":1"));
        assertTrue(result.contains("\"status\":\"COMPLETED\""));
        assertTrue(result.contains("\"progressPercentage\":100.0"));
        assertTrue(result.contains("\"successRate\":90.0"));
    }
    
    @Test
    void testFormatBatchFailed() {
        String result = formatter.formatBatchFailed("job123", "batch456", "API error", 3);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.failed"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("API error"));
        assertTrue(result.contains("\"status\":\"FAILED\""));
        assertTrue(result.contains("\"completedRequests\":3"));
        assertTrue(result.contains("\"failedRequests\":1"));
    }
    
    @Test
    void testFormatBatchStatusUpdate() {
        BatchJobStatus status = new BatchJobStatus(
            "job123",
            "batch456", 
            BatchState.PROCESSING,
            10,
            7,
            1,
            Instant.now().minusSeconds(60),
            Instant.now(),
            null
        );
        
        String result = formatter.formatBatchStatusUpdate(status);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.progress"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("\"totalRequests\":10"));
        assertTrue(result.contains("\"completedRequests\":7"));
        assertTrue(result.contains("\"failedRequests\":1"));
        assertTrue(result.contains("\"progressPercentage\":80.0"));
        assertTrue(result.contains("\"status\":\"PROCESSING\""));
        assertTrue(result.contains("\"state\":\"PROCESSING\""));
    }
    
    @Test
    void testFormatBatchTimeout() {
        String result = formatter.formatBatchTimeout("job123", "batch456", 30, 5);
        
        assertNotNull(result);
        assertTrue(result.contains("batch.timeout"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("\"timeoutMinutes\":30"));
        assertTrue(result.contains("\"completedRequests\":5"));
        assertTrue(result.contains("\"status\":\"TIMEOUT\""));
    }
    
    @Test
    void testFormatBatchCancelled() {
        String result = formatter.formatBatchCancelled("job123", "batch456", "User requested");
        
        assertNotNull(result);
        assertTrue(result.contains("batch.cancelled"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("batch456"));
        assertTrue(result.contains("\"status\":\"CANCELLED\""));
        assertTrue(result.contains("\"progressPercentage\":0.0"));
    }
    
    @Test
    void testFormatJobStatusUpdate() {
        String result = formatter.formatJobStatusUpdate("job123", "COMPLETED");
        
        assertNotNull(result);
        assertTrue(result.contains("job.status.update"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("COMPLETED"));
    }
    
    @Test
    void testFormatProgressUpdate() {
        String result = formatter.formatProgressUpdate("job123", "article456", 3);
        
        assertNotNull(result);
        assertTrue(result.contains("job.progress.update"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("article456"));
        assertTrue(result.contains("\"predictionsFound\":3"));
    }
    
    @Test
    void testFormatJobCompleted() {
        String result = formatter.formatJobCompleted("job123", 15, 42);
        
        assertNotNull(result);
        assertTrue(result.contains("job.completed"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("\"processedArticles\":15"));
        assertTrue(result.contains("\"totalPredictions\":42"));
    }
    
    @Test
    void testFormatJobFailed() {
        String result = formatter.formatJobFailed("job123", "Network timeout");
        
        assertNotNull(result);
        assertTrue(result.contains("job.failed"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("Network timeout"));
    }
    
    @Test
    void testFormatJobCancelled() {
        String result = formatter.formatJobCancelled("job123");
        
        assertNotNull(result);
        assertTrue(result.contains("job.cancelled"));
        assertTrue(result.contains("job123"));
    }
    
    @Test
    void testFormatPredictionExtracted() {
        String result = formatter.formatPredictionExtracted("job123", "article456", "Stock will rise", 8);
        
        assertNotNull(result);
        assertTrue(result.contains("prediction.extracted"));
        assertTrue(result.contains("job123"));
        assertTrue(result.contains("article456"));
        assertTrue(result.contains("Stock will rise"));
        assertTrue(result.contains("\"rating\":8"));
    }
    
    @Test
    void testProgressPercentageCalculation() {
        // Test zero division
        String result = formatter.formatBatchProgress("job123", "batch456", 0, 0, 0);
        assertTrue(result.contains("\"progressPercentage\":0"));
        
        // Test normal calculation
        result = formatter.formatBatchProgress("job123", "batch456", 3, 4, 0);
        assertTrue(result.contains("\"progressPercentage\":75"));
        
        // Test rounding
        result = formatter.formatBatchProgress("job123", "batch456", 1, 3, 0);
        assertTrue(result.contains("\"progressPercentage\":33"));
    }
    
    @Test
    void testSuccessRateCalculation() {
        // Test zero division
        String result = formatter.formatBatchCompleted("job123", "batch456", 0, 0, 0, 0);
        assertTrue(result.contains("\"successRate\":0"));
        
        // Test normal calculation
        result = formatter.formatBatchCompleted("job123", "batch456", 10, 8, 2, 20);
        assertTrue(result.contains("\"successRate\":80"));
        
        // Test perfect success
        result = formatter.formatBatchCompleted("job123", "batch456", 5, 5, 0, 15);
        assertTrue(result.contains("\"successRate\":100"));
    }
}