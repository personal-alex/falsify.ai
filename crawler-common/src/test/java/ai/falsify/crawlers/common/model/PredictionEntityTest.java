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

@QuarkusTest
class PredictionEntityTest {

    @Inject
    EntityManager entityManager;

    private PredictionEntity testPrediction;
    private ArticleEntity testArticle;
    private AuthorEntity testAuthor;

    @BeforeEach
    void setUp() {
        // Initialize test data - will be created in each test method's transaction
        testAuthor = null;
        testArticle = null;
        testPrediction = null;
    }
    
    private void createTestData() {
        // Clean up any existing test data in proper order to avoid foreign key constraint violations
        // Delete dependent records first
        entityManager.createNativeQuery("DELETE FROM analysis_job_articles").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM article_predictions").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM prediction_instances").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM analysis_jobs").executeUpdate();
        
        // Now delete the main entities
        PredictionEntity.deleteAll();
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
        entityManager.flush();

        // Create test author
        testAuthor = new AuthorEntity("Test Author", "http://example.com/avatar.jpg");
        testAuthor.persist();

        // Create test article
        Article article = new Article("Test Article", "http://example.com/test", "Test content");
        testArticle = new ArticleEntity(article, "test-crawler", testAuthor);
        testArticle.persist();

        // Create test prediction
        testPrediction = new PredictionEntity("The stock market will rise by 10%", "economic");
        testPrediction.persist();
        
