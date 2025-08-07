package ai.falsify.crawlers.common.util;

import ai.falsify.crawlers.common.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Utility class for cleaning up test data in the correct order to avoid foreign key constraint violations.
 */
@ApplicationScoped
public class TestDataCleanup {
    
    /**
     * Cleans up all test data in the correct order to avoid foreign key constraint violations.
     * This method should be called in @BeforeEach methods of test classes.
     * Uses separate transactions to avoid deadlock issues.
     */
    public void cleanupAllData() {
        
        // Use multiple separate transactions to avoid deadlocks
        cleanupWithRetry();
    }
    
    /**
     * Performs cleanup with retry logic to handle deadlocks and transaction conflicts.
     */
    private void cleanupWithRetry() {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                performCleanup();
                return; // Success, exit retry loop
            } catch (Exception e) {
                System.err.println("Cleanup attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    System.err.println("Warning: Could not clean up test data after " + maxRetries + " attempts");
                    return;
                }
                
                // Wait a bit before retrying to let any conflicting transactions complete
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
    
    /**
     * Performs the actual cleanup in separate transactions to avoid deadlocks.
     */
    @Transactional
    public void performCleanup() {
        try {
            // Use TRUNCATE CASCADE for the most reliable cleanup
            var em = AnalysisJobEntity.getEntityManager();
            em.createNativeQuery("TRUNCATE TABLE analysis_job_articles, prediction_instances, predictions, analysis_jobs, articles, authors RESTART IDENTITY CASCADE").executeUpdate();
        } catch (Exception e) {
            // Fallback to individual deletes in proper order
            cleanupWithIndividualDeletes();
        }
    }
    
    /**
     * Fallback cleanup method using individual DELETE statements.
     */
    @Transactional
    public void cleanupWithIndividualDeletes() {
        try {
            var em = AnalysisJobEntity.getEntityManager();
            
            // Delete in proper order to avoid foreign key constraint violations
            em.createNativeQuery("DELETE FROM analysis_job_articles").executeUpdate();
            em.createNativeQuery("DELETE FROM prediction_instances").executeUpdate();
            em.createNativeQuery("DELETE FROM predictions").executeUpdate();
            em.createNativeQuery("DELETE FROM analysis_jobs").executeUpdate();
            em.createNativeQuery("DELETE FROM articles").executeUpdate();
            em.createNativeQuery("DELETE FROM authors").executeUpdate();
            
            // Reset sequences if they exist
            try {
                em.createNativeQuery("SELECT setval('articles_id_seq', 1, false)").executeUpdate();
                em.createNativeQuery("SELECT setval('authors_id_seq', 1, false)").executeUpdate();
                em.createNativeQuery("SELECT setval('predictions_id_seq', 1, false)").executeUpdate();
                em.createNativeQuery("SELECT setval('prediction_instances_id_seq', 1, false)").executeUpdate();
                em.createNativeQuery("SELECT setval('analysis_jobs_id_seq', 1, false)").executeUpdate();
            } catch (Exception seqException) {
                // Sequences might not exist in test database, ignore
            }
        } catch (Exception fallbackException) {
            // Final fallback using entity methods
            cleanupWithEntityMethods();
        }
    }
    
    /**
     * Final fallback cleanup method using Panache entity methods.
     */
    @Transactional
    public void cleanupWithEntityMethods() {
        try {
            // Delete in proper order to avoid foreign key constraint violations
            PredictionInstanceEntity.deleteAll();
            PredictionEntity.deleteAll();
            AnalysisJobEntity.deleteAll();
            ArticleEntity.deleteAll();
            AuthorEntity.deleteAll();
        } catch (Exception entityException) {
            System.err.println("Warning: Could not clean up test data with entity methods: " + entityException.getMessage());
        }
    }
}