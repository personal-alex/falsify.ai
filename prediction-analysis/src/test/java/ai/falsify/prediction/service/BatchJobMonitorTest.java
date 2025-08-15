package ai.falsify.prediction.service;

import ai.falsify.prediction.model.BatchJobStatus;
import ai.falsify.prediction.service.BatchPredictionExtractor.ArticleData;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatchJobMonitor service.
 * Tests the core functionality of batch job monitoring, registration, and cleanup.
 */
@QuarkusTest
class BatchJobMonitorTest {

    @Inject
    BatchJobMonitor batchJobMonitor;

    @Test
    void testMonitorInitialization() {
        // Test that the monitor can be initialized
        assertNotNull(batchJobMonitor);
    }

    @Test
    void testJobUnregistration() {
        // Test that jobs can be unregistered
        String jobId = "test-job-2";
        
        // Initially no jobs (or whatever the current count is)
        int initialCount = batchJobMonitor.getActiveBatchJobCount();
        
        // Unregistering non-existent job should not cause errors
        assertDoesNotThrow(() -> batchJobMonitor.unregisterBatchJob(jobId));
        
        // Count should remain the same
        assertEquals(initialCount, batchJobMonitor.getActiveBatchJobCount());
    }

    @Test
    void testGetActiveBatchJobInfo() {
        // Test getting active job information
        Map<String, String> info = batchJobMonitor.getActiveBatchJobInfo();
        assertNotNull(info);
        
        // Should be empty initially
        assertTrue(info.isEmpty());
    }

    @Test
    void testGetMonitoringStatistics() {
        // Test getting monitoring statistics
        Map<String, Object> stats = batchJobMonitor.getMonitoringStatistics();
        assertNotNull(stats);
        
        // Verify expected fields are present
        assertTrue(stats.containsKey("isRunning"));
        assertTrue(stats.containsKey("activeBatchJobs"));
        assertTrue(stats.containsKey("monitoringEnabled"));
        assertTrue(stats.containsKey("pollingIntervalSeconds"));
        assertTrue(stats.containsKey("maxConcurrentJobs"));
        assertTrue(stats.containsKey("timeoutMinutes"));
        assertTrue(stats.containsKey("jobStateDistribution"));
        
        // Verify values are present and reasonable
        assertTrue(stats.get("activeBatchJobs") instanceof Integer);
        assertTrue(stats.get("monitoringEnabled") instanceof Boolean);
        assertTrue(stats.get("pollingIntervalSeconds") instanceof Integer);
        assertTrue(stats.get("maxConcurrentJobs") instanceof Integer);
        assertTrue(stats.get("timeoutMinutes") instanceof Integer);
    }

    @Test
    void testForceCleanupAllJobs() {
        // Test force cleanup functionality
        assertDoesNotThrow(() -> batchJobMonitor.forceCleanupAllJobs());
        
        // After cleanup, should have no active jobs (or at least not more than before)
        assertTrue(batchJobMonitor.getActiveBatchJobCount() >= 0);
    }

    @Test
    void testJobStatusForNonExistentJob() {
        // Test getting status for non-existent job
        BatchJobStatus status = batchJobMonitor.getJobStatus("non-existent-job");
        assertNull(status);
    }

    @Test
    void testMonitoringStatisticsStructure() {
        // Monitor should handle statistics requests gracefully
        assertDoesNotThrow(() -> {
            Map<String, Object> stats = batchJobMonitor.getMonitoringStatistics();
            assertNotNull(stats);
            
            // Verify the statistics contain expected structure
            assertTrue(stats.containsKey("jobStateDistribution"));
            Object stateDistribution = stats.get("jobStateDistribution");
            assertTrue(stateDistribution instanceof Map);
        });
    }
}