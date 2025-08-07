package ai.falsify.crawlers.common.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PredictionInstanceEntityTest {

    @Inject
    EntityManager entityManager;

    private PredictionEntity testPrediction;
    private ArticleEntity testArticle;
    private AuthorEntity testAuthor;
    private AnalysisJobEntity testAnalysisJob;
    private PredictionInstanceEntity testInstance;

    @BeforeEach
    void setUp() {
        // Initialize test data - will be created in each test method's transaction
        testAuthor = null;
        testArticle = null;
        testPrediction = null;
        testAnalysisJob = null;
        testInstance = null;
    }
    
    private void createTestData() {
        // Clean up any existing test data in proper order to avoid foreign key constraint violations
        // Delete dependent records first
        try {
            entityManager.createNativeQuery("DELETE FROM analysis_job_articles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM article_predictions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM prediction_instances").executeUpdate();
        } catch (Exception e) {
            // Tables might not exist yet in some test scenarios, ignore
        }
        
        // Now delete the main entities
        AnalysisJobEntity.deleteAll();
        PredictionEntity.deleteAll();
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
        entityManager.flush();

        // Create test author
        testAuthor = new AuthorEntity("Test Author", "http://example.com/avatar.jpg");
        testAuthor.persist();

        // Create test article
        Article article = new Article("Test Article", "http://example.com/test", "Test content with prediction");
        testArticle = new ArticleEntity(article, "test-crawler", testAuthor);
        testArticle.persist();

        // Create test prediction
        testPrediction = new PredictionEntity("The stock market will rise by 10%", "economic");
        testPrediction.persist();

        // Create test analysis job
        testAnalysisJob = new AnalysisJobEntity("mock");
        testAnalysisJob.totalArticles = 1;
        testAnalysisJob.persist();
        
        // Ensure all base entities are flushed before creating instances
        entityManager.flush();

        // Create test prediction instance
        testInstance = new PredictionInstanceEntity(
            testPrediction, 
            testArticle, 
            testAnalysisJob, 
            4, 
            new BigDecimal("0.85"), 
            "The market shows strong indicators..."
        );
        testInstance.persist();
        
        // Ensure the instance is also flushed
        entityManager.flush();
    }

    @Test
    @TestTransaction
    @DisplayName("Should create prediction instance with valid data")
    void shouldCreatePredictionInstanceWithValidData() {
        createTestData();
        
        PredictionInstanceEntity instance = new PredictionInstanceEntity(
            testPrediction,
            testArticle,
            testAnalysisJob,
            5,
            new BigDecimal("0.95"),
            "Strong prediction context"
        );
        instance.persist();

        assertNotNull(instance.id);
        assertEquals(testPrediction.id, instance.prediction.id);
        assertEquals(testArticle.id, instance.article.id);
        assertEquals(testAnalysisJob.id, instance.analysisJob.id);
        assertEquals(5, instance.rating);
        assertEquals(new BigDecimal("0.95"), instance.confidenceScore);
        assertEquals("Strong prediction context", instance.context);
        assertNotNull(instance.extractedAt);
    }

    @Test
    @TestTransaction
    @DisplayName("Should validate rating range")
    void shouldValidateRatingRange() {
        createTestData();
        
        // Test valid ratings
        assertDoesNotThrow(() -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 1, BigDecimal.ZERO, null);
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, BigDecimal.ZERO, null);
        });

        // Test invalid ratings
        assertThrows(IllegalArgumentException.class, () -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 0, BigDecimal.ZERO, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 6, BigDecimal.ZERO, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, null, BigDecimal.ZERO, null);
        });
    }

    @Test
    @TestTransaction
    @DisplayName("Should validate confidence score range")
    void shouldValidateConfidenceScoreRange() {
        createTestData();
        
        // Test valid confidence scores
        assertDoesNotThrow(() -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.00"), null);
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("1.00"), null);
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.50"), null);
        });

        // Test invalid confidence scores
        assertThrows(IllegalArgumentException.class, () -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("-0.01"), null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("1.01"), null);
        });

        // Test null confidence score (should default to 0.00)
        PredictionInstanceEntity instance = new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, null, null);
        assertEquals(BigDecimal.ZERO, instance.confidenceScore);
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by prediction")
    void shouldFindInstancesByPrediction() {
        createTestData();
        
        List<PredictionInstanceEntity> instances = PredictionInstanceEntity.findByPrediction(testPrediction);
        
        assertEquals(1, instances.size());
        assertEquals(testInstance.id, instances.get(0).id);

        // Test with null prediction
        List<PredictionInstanceEntity> nullResult = PredictionInstanceEntity.findByPrediction(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by article")
    void shouldFindInstancesByArticle() {
        createTestData();
        
        List<PredictionInstanceEntity> instances = PredictionInstanceEntity.findByArticle(testArticle);
        
        assertEquals(1, instances.size());
        assertEquals(testInstance.id, instances.get(0).id);

        // Test with null article
        List<PredictionInstanceEntity> nullResult = PredictionInstanceEntity.findByArticle(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by analysis job")
    void shouldFindInstancesByAnalysisJob() {
        createTestData();
        
        List<PredictionInstanceEntity> instances = PredictionInstanceEntity.findByAnalysisJob(testAnalysisJob);
        
        assertEquals(1, instances.size());
        assertEquals(testInstance.id, instances.get(0).id);

        // Test with null analysis job
        List<PredictionInstanceEntity> nullResult = PredictionInstanceEntity.findByAnalysisJob(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by rating")
    void shouldFindInstancesByRating() {
        createTestData();
        
        // Create additional instances with different ratings
        PredictionInstanceEntity instance5 = new PredictionInstanceEntity(
            testPrediction, testArticle, testAnalysisJob, 5, new BigDecimal("0.90"), null);
        instance5.persist();

        PredictionInstanceEntity instance3 = new PredictionInstanceEntity(
            testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.60"), null);
        instance3.persist();
        
        entityManager.flush();

        List<PredictionInstanceEntity> rating4 = PredictionInstanceEntity.findByRating(4);
        assertEquals(1, rating4.size());
        assertEquals(testInstance.id, rating4.get(0).id);

        List<PredictionInstanceEntity> rating5 = PredictionInstanceEntity.findByRating(5);
        assertEquals(1, rating5.size());
        assertEquals(instance5.id, rating5.get(0).id);

        List<PredictionInstanceEntity> rating1 = PredictionInstanceEntity.findByRating(1);
        assertTrue(rating1.isEmpty());

        // Test invalid ratings
        assertTrue(PredictionInstanceEntity.findByRating(0).isEmpty());
        assertTrue(PredictionInstanceEntity.findByRating(6).isEmpty());
        assertTrue(PredictionInstanceEntity.findByRating(null).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by minimum rating")
    void shouldFindInstancesByMinimumRating() {
        createTestData();
        
        // Create additional analysis jobs for these instances
        AnalysisJobEntity job2 = new AnalysisJobEntity("mock");
        job2.persist();
        AnalysisJobEntity job3 = new AnalysisJobEntity("mock");
        job3.persist();
        AnalysisJobEntity job4 = new AnalysisJobEntity("mock");
        job4.persist();
        entityManager.flush();
        
        // Create additional instances with different ratings
        new PredictionInstanceEntity(testPrediction, testArticle, job2, 5, new BigDecimal("0.90"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, job3, 3, new BigDecimal("0.60"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, job4, 2, new BigDecimal("0.40"), null).persist();
        entityManager.flush();

        List<PredictionInstanceEntity> minRating4 = PredictionInstanceEntity.findByMinRating(4);
        assertEquals(2, minRating4.size()); // Rating 4 and 5

        List<PredictionInstanceEntity> minRating3 = PredictionInstanceEntity.findByMinRating(3);
        assertEquals(3, minRating3.size()); // Rating 3, 4, and 5

        List<PredictionInstanceEntity> minRating5 = PredictionInstanceEntity.findByMinRating(5);
        assertEquals(1, minRating5.size()); // Only rating 5
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by minimum confidence")
    void shouldFindInstancesByMinimumConfidence() {
        createTestData();
        
        // Create additional instances with different confidence scores
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.95"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.70"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.50"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<PredictionInstanceEntity> minConf80 = PredictionInstanceEntity.findByMinConfidence(new BigDecimal("0.80"));
        assertEquals(2, minConf80.size()); // 0.85 and 0.95

        List<PredictionInstanceEntity> minConf90 = PredictionInstanceEntity.findByMinConfidence(new BigDecimal("0.90"));
        assertEquals(1, minConf90.size()); // Only 0.95

        List<PredictionInstanceEntity> minConf50 = PredictionInstanceEntity.findByMinConfidence(new BigDecimal("0.50"));
        assertEquals(4, minConf50.size()); // All instances
    }

    @Test
    @TestTransaction
    @DisplayName("Should find recent instances")
    void shouldFindRecentInstances() {
        createTestData();
        
        // Create additional instances
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.70"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, new BigDecimal("0.90"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<PredictionInstanceEntity> recent = PredictionInstanceEntity.findRecent(2);
        assertEquals(2, recent.size());
        
        // Should be ordered by extractedAt DESC
        assertTrue(recent.get(0).extractedAt.isAfter(recent.get(1).extractedAt) || 
                  recent.get(0).extractedAt.equals(recent.get(1).extractedAt));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find instances by article and job")
    void shouldFindInstancesByArticleAndJob() {
        createTestData();
        
        List<PredictionInstanceEntity> instances = PredictionInstanceEntity.findByArticleAndJob(testArticle, testAnalysisJob);
        
        assertEquals(1, instances.size());
        assertEquals(testInstance.id, instances.get(0).id);

        // Test with null parameters
        assertTrue(PredictionInstanceEntity.findByArticleAndJob(null, testAnalysisJob).isEmpty());
        assertTrue(PredictionInstanceEntity.findByArticleAndJob(testArticle, null).isEmpty());
        assertTrue(PredictionInstanceEntity.findByArticleAndJob(null, null).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should calculate average rating for prediction")
    void shouldCalculateAverageRatingForPrediction() {
        createTestData();
        
        // Create additional instances with different ratings
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 2, new BigDecimal("0.40"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, new BigDecimal("0.90"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        Double avgRating = PredictionInstanceEntity.getAverageRatingForPrediction(testPrediction);
        assertNotNull(avgRating);
        assertEquals(3.67, avgRating, 0.01); // (4 + 2 + 5) / 3 = 3.67

        // Test with prediction that has no instances
        PredictionEntity emptyPrediction = new PredictionEntity("Empty prediction", "test");
        emptyPrediction.persist();
        entityManager.flush();
        assertNull(PredictionInstanceEntity.getAverageRatingForPrediction(emptyPrediction));

        // Test with null prediction
        assertNull(PredictionInstanceEntity.getAverageRatingForPrediction(null));
    }

    @Test
    @TestTransaction
    @DisplayName("Should calculate average confidence for prediction")
    void shouldCalculateAverageConfidenceForPrediction() {
        createTestData();
        
        // Create additional instances with different confidence scores
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.60"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.90"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        BigDecimal avgConfidence = PredictionInstanceEntity.getAverageConfidenceForPrediction(testPrediction);
        assertNotNull(avgConfidence);
        assertEquals(new BigDecimal("0.78"), avgConfidence.setScale(2, java.math.RoundingMode.HALF_UP)); // (0.85 + 0.60 + 0.90) / 3

        // Test with prediction that has no instances
        PredictionEntity emptyPrediction = new PredictionEntity("Empty prediction", "test");
        emptyPrediction.persist();
        entityManager.flush();
        assertNull(PredictionInstanceEntity.getAverageConfidenceForPrediction(emptyPrediction));

        // Test with null prediction
        assertNull(PredictionInstanceEntity.getAverageConfidenceForPrediction(null));
    }

    @Test
    @TestTransaction
    @DisplayName("Should count instances by analysis job")
    void shouldCountInstancesByAnalysisJob() {
        createTestData();
        
        // Create additional instances
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.70"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();
        
        long count = PredictionInstanceEntity.countByAnalysisJob(testAnalysisJob);
        assertEquals(2, count);

        // Test with job that has no instances
        AnalysisJobEntity emptyJob = new AnalysisJobEntity("mock");
        emptyJob.persist();
        entityManager.flush();
        assertEquals(0, PredictionInstanceEntity.countByAnalysisJob(emptyJob));

        // Test with null job
        assertEquals(0, PredictionInstanceEntity.countByAnalysisJob(null));
    }

    @Test
    @TestTransaction
    @DisplayName("Should count instances by rating")
    void shouldCountInstancesByRating() {
        createTestData();
        
        // Create additional instances with different ratings
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 4, new BigDecimal("0.80"), null).persist();
        new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, new BigDecimal("0.90"), null).persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        assertEquals(2, PredictionInstanceEntity.countByRating(4)); // Two instances with rating 4
        assertEquals(1, PredictionInstanceEntity.countByRating(5)); // One instance with rating 5
        assertEquals(0, PredictionInstanceEntity.countByRating(1)); // No instances with rating 1
        assertEquals(0, PredictionInstanceEntity.countByRating(null)); // Null rating
    }

    @Test
    @TestTransaction
    @DisplayName("Should get star rating string")
    void shouldGetStarRatingString() {
        createTestData();
        
        assertEquals("★★★★☆", testInstance.getStarRating());

        PredictionInstanceEntity instance5 = new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, BigDecimal.ZERO, null);
        assertEquals("★★★★★", instance5.getStarRating());

        PredictionInstanceEntity instance1 = new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 1, BigDecimal.ZERO, null);
        assertEquals("★☆☆☆☆", instance1.getStarRating());
    }

    @Test
    @TestTransaction
    @DisplayName("Should get confidence percentage")
    void shouldGetConfidencePercentage() {
        createTestData();
        
        assertEquals(85.0, testInstance.getConfidencePercentage(), 0.01);

        PredictionInstanceEntity instance100 = new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 5, BigDecimal.ONE, null);
        assertEquals(100.0, instance100.getConfidencePercentage(), 0.01);

        PredictionInstanceEntity instance0 = new PredictionInstanceEntity(testPrediction, testArticle, testAnalysisJob, 1, BigDecimal.ZERO, null);
        assertEquals(0.0, instance0.getConfidencePercentage(), 0.01);
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize context text")
    void shouldSanitizeContextText() {
        createTestData();
        
        PredictionInstanceEntity instance = new PredictionInstanceEntity(
            testPrediction, 
            testArticle, 
            testAnalysisJob, 
            3, 
            new BigDecimal("0.70"), 
            "<script>alert('xss')</script>Context with HTML"
        );
        instance.persist();

        assertFalse(instance.context.contains("<script>"));
        assertTrue(instance.context.contains("&lt;script&gt;"));
        assertTrue(instance.context.contains("Context with HTML"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null context")
    void shouldHandleNullContext() {
        createTestData();
        
        PredictionInstanceEntity instance = new PredictionInstanceEntity(
            testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.70"), null);
        instance.persist();

        assertNull(instance.context);
    }

    @Test
    @TestTransaction
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        createTestData();
        
        String toString = testInstance.toString();
        
        assertTrue(toString.contains("PredictionInstanceEntity"));
        assertTrue(toString.contains("id=" + testInstance.id));
        assertTrue(toString.contains("prediction=" + testPrediction.id));
        assertTrue(toString.contains("article=" + testArticle.id));
        assertTrue(toString.contains("analysisJob=" + testAnalysisJob.jobId));
        assertTrue(toString.contains("rating=4"));
        assertTrue(toString.contains("confidenceScore=0.85"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should set extracted timestamp automatically")
    void shouldSetExtractedTimestampAutomatically() {
        createTestData();
        
        Instant before = Instant.now().minusSeconds(1);
        PredictionInstanceEntity instance = new PredictionInstanceEntity(
            testPrediction, testArticle, testAnalysisJob, 3, new BigDecimal("0.70"), null);
        Instant after = Instant.now().plusSeconds(1);
        
        instance.persist();

        assertNotNull(instance.extractedAt);
        assertTrue(instance.extractedAt.isAfter(before));
        assertTrue(instance.extractedAt.isBefore(after));
    }
}