        // Ensure all entities are flushed to database
        entityManager.flush();
    }

    @Test
    @TestTransaction
    @DisplayName("Should create prediction with valid data")
    void shouldCreatePredictionWithValidData() {
        PredictionEntity prediction = new PredictionEntity("Bitcoin will reach $100,000", "cryptocurrency");
        prediction.persist();

        assertNotNull(prediction.id);
        assertEquals("Bitcoin will reach $100,000", prediction.predictionText);
        assertEquals("cryptocurrency", prediction.predictionType);
        assertNotNull(prediction.createdAt);
        assertTrue(prediction.articles.isEmpty());
        assertTrue(prediction.instances.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find prediction by text")
    void shouldFindPredictionByText() {
        createTestData();
        
        PredictionEntity found = PredictionEntity.findByText("The stock market will rise by 10%");
        
        assertNotNull(found);
        assertEquals(testPrediction.id, found.id);
        assertEquals("The stock market will rise by 10%", found.predictionText);
        assertEquals("economic", found.predictionType);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return null when prediction text not found")
    void shouldReturnNullWhenPredictionTextNotFound() {
        PredictionEntity found = PredictionEntity.findByText("Non-existent prediction");
        assertNull(found);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null and empty text in findByText")
    void shouldHandleNullAndEmptyTextInFindByText() {
        assertNull(PredictionEntity.findByText(null));
        assertNull(PredictionEntity.findByText(""));
        assertNull(PredictionEntity.findByText("   "));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find or create prediction")
    void shouldFindOrCreatePrediction() {
        createTestData();
        
        // Test finding existing prediction
        PredictionEntity found = PredictionEntity.findOrCreate("The stock market will rise by 10%", "economic");
        assertEquals(testPrediction.id, found.id);

        // Test creating new prediction
        PredictionEntity created = PredictionEntity.findOrCreate("New prediction text", "political");
        entityManager.flush(); // Ensure the new entity is persisted
        
        assertNotNull(created.id);
        assertNotEquals(testPrediction.id, created.id);
        assertEquals("New prediction text", created.predictionText);
        assertEquals("political", created.predictionType);
    }

    @Test
    @TestTransaction
    @DisplayName("Should throw exception when findOrCreate called with null text")
    void shouldThrowExceptionWhenFindOrCreateCalledWithNullText() {
        assertThrows(IllegalArgumentException.class, () -> {
            PredictionEntity.findOrCreate(null, "economic");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PredictionEntity.findOrCreate("", "economic");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PredictionEntity.findOrCreate("   ", "economic");
        });
    }

    @Test
    @TestTransaction
    @DisplayName("Should find predictions by type")
    void shouldFindPredictionsByType() {
        createTestData();
        
        // Create additional predictions with different types
        PredictionEntity political = new PredictionEntity("Election prediction", "political");
        political.persist();
        
        PredictionEntity sports = new PredictionEntity("Team will win championship", "sports");
        sports.persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<PredictionEntity> economicPredictions = PredictionEntity.findByType("economic");
        assertEquals(1, economicPredictions.size());
        assertEquals(testPrediction.id, economicPredictions.get(0).id);

        List<PredictionEntity> politicalPredictions = PredictionEntity.findByType("political");
        assertEquals(1, politicalPredictions.size());
        assertEquals(political.id, politicalPredictions.get(0).id);

        List<PredictionEntity> nonExistent = PredictionEntity.findByType("non-existent");
        assertTrue(nonExistent.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should find predictions containing text")
    void shouldFindPredictionsContainingText() {
        createTestData();
        
        PredictionEntity additional = new PredictionEntity("The market will crash soon", "economic");
        additional.persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<PredictionEntity> marketPredictions = PredictionEntity.findContainingText("market");
        assertEquals(2, marketPredictions.size());

        List<PredictionEntity> stockPredictions = PredictionEntity.findContainingText("stock");
        assertEquals(1, stockPredictions.size());
        assertEquals(testPrediction.id, stockPredictions.get(0).id);

        List<PredictionEntity> nonExistent = PredictionEntity.findContainingText("bitcoin");
        assertTrue(nonExistent.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should get distinct prediction types")
    void shouldGetDistinctPredictionTypes() {
        createTestData();
        
        // Create predictions with various types
        new PredictionEntity("Political prediction", "political").persist();
        new PredictionEntity("Sports prediction", "sports").persist();
        new PredictionEntity("Another economic prediction", "economic").persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        List<String> types = PredictionEntity.findDistinctTypes();
        assertTrue(types.contains("economic"));
        assertTrue(types.contains("political"));
        assertTrue(types.contains("sports"));
        assertEquals(3, types.size());
    }

    @Test
    @TestTransaction
    @DisplayName("Should count predictions by type")
    void shouldCountPredictionsByType() {
        createTestData();
        
        new PredictionEntity("Another economic prediction", "economic").persist();
        new PredictionEntity("Political prediction", "political").persist();
        
        // Flush to ensure entities are persisted before queries
        entityManager.flush();

        assertEquals(2, PredictionEntity.countByType("economic"));
        assertEquals(1, PredictionEntity.countByType("political"));
        assertEquals(0, PredictionEntity.countByType("sports"));
        assertEquals(0, PredictionEntity.countByType("non-existent"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should manage article relationships")
    void shouldManageArticleRelationships() {
        createTestData();
        
        // Test adding article
        testPrediction.addArticle(testArticle);
        entityManager.flush();

        assertTrue(testPrediction.articles.contains(testArticle));
        assertTrue(testArticle.predictions.contains(testPrediction));

        // Test removing article
        testPrediction.removeArticle(testArticle);
        entityManager.flush();

        assertFalse(testPrediction.articles.contains(testArticle));
        assertFalse(testArticle.predictions.contains(testPrediction));
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null article in relationship methods")
    void shouldHandleNullArticleInRelationshipMethods() {
        createTestData();
        
        int initialSize = testPrediction.articles.size();
        
        testPrediction.addArticle(null);
        assertEquals(initialSize, testPrediction.articles.size());

        testPrediction.removeArticle(null);
        assertEquals(initialSize, testPrediction.articles.size());
    }

    @Test
    @TestTransaction
    @DisplayName("Should prevent duplicate article relationships")
    void shouldPreventDuplicateArticleRelationships() {
        createTestData();
        
        testPrediction.addArticle(testArticle);
        testPrediction.addArticle(testArticle); // Add same article again
        entityManager.flush();

        assertEquals(1, testPrediction.articles.size());
        assertEquals(1, testArticle.predictions.size());
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize prediction text")
    void shouldSanitizePredictionText() {
        PredictionEntity prediction = new PredictionEntity("<script>alert('xss')</script>Market will rise", "economic");
        prediction.persist();

        assertFalse(prediction.predictionText.contains("<script>"));
        assertTrue(prediction.predictionText.contains("&lt;script&gt;"));
        assertTrue(prediction.predictionText.contains("Market will rise"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize prediction type")
    void shouldSanitizePredictionType() {
        PredictionEntity prediction = new PredictionEntity("Test prediction", "ECONOMIC");
        prediction.persist();

        assertEquals("economic", prediction.predictionType);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle long prediction type")
    void shouldHandleLongPredictionType() {
        String longType = "a".repeat(60); // Longer than 50 characters
        PredictionEntity prediction = new PredictionEntity("Test prediction", longType);
        prediction.persist();

        assertEquals(50, prediction.predictionType.length());
        assertEquals("a".repeat(50), prediction.predictionType);
    }

    @Test
    @TestTransaction
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        createTestData();
        
        String toString = testPrediction.toString();
        
        assertTrue(toString.contains("PredictionEntity"));
        assertTrue(toString.contains("id=" + testPrediction.id));
        assertTrue(toString.contains("predictionType='economic'"));
        assertTrue(toString.contains("articlesCount=0"));
        assertTrue(toString.contains("instancesCount=0"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should truncate long prediction text in toString")
    void shouldTruncateLongPredictionTextInToString() {
        String longText = "a".repeat(100);
        PredictionEntity prediction = new PredictionEntity(longText, "test");
        prediction.persist();

        String toString = prediction.toString();
        assertTrue(toString.contains("..."));
        assertFalse(toString.contains("a".repeat(100)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should set created timestamp automatically")
    void shouldSetCreatedTimestampAutomatically() {
        Instant before = Instant.now().minusSeconds(1);
        PredictionEntity prediction = new PredictionEntity("Test prediction", "test");
        Instant after = Instant.now().plusSeconds(1);
        
        prediction.persist();

        assertNotNull(prediction.createdAt);
        assertTrue(prediction.createdAt.isAfter(before));
        assertTrue(prediction.createdAt.isBefore(after));
    }
}