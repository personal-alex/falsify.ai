package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.JobRecord;
import ai.falsify.crawlers.model.JobStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobTrackerService.
 */
@QuarkusTest

class JobTrackerServiceTest {
    
    @Inject
    JobTrackerService jobTrackerService;
    
    @InjectMock
    WebSocketNotificationService webSocketService;
    
    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing data
        JobRecord.deleteAll();
        reset(webSocketService);
    }
    
    @Test
    @Transactional
    void testTrackJobStart() {
        // Given
        String crawlerId = "test-crawler";
        String requestId = "req-123";
        
        // When
        JobRecord jobRecord = jobTrackerService.trackJobStart(crawlerId, requestId);
        
        // Then
        assertNotNull(jobRecord);
        assertNotNull(jobRecord.jobId);
        assertEquals(crawlerId, jobRecord.crawlerId);
        assertEquals(requestId, jobRecord.requestId);
        assertEquals(JobRecord.JobStatus.RUNNING, jobRecord.status);
        assertNotNull(jobRecord.startTime);
        assertTrue(jobRecord.jobId.startsWith("job-"));
        
        // Verify WebSocket notification
        ArgumentCaptor<JobStatus> captor = ArgumentCaptor.forClass(JobStatus.class);
        verify(webSocketService).broadcastJobStarted(captor.capture());
        JobStatus notifiedStatus = captor.getValue();
        assertEquals(jobRecord.jobId, notifiedStatus.jobId);
        assertEquals(crawlerId, notifiedStatus.crawlerId);
    }
    
    @Test
    @Transactional
    void testTrackJobProgress() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        
        // When
        jobTrackerService.trackJobProgress(jobId, 5, 2, 1, "Processing articles");
        
        // Then
        JobRecord updated = JobRecord.findByJobId(jobId);
        assertNotNull(updated);
        assertEquals(5, updated.articlesProcessed);
        assertEquals(2, updated.articlesSkipped);
        assertEquals(1, updated.articlesFailed);
        assertEquals("Processing articles", updated.currentActivity);
        assertEquals(JobRecord.JobStatus.RUNNING, updated.status);
        
        // Verify WebSocket notification for progress update
        verify(webSocketService).broadcastJobProgress(any(JobStatus.class));
    }
    
    @Test
    @Transactional
    void testTrackJobCompletion() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        
        // When
        jobTrackerService.trackJobCompletion(jobId, 10, 3, 2);
        
        // Then
        JobRecord completed = JobRecord.findByJobId(jobId);
        assertNotNull(completed);
        assertEquals(JobRecord.JobStatus.COMPLETED, completed.status);
        assertEquals(10, completed.articlesProcessed);
        assertEquals(3, completed.articlesSkipped);
        assertEquals(2, completed.articlesFailed);
        assertEquals("Completed successfully", completed.currentActivity);
        assertNotNull(completed.endTime);
        
        // Verify WebSocket notification
        ArgumentCaptor<JobStatus> captor = ArgumentCaptor.forClass(JobStatus.class);
        verify(webSocketService).broadcastJobCompleted(captor.capture());
        JobStatus notifiedStatus = captor.getValue();
        assertEquals(jobId, notifiedStatus.jobId);
        assertEquals(JobRecord.JobStatus.COMPLETED, notifiedStatus.status);
    }
    
    @Test
    @Transactional
    void testTrackJobFailure() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        String errorMessage = "Network connection failed";
        
        // When
        jobTrackerService.trackJobFailure(jobId, errorMessage);
        
        // Then
        JobRecord failed = JobRecord.findByJobId(jobId);
        assertNotNull(failed);
        assertEquals(JobRecord.JobStatus.FAILED, failed.status);
        assertEquals(errorMessage, failed.errorMessage);
        assertEquals("Failed", failed.currentActivity);
        assertNotNull(failed.endTime);
        
        // Verify WebSocket notification
        ArgumentCaptor<JobStatus> captor = ArgumentCaptor.forClass(JobStatus.class);
        verify(webSocketService).broadcastJobFailed(captor.capture());
        JobStatus notifiedStatus = captor.getValue();
        assertEquals(jobId, notifiedStatus.jobId);
        assertEquals(JobRecord.JobStatus.FAILED, notifiedStatus.status);
    }
    
    @Test
    @Transactional
    void testCancelJob() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        
        // When
        jobTrackerService.cancelJob(jobId);
        
        // Then
        JobRecord cancelled = JobRecord.findByJobId(jobId);
        assertNotNull(cancelled);
        assertEquals(JobRecord.JobStatus.CANCELLED, cancelled.status);
        assertEquals("Cancelled", cancelled.currentActivity);
        assertNotNull(cancelled.endTime);
        
        // Verify WebSocket notification (uses broadcastJobFailed for cancelled jobs)
        verify(webSocketService).broadcastJobFailed(any(JobStatus.class));
    }
    
    @Test
    @Transactional
    void testGetRecentJobs() {
        // Given
        String crawlerId = "test-crawler";
        
        // Create multiple jobs
        for (int i = 0; i < 7; i++) {
            JobRecord job = jobTrackerService.trackJobStart(crawlerId, "req-" + i);
            if (i < 3) {
                jobTrackerService.trackJobCompletion(job.jobId, i * 2, i, 0);
            }
        }
        
        // When
        List<JobStatus> recentJobs = jobTrackerService.getRecentJobs(crawlerId);
        
        // Then
        assertEquals(5, recentJobs.size()); // Should return only 5 most recent
        
        // Verify they are ordered by start time descending (most recent first)
        for (int i = 0; i < recentJobs.size() - 1; i++) {
            assertTrue(recentJobs.get(i).startTime.isAfter(recentJobs.get(i + 1).startTime) ||
                      recentJobs.get(i).startTime.equals(recentJobs.get(i + 1).startTime));
        }
    }
    
    @Test
    @Transactional
    void testGetJobHistory() {
        // Given
        String crawlerId = "test-crawler";
        
        // Create multiple jobs
        for (int i = 0; i < 15; i++) {
            JobRecord job = jobTrackerService.trackJobStart(crawlerId, "req-" + i);
            jobTrackerService.trackJobCompletion(job.jobId, i, 0, 0);
        }
        
        // When
        List<JobStatus> page1 = jobTrackerService.getJobHistory(crawlerId, 0, 10);
        List<JobStatus> page2 = jobTrackerService.getJobHistory(crawlerId, 1, 10);
        
        // Then
        assertEquals(10, page1.size());
        assertEquals(5, page2.size());
        
        // Verify no overlap between pages
        assertNotEquals(page1.get(0).jobId, page2.get(0).jobId);
    }
    
    @Test
    @Transactional
    void testGetJob() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        
        // When
        Optional<JobStatus> result = jobTrackerService.getJob(jobId);
        
        // Then
        assertTrue(result.isPresent());
        JobStatus jobStatus = result.get();
        assertEquals(jobId, jobStatus.jobId);
        assertEquals("test-crawler", jobStatus.crawlerId);
        assertEquals(JobRecord.JobStatus.RUNNING, jobStatus.status);
    }
    
    @Test
    @Transactional
    void testGetJobNotFound() {
        // When
        Optional<JobStatus> result = jobTrackerService.getJob("non-existent-job");
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    @Transactional
    void testGetRunningJobs() {
        // Given
        String crawlerId1 = "crawler-1";
        String crawlerId2 = "crawler-2";
        
        JobRecord job1 = jobTrackerService.trackJobStart(crawlerId1, "req-1");
        JobRecord job2 = jobTrackerService.trackJobStart(crawlerId2, "req-2");
        JobRecord job3 = jobTrackerService.trackJobStart(crawlerId1, "req-3");
        
        // Complete one job
        jobTrackerService.trackJobCompletion(job2.jobId, 5, 0, 0);
        
        // When
        List<JobStatus> runningJobs = jobTrackerService.getRunningJobs();
        
        // Then
        assertEquals(2, runningJobs.size());
        assertTrue(runningJobs.stream().allMatch(job -> job.status == JobRecord.JobStatus.RUNNING));
    }
    
    @Test
    @Transactional
    void testGetRunningJobsForCrawler() {
        // Given
        String crawlerId1 = "crawler-1";
        String crawlerId2 = "crawler-2";
        
        JobRecord job1 = jobTrackerService.trackJobStart(crawlerId1, "req-1");
        JobRecord job2 = jobTrackerService.trackJobStart(crawlerId2, "req-2");
        JobRecord job3 = jobTrackerService.trackJobStart(crawlerId1, "req-3");
        
        // When
        List<JobStatus> runningJobs = jobTrackerService.getRunningJobs(crawlerId1);
        
        // Then
        assertEquals(2, runningJobs.size());
        assertTrue(runningJobs.stream().allMatch(job -> 
            job.status == JobRecord.JobStatus.RUNNING && job.crawlerId.equals(crawlerId1)));
    }
    
    @Test
    @Transactional
    void testHasRunningJobs() {
        // Given
        String crawlerId = "test-crawler";
        
        // Initially no running jobs
        assertFalse(jobTrackerService.hasRunningJobs(crawlerId));
        
        // Start a job
        JobRecord job = jobTrackerService.trackJobStart(crawlerId, "req-1");
        
        // Now has running jobs
        assertTrue(jobTrackerService.hasRunningJobs(crawlerId));
        
        // Complete the job
        jobTrackerService.trackJobCompletion(job.jobId, 5, 0, 0);
        
        // No longer has running jobs
        assertFalse(jobTrackerService.hasRunningJobs(crawlerId));
    }
    
    @Test
    @Transactional
    void testTrackJobProgressForUnknownJob() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress("unknown-job", 1, 0, 0, "test");
        });
        
        // Should not send WebSocket notification for unknown job
        verify(webSocketService, never()).broadcastJobProgress(any(JobStatus.class));
    }
    
    @Test
    @Transactional
    void testTrackJobProgressForCompletedJob() {
        // Given
        JobRecord jobRecord = jobTrackerService.trackJobStart("test-crawler", "req-123");
        String jobId = jobRecord.jobId;
        jobTrackerService.trackJobCompletion(jobId, 5, 0, 0);
        
        // When/Then - should not throw exception but should not update
        assertDoesNotThrow(() -> {
            jobTrackerService.trackJobProgress(jobId, 10, 0, 0, "should not update");
        });
        
        // Verify job was not updated
        JobRecord updated = JobRecord.findByJobId(jobId);
        assertEquals(5, updated.articlesProcessed); // Still the completion value
        assertEquals("Completed successfully", updated.currentActivity); // Still completion activity
    }
    
    @Test
    @Transactional
    void testRecoverOrphanedJobs() {
        // Given - create some running jobs directly in database
        JobRecord job1 = new JobRecord("crawler-1", "job-1", "req-1");
        job1.persist();
        JobRecord job2 = new JobRecord("crawler-2", "job-2", "req-2");
        job2.persist();
        JobRecord job3 = new JobRecord("crawler-1", "job-3", "req-3");
        job3.markCompleted(5, 0, 0);
        job3.persist();
        
        // When
        jobTrackerService.recoverOrphanedJobs();
        
        // Then
        JobRecord recovered1 = JobRecord.findByJobId("job-1");
        JobRecord recovered2 = JobRecord.findByJobId("job-2");
        JobRecord notRecovered = JobRecord.findByJobId("job-3");
        
        assertEquals(JobRecord.JobStatus.FAILED, recovered1.status);
        assertEquals("Job was interrupted by service restart", recovered1.errorMessage);
        assertEquals(JobRecord.JobStatus.FAILED, recovered2.status);
        assertEquals("Job was interrupted by service restart", recovered2.errorMessage);
        assertEquals(JobRecord.JobStatus.COMPLETED, notRecovered.status); // Should not be changed
    }
}