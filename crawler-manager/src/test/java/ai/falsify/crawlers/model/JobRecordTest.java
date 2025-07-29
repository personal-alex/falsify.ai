package ai.falsify.crawlers.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JobRecord entity.
 */
@QuarkusTest

class JobRecordTest {
    
    @Inject
    EntityManager entityManager;
    
    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing data
        JobRecord.deleteAll();
    }
    
    @Test
    @Transactional
    void testJobRecordCreation() {
        // Given
        String crawlerId = "test-crawler";
        String jobId = "job-123";
        String requestId = "req-456";
        
        // When
        JobRecord jobRecord = new JobRecord(crawlerId, jobId, requestId);
        jobRecord.persist();
        
        // Then
        assertNotNull(jobRecord.id);
        assertEquals(crawlerId, jobRecord.crawlerId);
        assertEquals(jobId, jobRecord.jobId);
        assertEquals(requestId, jobRecord.requestId);
        assertEquals(JobRecord.JobStatus.RUNNING, jobRecord.status);
        assertNotNull(jobRecord.startTime);
        assertNotNull(jobRecord.lastUpdated);
        assertNull(jobRecord.endTime);
        assertEquals(0, jobRecord.articlesProcessed);
        assertEquals(0, jobRecord.articlesSkipped);
        assertEquals(0, jobRecord.articlesFailed);
        assertEquals("Starting crawl operation", jobRecord.currentActivity);
    }
    
    @Test
    @Transactional
    void testUpdateProgress() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        Instant originalLastUpdated = jobRecord.lastUpdated;
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        jobRecord.updateProgress(5, 2, 1, "Processing articles");
        
        // Then
        assertEquals(5, jobRecord.articlesProcessed);
        assertEquals(2, jobRecord.articlesSkipped);
        assertEquals(1, jobRecord.articlesFailed);
        assertEquals("Processing articles", jobRecord.currentActivity);
        assertTrue(jobRecord.lastUpdated.isAfter(originalLastUpdated));
        assertEquals(JobRecord.JobStatus.RUNNING, jobRecord.status);
    }
    
    @Test
    @Transactional
    void testMarkCompleted() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        
        // When
        jobRecord.markCompleted(10, 3, 2);
        
        // Then
        assertEquals(JobRecord.JobStatus.COMPLETED, jobRecord.status);
        assertEquals(10, jobRecord.articlesProcessed);
        assertEquals(3, jobRecord.articlesSkipped);
        assertEquals(2, jobRecord.articlesFailed);
        assertEquals("Completed successfully", jobRecord.currentActivity);
        assertNotNull(jobRecord.endTime);
        assertNotNull(jobRecord.getDurationMs());
        assertTrue(jobRecord.isCompleted());
        assertFalse(jobRecord.isRunning());
    }
    
    @Test
    @Transactional
    void testMarkFailed() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        String errorMessage = "Network connection failed";
        
        // When
        jobRecord.markFailed(errorMessage);
        
        // Then
        assertEquals(JobRecord.JobStatus.FAILED, jobRecord.status);
        assertEquals(errorMessage, jobRecord.errorMessage);
        assertEquals("Failed", jobRecord.currentActivity);
        assertNotNull(jobRecord.endTime);
        assertNotNull(jobRecord.getDurationMs());
        assertTrue(jobRecord.isCompleted());
        assertFalse(jobRecord.isRunning());
    }
    
    @Test
    @Transactional
    void testMarkCancelled() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        
        // When
        jobRecord.markCancelled();
        
        // Then
        assertEquals(JobRecord.JobStatus.CANCELLED, jobRecord.status);
        assertEquals("Cancelled", jobRecord.currentActivity);
        assertNotNull(jobRecord.endTime);
        assertNotNull(jobRecord.getDurationMs());
        assertTrue(jobRecord.isCompleted());
        assertFalse(jobRecord.isRunning());
    }
    
    @Test
    @Transactional
    void testGetElapsedTimeMs() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        
        // Wait a bit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        long elapsedTime = jobRecord.getElapsedTimeMs();
        
        // Then
        assertTrue(elapsedTime > 0);
        assertTrue(elapsedTime >= 50);
    }
    
    @Test
    @Transactional
    void testGetTotalArticlesAttempted() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.updateProgress(10, 5, 3, "Processing");
        jobRecord.persist();
        
        // When
        int total = jobRecord.getTotalArticlesAttempted();
        
        // Then
        assertEquals(18, total); // 10 + 5 + 3
    }
    
    @Test
    @Transactional
    void testGetSuccessRate() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.updateProgress(8, 1, 1, "Processing");
        jobRecord.persist();
        
        // When
        double successRate = jobRecord.getSuccessRate();
        
        // Then
        assertEquals(80.0, successRate, 0.01); // 8 out of 10 = 80%
    }
    
    @Test
    @Transactional
    void testGetSuccessRateWithZeroArticles() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        
        // When
        double successRate = jobRecord.getSuccessRate();
        
        // Then
        assertEquals(0.0, successRate, 0.01);
    }
    
    @Test
    @Transactional
    void testFindByJobId() {
        // Given
        String jobId = "job-unique-123";
        JobRecord jobRecord = new JobRecord("test-crawler", jobId, "req-456");
        jobRecord.persist();
        
        // When
        JobRecord found = JobRecord.findByJobId(jobId);
        
        // Then
        assertNotNull(found);
        assertEquals(jobId, found.jobId);
        assertEquals("test-crawler", found.crawlerId);
    }
    
    @Test
    @Transactional
    void testFindByJobIdNotFound() {
        // When
        JobRecord found = JobRecord.findByJobId("non-existent-job");
        
        // Then
        assertNull(found);
    }
    
    @Test
    @Transactional
    void testJobRecordPersistence() {
        // Given
        JobRecord jobRecord = new JobRecord("test-crawler", "job-123", "req-456");
        jobRecord.persist();
        Long id = jobRecord.id;
        
        // Clear the persistence context
        entityManager.flush();
        entityManager.clear();
        
        // When
        JobRecord retrieved = JobRecord.findById(id);
        
        // Then
        assertNotNull(retrieved);
        assertEquals("test-crawler", retrieved.crawlerId);
        assertEquals("job-123", retrieved.jobId);
        assertEquals("req-456", retrieved.requestId);
        assertEquals(JobRecord.JobStatus.RUNNING, retrieved.status);
    }
}