package ai.falsify.crawlers;

import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.AuthorEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transaction management and error handling in CaspitCrawler.
 * Verifies that transaction boundaries are properly isolated and failures don't cascade.
 */
@QuarkusTest
@TestProfile(CaspitCrawlerTransactionTest.TransactionTestProfile.class)
class CaspitCrawlerTransactionTest {

    @Inject
    CaspitCrawler crawler;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clear database before each test
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up after each test
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
    }

    @Test
    @DisplayName("Should handle author creation within transaction boundaries")
    void testAuthorCreationTransactionBoundaries() {
        // Test that author creation works within transaction context
        Article testArticle = new Article(
            "Test Article Title",
            "https://test.example.com/article-1",
            "This is test article content with sufficient length for validation."
        );

        // This should work without transaction issues
        assertDoesNotThrow(() -> {
            crawler.persistArticleToDatabase(testArticle);
        });

        // Verify article was persisted
        ArticleEntity persistedArticle = ArticleEntity.findByUrl(testArticle.url());
        assertNotNull(persistedArticle, "Article should be persisted");
        assertEquals(testArticle.title(), persistedArticle.title);
        assertEquals(testArticle.url(), persistedArticle.url);
        assertEquals(testArticle.text(), persistedArticle.text);

        // Verify author was created
        assertNotNull(persistedArticle.author, "Article should have an author");
        assertNotNull(persistedArticle.author.name, "Author should have a name");
    }

    @Test
    @DisplayName("Should handle duplicate article persistence gracefully")
    void testDuplicateArticlePersistence() {
        Article testArticle = new Article(
            "Duplicate Test Article",
            "https://test.example.com/duplicate-article",
            "This is a duplicate test article content."
        );

        // Persist the article first time
        assertDoesNotThrow(() -> {
            crawler.persistArticleToDatabase(testArticle);
        });

        // Verify first persistence worked
        ArticleEntity firstArticle = ArticleEntity.findByUrl(testArticle.url());
        assertNotNull(firstArticle, "First article should be persisted");

        // Try to persist the same article again - should not throw exception
        assertDoesNotThrow(() -> {
            crawler.persistArticleToDatabase(testArticle);
        });

        // Verify only one article exists
        long articleCount = ArticleEntity.count("url", testArticle.url());
        assertEquals(1, articleCount, "Should have only one article with the same URL");
    }

    @Test
    @DisplayName("Should handle author fallback when author creation fails")
    void testAuthorFallbackHandling() {
        Article testArticle = new Article(
            "Test Article with Author Fallback",
            "https://test.example.com/author-fallback-test",
            "This is test article content for author fallback testing."
        );

        // This should work even if author configuration is problematic
        assertDoesNotThrow(() -> {
            crawler.persistArticleToDatabase(testArticle);
        });

        // Verify article was persisted with some author
        ArticleEntity persistedArticle = ArticleEntity.findByUrl(testArticle.url());
        assertNotNull(persistedArticle, "Article should be persisted");
        assertNotNull(persistedArticle.author, "Article should have an author (fallback if needed)");
    }

    @Test
    @DisplayName("Should validate article data before persistence")
    void testArticleDataValidation() {
        // Test with null URL
        Article invalidArticle1 = new Article("Title", null, "Content");
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle1);
        });

        // Test with empty URL
        Article invalidArticle2 = new Article("Title", "", "Content");
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle2);
        });

        // Test with null title
        Article invalidArticle3 = new Article(null, "https://test.com", "Content");
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle3);
        });

        // Test with empty title
        Article invalidArticle4 = new Article("", "https://test.com", "Content");
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle4);
        });

        // Test with null content
        Article invalidArticle5 = new Article("Title", "https://test.com", null);
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle5);
        });

        // Test with empty content
        Article invalidArticle6 = new Article("Title", "https://test.com", "");
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle6);
        });
    }

    @Test
    @DisplayName("Should handle transaction isolation properly")
    void testTransactionIsolation() {
        Article validArticle = new Article(
            "Valid Article",
            "https://test.example.com/valid-article",
            "This is valid article content."
        );

        Article invalidArticle = new Article(
            "Invalid Article",
            null, // Invalid URL
            "This is invalid article content."
        );

        // Persist valid article - should succeed
        assertDoesNotThrow(() -> {
            crawler.persistArticleToDatabase(validArticle);
        });

        // Try to persist invalid article - should fail but not affect valid article
        assertThrows(RuntimeException.class, () -> {
            crawler.persistArticleToDatabase(invalidArticle);
        });

        // Verify valid article is still persisted
        ArticleEntity persistedArticle = ArticleEntity.findByUrl(validArticle.url());
        assertNotNull(persistedArticle, "Valid article should remain persisted despite invalid article failure");
        assertEquals(validArticle.title(), persistedArticle.title);
    }

    @Test
    @DisplayName("Should handle circuit breaker operations")
    void testCircuitBreakerOperations() {
        // Test circuit breaker status retrieval
        assertDoesNotThrow(() -> {
            var status = crawler.getCircuitBreakerStatus();
            assertNotNull(status, "Circuit breaker status should not be null");
        });

        // Test circuit breaker reset
        assertDoesNotThrow(() -> {
            crawler.resetCircuitBreaker();
        });
    }

    /**
     * Test profile for transaction tests with in-memory database
     */
    public static class TransactionTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            java.util.Map<String, String> config = new java.util.HashMap<>();
            
            // Use in-memory H2 database for testing
            config.put("quarkus.datasource.devservices.enabled", "true");
            config.put("quarkus.datasource.db-kind", "h2");
            config.put("quarkus.datasource.jdbc.url", "jdbc:h2:mem:transaction-test;DB_CLOSE_DELAY=-1");
            config.put("quarkus.hibernate-orm.enabled", "true");
            config.put("quarkus.hibernate-orm.database.generation", "drop-and-create");
            
            // Disable Redis for transaction tests
            config.put("quarkus.redis.devservices.enabled", "false");
            
            // Configure crawler for testing
            config.put("caspit.crawler.base-url", "https://test.example.com");
            config.put("caspit.crawler.author.name", "Test Author");
            config.put("caspit.crawler.author.fallback-name", "Unknown Author");
            config.put("caspit.crawler.crawling.min-content-length", "10");
            
            // Disable scheduled tasks that might interfere
            config.put("quarkus.scheduler.enabled", "false");
            
            return config;
        }
    }
}