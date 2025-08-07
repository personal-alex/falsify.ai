package ai.falsify.crawlers.common.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ai.falsify.crawlers.common.model.AnalysisStatus.*;

@QuarkusTest
class AnalysisJobEntityTest {

    @Inject
    EntityManager entityManager;

    private AnalysisJobEntity testJob;
    private ArticleEntity testArticle;
    private AuthorEntity testAuthor;

    @BeforeEach
    void setUp() {
        // Initialize test data - will be created in each test method's transaction
        testAuthor = null;
        testArticle = null;
        testJob = null;
    }
    
    private void cleanupTestData() {
        // Delete dependent records first to avoid foreign key constraint violations
        try {
            entityManager.createNativeQuery("DELETE FROM analysis_job_articles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM article_predictions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM prediction_instances").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM analysis_jobs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM predictions").executeUpdate();
        } catch (Exception e) {
            // Tables might not exist yet in some test scenarios, ignore
        }
        
        // Now delete the main entities
        AnalysisJobEntity.deleteAll();
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
        entityManager.flush();
    }
    
    private void createTestData() {
        // Clean up any existing test data in proper order to avoid foreign key constraint violations
        cleanupTestData();

        // Create test author
        testAuthor = new AuthorEntity("Test Author", "http://example.com/avatar.jpg");
        testAuthor.persist();

        // Create test article
        Article article = new Article("Test Article", "http://example.com/test", "Test content");
        testArticle = new ArticleEntity(article, "test-crawler", testAuthor);
        testArticle.persist();

        // Create test analysis job
        testJob = new AnalysisJobEntity("mock");
        testJob.totalArticles = 5;
        testJob.processedArticles = 2;
        testJob.predictionsFound = 3;
        testJob.persist();
        
        // Ensure all entities are flushed to database
        entityManager.flush();
    }

    @Test
    @TestTransaction
    @DisplayName("Should create analysis job with default values")
    void shouldCreateAnalysisJobWithDefaultValues() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.persist();

        assertNotNull(job.id);
        assertNotNull(job.jobId);
        assertEquals(PENDING, job.status);
        assertNotNull(job.startedAt);
        assertEquals(Integer.valueOf(0), job.totalArticles);
        assertEquals(Integer.valueOf(0), job.processedArticles);
        assertEquals(Integer.valueOf(0), job.predictionsFound);
        assertEquals("mock", job.analysisType);
        assertNull(job.completedAt);
        assertNull(job.errorMessage);
    }

    @Test
    @TestTransaction
    @DisplayName("Should create analysis job with specified type")
    void shouldCreateAnalysisJobWithSpecifiedType() {
        AnalysisJobEntity job = new AnalysisJobEntity("llm");
        job.persist();

        assertEquals("llm", job.analysisType);
        assertEquals(PENDING, job.status);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null analysis type")
    void shouldHandleNullAnalysisType() {
        AnalysisJobEntity job = new AnalysisJobEntity(null);
        job.persist();

        assertEquals("mock", job.analysisType); // Should default to "mock"
    }

    @Test
    @TestTransaction
    @DisplayName("Should find job by job ID")
    void shouldFindJobByJobId() {
        createTestData();
        
        AnalysisJobEntity found = AnalysisJobEntity.findByJobId(testJob.jobId);
        
        assertNotNull(found);
        assertEquals(testJob.id, found.id);
        assertEquals(testJob.jobId, found.jobId);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return null when job ID not found")
    void shouldReturnNullWhenJobIdNotFound() {
        AnalysisJobEntity found = AnalysisJobEntity.findByJobId("non-existent-job-id");
        assertNull(found);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null and empty job ID in findByJobId")
    void shouldHandleNullAndEmptyJobIdInFindByJobId() {
        assertNull(AnalysisJobEntity.findByJobId(null));
        assertNull(AnalysisJobEntity.findByJobId(""));
        assertNull(AnalysisJobEntity.findByJobId("   "));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find jobs by status")
    void shouldFindJobsByStatus() {
        createTestData();
        
        // Create additional jobs with different statuses
        AnalysisJobEntity runningJob = new AnalysisJobEntity("mock");
        runningJob.status = RUNNING;
        runningJob.persist();

        AnalysisJobEntity completedJob = new AnalysisJobEntity("llm");
        completedJob.status = COMPLETED;
        completedJob.persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<AnalysisJobEntity> pendingJobs = AnalysisJobEntity.findByStatus(PENDING);
        assertEquals(1, pendingJobs.size());
        assertEquals(testJob.id, pendingJobs.get(0).id);

        List<AnalysisJobEntity> runningJobs = AnalysisJobEntity.findByStatus(RUNNING);
        assertEquals(1, runningJobs.size());
        assertEquals(runningJob.id, runningJobs.get(0).id);

        List<AnalysisJobEntity> completedJobs = AnalysisJobEntity.findByStatus(COMPLETED);
        assertEquals(1, completedJobs.size());
        assertEquals(completedJob.id, completedJobs.get(0).id);

        // Test with null status
        List<AnalysisJobEntity> nullResult = AnalysisJobEntity.findByStatus(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find recent jobs")
    void shouldFindRecentJobs() {
        // Create additional jobs
        new AnalysisJobEntity("mock").persist();
        new AnalysisJobEntity("llm").persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<AnalysisJobEntity> recent = AnalysisJobEntity.findRecent(2);
        assertEquals(2, recent.size());
        
        // Should be ordered by startedAt DESC
        assertTrue(recent.get(0).startedAt.isAfter(recent.get(1).startedAt) || 
                  recent.get(0).startedAt.equals(recent.get(1).startedAt));
    }

    @Test
    @TestTransaction
    @DisplayName("Should count jobs by status")
    void shouldCountJobsByStatus() {
        createTestData();
        
        // Create additional jobs with different statuses
        new AnalysisJobEntity("mock").persist(); // PENDING
        AnalysisJobEntity runningJob = new AnalysisJobEntity("mock");
        runningJob.status = RUNNING;
        runningJob.persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        assertEquals(2, AnalysisJobEntity.countByStatus(PENDING));
        assertEquals(1, AnalysisJobEntity.countByStatus(RUNNING));
        assertEquals(0, AnalysisJobEntity.countByStatus(COMPLETED));
        assertEquals(0, AnalysisJobEntity.countByStatus(null));
    }

    @Test
    @TestTransaction
    @DisplayName("Should mark job as started")
    void shouldMarkJobAsStarted() {
        createTestData();
        
        Instant before = Instant.now().minusSeconds(1);
        testJob.markStarted();
        Instant after = Instant.now().plusSeconds(1);

        assertEquals(RUNNING, testJob.status);
        assertTrue(testJob.startedAt.isAfter(before));
        assertTrue(testJob.startedAt.isBefore(after));
    }

    @Test
    @TestTransaction
    @DisplayName("Should mark job as completed")
    void shouldMarkJobAsCompleted() {
        createTestData();
        
        Instant before = Instant.now().minusSeconds(1);
        testJob.markCompleted();
        Instant after = Instant.now().plusSeconds(1);

        assertEquals(COMPLETED, testJob.status);
        assertNotNull(testJob.completedAt);
        assertTrue(testJob.completedAt.isAfter(before));
        assertTrue(testJob.completedAt.isBefore(after));
    }

    @Test
    @TestTransaction
    @DisplayName("Should mark job as failed with error message")
    void shouldMarkJobAsFailedWithErrorMessage() {
        createTestData();
        
        String errorMessage = "Analysis failed due to network error";
        Instant before = Instant.now().minusSeconds(1);
        testJob.markFailed(errorMessage);
        Instant after = Instant.now().plusSeconds(1);

        assertEquals(FAILED, testJob.status);
        assertEquals(errorMessage, testJob.errorMessage);
        assertNotNull(testJob.completedAt);
        assertTrue(testJob.completedAt.isAfter(before));
        assertTrue(testJob.completedAt.isBefore(after));
    }

    @Test
    @TestTransaction
    @DisplayName("Should mark job as cancelled")
    void shouldMarkJobAsCancelled() {
        createTestData();
        
        Instant before = Instant.now().minusSeconds(1);
        testJob.markCancelled();
        Instant after = Instant.now().plusSeconds(1);

        assertEquals(CANCELLED, testJob.status);
        assertNotNull(testJob.completedAt);
        assertTrue(testJob.completedAt.isAfter(before));
        assertTrue(testJob.completedAt.isBefore(after));
    }

    @Test
    @TestTransaction
    @DisplayName("Should update progress")
    void shouldUpdateProgress() {
        createTestData();
        
        testJob.updateProgress(8, 12);

        assertEquals(Integer.valueOf(8), testJob.processedArticles);
        assertEquals(Integer.valueOf(12), testJob.predictionsFound);
    }

    @Test
    @TestTransaction
    @DisplayName("Should calculate progress percentage")
    void shouldCalculateProgressPercentage() {
        createTestData();
        
        testJob.totalArticles = 10;
        testJob.processedArticles = 3;

        assertEquals(30.0, testJob.getProgressPercentage(), 0.01);

        // Test with completed job
        testJob.processedArticles = 10;
        assertEquals(100.0, testJob.getProgressPercentage(), 0.01);

        // Test with over-completion (should cap at 100%)
        testJob.processedArticles = 12;
        assertEquals(100.0, testJob.getProgressPercentage(), 0.01);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle edge cases in progress percentage")
    void shouldHandleEdgeCasesInProgressPercentage() {
        createTestData();
        
        // Test with null totalArticles
        testJob.totalArticles = null;
        assertEquals(0.0, testJob.getProgressPercentage(), 0.01);

        // Test with zero totalArticles
        testJob.totalArticles = 0;
        assertEquals(0.0, testJob.getProgressPercentage(), 0.01);

        // Test with null processedArticles
        testJob.totalArticles = 10;
        testJob.processedArticles = null;
        assertEquals(0.0, testJob.getProgressPercentage(), 0.01);
    }

    @Test
    @TestTransaction
    @DisplayName("Should check if job is terminal")
    void shouldCheckIfJobIsTerminal() {
        createTestData();
        
        // Test non-terminal states
        testJob.status = PENDING;
        assertFalse(testJob.isTerminal());

        testJob.status = RUNNING;
        assertFalse(testJob.isTerminal());

        // Test terminal states
        testJob.status = COMPLETED;
        assertTrue(testJob.isTerminal());

        testJob.status = FAILED;
        assertTrue(testJob.isTerminal());

        testJob.status = CANCELLED;
        assertTrue(testJob.isTerminal());
    }

    @Test
    @TestTransaction
    @DisplayName("Should calculate job duration")
    void shouldCalculateJobDuration() {
        createTestData();
        
        // Test with completed job
        testJob.startedAt = Instant.now().minusSeconds(60);
        testJob.completedAt = Instant.now();
        
        Long duration = testJob.getDurationMillis();
        assertNotNull(duration);
        assertTrue(duration >= 59000 && duration <= 61000); // Around 60 seconds

        // Test with running job (should use current time)
        testJob.completedAt = null;
        duration = testJob.getDurationMillis();
        assertNotNull(duration);
        assertTrue(duration >= 59000); // At least 59 seconds

        // Test with null startedAt
        testJob.startedAt = null;
        assertNull(testJob.getDurationMillis());
    }

    @Test
    @TestTransaction
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        createTestData();
        
        String toString = testJob.toString();
        
        assertTrue(toString.contains("AnalysisJobEntity"));
        assertTrue(toString.contains("id=" + testJob.id));
        assertTrue(toString.contains("jobId='" + testJob.jobId + "'"));
        assertTrue(toString.contains("status=PENDING"));
        assertTrue(toString.contains("analysisType='mock'"));
        assertTrue(toString.contains("totalArticles=5"));
        assertTrue(toString.contains("processedArticles=2"));
        assertTrue(toString.contains("predictionsFound=3"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should generate unique job IDs")
    void shouldGenerateUniqueJobIds() {
        AnalysisJobEntity job1 = new AnalysisJobEntity();
        AnalysisJobEntity job2 = new AnalysisJobEntity();
        
        assertNotNull(job1.jobId);
        assertNotNull(job2.jobId);
        assertNotEquals(job1.jobId, job2.jobId);
    }

    @Test
    @TestTransaction
    @DisplayName("Should set started timestamp automatically")
    void shouldSetStartedTimestampAutomatically() {
        Instant before = Instant.now().minusSeconds(1);
        AnalysisJobEntity job = new AnalysisJobEntity();
        Instant after = Instant.now().plusSeconds(1);
        
        job.persist();

        assertNotNull(job.startedAt);
        assertTrue(job.startedAt.isAfter(before));
        assertTrue(job.startedAt.isBefore(after));
    }

    @Test
    @TestTransaction
    @DisplayName("Should manage analyzed articles relationship")
    void shouldManageAnalyzedArticlesRelationship() {
        createTestData();
        
        // Test adding article to job
        testJob.analyzedArticles.add(testArticle);
        entityManager.flush();

        assertTrue(testJob.analyzedArticles.contains(testArticle));
        assertEquals(1, testJob.analyzedArticles.size());

        // Test removing article from job
        testJob.analyzedArticles.remove(testArticle);
        entityManager.flush();

        assertFalse(testJob.analyzedArticles.contains(testArticle));
        assertEquals(0, testJob.analyzedArticles.size());
    }

    @Test
    @TestTransaction
    @DisplayName("Should delete old completed jobs")
    void shouldDeleteOldCompletedJobs() {
        // Clean up existing data
        cleanupTestData();

        // Create old completed job
        AnalysisJobEntity oldCompletedJob = new AnalysisJobEntity("mock");
        oldCompletedJob.status = COMPLETED;
        oldCompletedJob.startedAt = Instant.now().minusSeconds(40 * 24 * 60 * 60L); // 40 days ago
        oldCompletedJob.completedAt = Instant.now().minusSeconds(39 * 24 * 60 * 60L); // 39 days ago
        oldCompletedJob.persist();

        // Create old failed job
        AnalysisJobEntity oldFailedJob = new AnalysisJobEntity("llm");
        oldFailedJob.status = FAILED;
        oldFailedJob.startedAt = Instant.now().minusSeconds(35 * 24 * 60 * 60L); // 35 days ago
        oldFailedJob.completedAt = Instant.now().minusSeconds(34 * 24 * 60 * 60L); // 34 days ago
        oldFailedJob.persist();

        // Create old cancelled job
        AnalysisJobEntity oldCancelledJob = new AnalysisJobEntity("mock");
        oldCancelledJob.status = CANCELLED;
        oldCancelledJob.startedAt = Instant.now().minusSeconds(32 * 24 * 60 * 60L); // 32 days ago
        oldCancelledJob.completedAt = Instant.now().minusSeconds(31 * 24 * 60 * 60L); // 31 days ago
        oldCancelledJob.persist();

        // Create recent completed job (should not be deleted)
        AnalysisJobEntity recentJob = new AnalysisJobEntity("mock");
        recentJob.status = COMPLETED;
        recentJob.startedAt = Instant.now().minusSeconds(20 * 24 * 60 * 60L); // 20 days ago
        recentJob.completedAt = Instant.now().minusSeconds(19 * 24 * 60 * 60L); // 19 days ago
        recentJob.persist();

        // Create running job (should not be deleted)
        AnalysisJobEntity runningJob = new AnalysisJobEntity("llm");
        runningJob.status = RUNNING;
        runningJob.startedAt = Instant.now().minusSeconds(40 * 24 * 60 * 60L); // 40 days ago
        runningJob.persist();

        entityManager.flush();

        // Verify initial count
        assertEquals(5, AnalysisJobEntity.count());

        // Delete jobs older than 30 days
        long deletedCount = AnalysisJobEntity.deleteOldJobs(30);

        // Should delete 3 old terminal jobs (completed, failed, cancelled)
        assertEquals(3, deletedCount);
        assertEquals(2, AnalysisJobEntity.count()); // Recent job and running job should remain

        // Verify the remaining jobs are the correct ones
        List<AnalysisJobEntity> remainingJobs = AnalysisJobEntity.listAll();
        assertTrue(remainingJobs.stream().anyMatch(job -> job.id.equals(recentJob.id)));
        assertTrue(remainingJobs.stream().anyMatch(job -> job.id.equals(runningJob.id)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should use default 30 days for cleanup")
    void shouldUseDefault30DaysForCleanup() {
        // Clean up existing data
        AnalysisJobEntity.deleteAll();
        entityManager.flush();

        // Create old completed job (35 days old)
        AnalysisJobEntity oldJob = new AnalysisJobEntity("mock");
        oldJob.status = COMPLETED;
        oldJob.startedAt = Instant.now().minusSeconds(35 * 24 * 60 * 60L);
        oldJob.completedAt = Instant.now().minusSeconds(34 * 24 * 60 * 60L);
        oldJob.persist();

        entityManager.flush();

        // Delete using default (30 days)
        long deletedCount = AnalysisJobEntity.deleteOldJobs();
        assertEquals(1, deletedCount);
    }

    @Test
    @TestTransaction
    @DisplayName("Should validate days parameter in cleanup methods")
    void shouldValidateDaysParameterInCleanupMethods() {
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.deleteOldJobs(0));
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.deleteOldJobs(-1));
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.findJobsEligibleForCleanup(0));
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.findJobsEligibleForCleanup(-5));
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.countJobsEligibleForCleanup(0));
        assertThrows(IllegalArgumentException.class, () -> AnalysisJobEntity.countJobsEligibleForCleanup(-10));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find jobs eligible for cleanup")
    void shouldFindJobsEligibleForCleanup() {
        // Clean up existing data
        AnalysisJobEntity.deleteAll();
        entityManager.flush();

        // Create old completed job
        AnalysisJobEntity oldCompletedJob = new AnalysisJobEntity("mock");
        oldCompletedJob.status = COMPLETED;
        oldCompletedJob.startedAt = Instant.now().minusSeconds(35 * 24 * 60 * 60L);
        oldCompletedJob.completedAt = Instant.now().minusSeconds(34 * 24 * 60 * 60L);
        oldCompletedJob.persist();

        // Create old failed job
        AnalysisJobEntity oldFailedJob = new AnalysisJobEntity("llm");
        oldFailedJob.status = FAILED;
        oldFailedJob.startedAt = Instant.now().minusSeconds(40 * 24 * 60 * 60L);
        oldFailedJob.completedAt = Instant.now().minusSeconds(39 * 24 * 60 * 60L);
        oldFailedJob.persist();

        // Create recent job (should not be eligible)
        AnalysisJobEntity recentJob = new AnalysisJobEntity("mock");
        recentJob.status = COMPLETED;
        recentJob.startedAt = Instant.now().minusSeconds(20 * 24 * 60 * 60L);
        recentJob.completedAt = Instant.now().minusSeconds(19 * 24 * 60 * 60L);
        recentJob.persist();

        // Create running job (should not be eligible)
        AnalysisJobEntity runningJob = new AnalysisJobEntity("llm");
        runningJob.status = RUNNING;
        runningJob.startedAt = Instant.now().minusSeconds(40 * 24 * 60 * 60L);
        runningJob.persist();

        entityManager.flush();

        // Find jobs eligible for cleanup (older than 30 days)
        List<AnalysisJobEntity> eligibleJobs = AnalysisJobEntity.findJobsEligibleForCleanup(30);
        
        assertEquals(2, eligibleJobs.size());
        assertTrue(eligibleJobs.stream().anyMatch(job -> job.id.equals(oldCompletedJob.id)));
        assertTrue(eligibleJobs.stream().anyMatch(job -> job.id.equals(oldFailedJob.id)));

        // Verify they are ordered by completedAt ASC (oldest first)
        assertTrue(eligibleJobs.get(0).completedAt.isBefore(eligibleJobs.get(1).completedAt) ||
                  eligibleJobs.get(0).completedAt.equals(eligibleJobs.get(1).completedAt));
    }

    @Test
    @TestTransaction
    @DisplayName("Should count jobs eligible for cleanup")
    void shouldCountJobsEligibleForCleanup() {
        // Clean up existing data
        AnalysisJobEntity.deleteAll();
        entityManager.flush();

        // Create old terminal jobs
        AnalysisJobEntity oldCompleted = new AnalysisJobEntity("mock");
        oldCompleted.status = COMPLETED;
        oldCompleted.completedAt = Instant.now().minusSeconds(35 * 24 * 60 * 60L);
        oldCompleted.persist();

        AnalysisJobEntity oldFailed = new AnalysisJobEntity("llm");
        oldFailed.status = FAILED;
        oldFailed.completedAt = Instant.now().minusSeconds(40 * 24 * 60 * 60L);
        oldFailed.persist();

        AnalysisJobEntity oldCancelled = new AnalysisJobEntity("mock");
        oldCancelled.status = CANCELLED;
        oldCancelled.completedAt = Instant.now().minusSeconds(32 * 24 * 60 * 60L);
        oldCancelled.persist();

        // Create recent job (should not be counted)
        AnalysisJobEntity recentJob = new AnalysisJobEntity("mock");
        recentJob.status = COMPLETED;
        recentJob.completedAt = Instant.now().minusSeconds(20 * 24 * 60 * 60L);
        recentJob.persist();

        entityManager.flush();

        // Count jobs eligible for cleanup (older than 30 days)
        long eligibleCount = AnalysisJobEntity.countJobsEligibleForCleanup(30);
        assertEquals(3, eligibleCount);

        // Count with different threshold (15 days - should include the recent job too)
        long eligibleCount15Days = AnalysisJobEntity.countJobsEligibleForCleanup(15);
        assertEquals(4, eligibleCount15Days); // All jobs should be eligible
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle cleanup when no jobs are eligible")
    void shouldHandleCleanupWhenNoJobsAreEligible() {
        // Clean up existing data
        AnalysisJobEntity.deleteAll();
        entityManager.flush();

        // Create only recent jobs
        AnalysisJobEntity recentJob = new AnalysisJobEntity("mock");
        recentJob.status = COMPLETED;
        recentJob.completedAt = Instant.now().minusSeconds(10 * 24 * 60 * 60L); // 10 days ago
        recentJob.persist();

        entityManager.flush();

        // Try to delete jobs older than 30 days
        long deletedCount = AnalysisJobEntity.deleteOldJobs(30);
        assertEquals(0, deletedCount);

        // Verify job still exists
        assertEquals(1, AnalysisJobEntity.count());

        // Test other cleanup methods
        List<AnalysisJobEntity> eligible = AnalysisJobEntity.findJobsEligibleForCleanup(30);
        assertTrue(eligible.isEmpty());

        long eligibleCount = AnalysisJobEntity.countJobsEligibleForCleanup(30);
        assertEquals(0, eligibleCount);
    }
}