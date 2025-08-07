package ai.falsify.crawlers.common.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ArticleEntityTest {

    @Inject
    EntityManager entityManager;

    private AuthorEntity testAuthor;

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean up any existing test data in proper order to avoid foreign key constraint violations
        cleanupTestData();
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
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
        entityManager.flush();
    }
    
    private AuthorEntity createTestAuthor() {
        AuthorEntity author = new AuthorEntity("Test Author", "https://example.com/avatar.jpg");
        author.persist();
        return author;
    }

    @Test
    @TestTransaction
    @DisplayName("Should create article with author")
    void shouldCreateArticleWithAuthor() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        String crawlerSource = "test-crawler";

        // When
        ArticleEntity articleEntity = new ArticleEntity(article, crawlerSource, testAuthor);
        articleEntity.persist();

        // Then
        assertNotNull(articleEntity.id);
        assertEquals("Test Title", articleEntity.title);
        assertEquals("https://example.com/article", articleEntity.url);
        assertEquals("Test content", articleEntity.text);
        assertEquals(crawlerSource, articleEntity.crawlerSource);
        assertEquals(testAuthor.id, articleEntity.author.id);
        assertNotNull(articleEntity.createdAt);
        assertTrue(articleEntity.createdAt.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should use unknown author when author is null")
    void shouldUseUnknownAuthorWhenAuthorIsNull() {
        // Given
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        String crawlerSource = "test-crawler";

        // When
        ArticleEntity articleEntity = new ArticleEntity(article, crawlerSource, null);
        articleEntity.persist();

        // Then
        assertNotNull(articleEntity.author);
        assertEquals(AuthorEntity.getUnknownAuthor().name, articleEntity.author.name);
    }

    @Test
    @TestTransaction
    @DisplayName("Should create article with deprecated constructor")
    void shouldCreateArticleWithDeprecatedConstructor() {
        // Given
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        String crawlerSource = "test-crawler";

        // When
        @SuppressWarnings("deprecation")
        ArticleEntity articleEntity = new ArticleEntity(article, crawlerSource);
        articleEntity.persist();

        // Then
        assertNotNull(articleEntity.author);
        assertEquals(AuthorEntity.getUnknownAuthor().name, articleEntity.author.name);
    }

    @Test
    @TestTransaction
    @DisplayName("Should find article by URL")
    void shouldFindArticleByUrl() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", testAuthor);
        articleEntity.persist();

        // When
        ArticleEntity found = ArticleEntity.findByUrl("https://example.com/article");

        // Then
        assertNotNull(found);
        assertEquals(articleEntity.id, found.id);
        assertEquals("Test Title", found.title);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return null when finding non-existent URL")
    void shouldReturnNullWhenFindingNonExistentUrl() {
        // When
        ArticleEntity found = ArticleEntity.findByUrl("https://example.com/nonexistent");

        // Then
        assertNull(found);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null or empty URL in findByUrl")
    void shouldHandleNullOrEmptyUrlInFindByUrl() {
        // When
        ArticleEntity foundNull = ArticleEntity.findByUrl(null);
        ArticleEntity foundEmpty = ArticleEntity.findByUrl("");
        ArticleEntity foundWhitespace = ArticleEntity.findByUrl("   ");

        // Then
        assertNull(foundNull);
        assertNull(foundEmpty);
        assertNull(foundWhitespace);
    }

    @Test
    @TestTransaction
    @DisplayName("Should check if article exists by URL")
    void shouldCheckIfArticleExistsByUrl() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", testAuthor);
        articleEntity.persist();

        // When
        boolean exists = ArticleEntity.existsByUrl("https://example.com/article");
        boolean notExists = ArticleEntity.existsByUrl("https://example.com/nonexistent");

        // Then
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle null or empty URL in existsByUrl")
    void shouldHandleNullOrEmptyUrlInExistsByUrl() {
        // When
        boolean existsNull = ArticleEntity.existsByUrl(null);
        boolean existsEmpty = ArticleEntity.existsByUrl("");
        boolean existsWhitespace = ArticleEntity.existsByUrl("   ");

        // Then
        assertFalse(existsNull);
        assertFalse(existsEmpty);
        assertFalse(existsWhitespace);
    }

    @Test
    @TestTransaction
    @DisplayName("Should find articles by crawler source")
    void shouldFindArticlesByCrawlerSource() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article1 = new Article("Title 1", "https://example.com/1", "Content 1");
        Article article2 = new Article("Title 2", "https://example.com/2", "Content 2");
        Article article3 = new Article("Title 3", "https://example.com/3", "Content 3");
        
        ArticleEntity entity1 = new ArticleEntity(article1, "crawler-a", testAuthor);
        ArticleEntity entity2 = new ArticleEntity(article2, "crawler-a", testAuthor);
        ArticleEntity entity3 = new ArticleEntity(article3, "crawler-b", testAuthor);
        
        entity1.persist();
        entity2.persist();
        entity3.persist();

        // When
        List<ArticleEntity> crawlerAArticles = ArticleEntity.findByCrawlerSource("crawler-a");
        List<ArticleEntity> crawlerBArticles = ArticleEntity.findByCrawlerSource("crawler-b");

        // Then
        assertEquals(2, crawlerAArticles.size());
        assertEquals(1, crawlerBArticles.size());
        assertTrue(crawlerAArticles.stream().allMatch(a -> "crawler-a".equals(a.crawlerSource)));
        assertTrue(crawlerBArticles.stream().allMatch(a -> "crawler-b".equals(a.crawlerSource)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find articles by author")
    void shouldFindArticlesByAuthor() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        AuthorEntity author2 = new AuthorEntity("Author 2");
        author2.persist();
        
        Article article1 = new Article("Title 1", "https://example.com/1", "Content 1");
        Article article2 = new Article("Title 2", "https://example.com/2", "Content 2");
        Article article3 = new Article("Title 3", "https://example.com/3", "Content 3");
        
        ArticleEntity entity1 = new ArticleEntity(article1, "test-crawler", testAuthor);
        ArticleEntity entity2 = new ArticleEntity(article2, "test-crawler", testAuthor);
        ArticleEntity entity3 = new ArticleEntity(article3, "test-crawler", author2);
        
        entity1.persist();
        entity2.persist();
        entity3.persist();

        // When
        List<ArticleEntity> testAuthorArticles = ArticleEntity.findByAuthor(testAuthor);
        List<ArticleEntity> author2Articles = ArticleEntity.findByAuthor(author2);

        // Then
        assertEquals(2, testAuthorArticles.size());
        assertEquals(1, author2Articles.size());
        assertTrue(testAuthorArticles.stream().allMatch(a -> testAuthor.id.equals(a.author.id)));
        assertTrue(author2Articles.stream().allMatch(a -> author2.id.equals(a.author.id)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find articles by author name")
    void shouldFindArticlesByAuthorName() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article1 = new Article("Title 1", "https://example.com/1", "Content 1");
        Article article2 = new Article("Title 2", "https://example.com/2", "Content 2");
        
        ArticleEntity entity1 = new ArticleEntity(article1, "test-crawler", testAuthor);
        ArticleEntity entity2 = new ArticleEntity(article2, "test-crawler", testAuthor);
        
        entity1.persist();
        entity2.persist();

        // When
        List<ArticleEntity> articles = ArticleEntity.findByAuthorName("Test Author");

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().allMatch(a -> "Test Author".equals(a.author.name)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should find articles for analysis with filters")
    void shouldFindArticlesForAnalysisWithFilters() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        AuthorEntity author2 = new AuthorEntity("Author 2");
        author2.persist();
        
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        
        Article article1 = new Article("Java Programming", "https://example.com/1", "Content 1");
        Article article2 = new Article("Python Guide", "https://example.com/2", "Content 2");
        Article article3 = new Article("JavaScript Tips", "https://example.com/3", "Content 3");
        
        ArticleEntity entity1 = new ArticleEntity(article1, "test-crawler", testAuthor);
        ArticleEntity entity2 = new ArticleEntity(article2, "test-crawler", author2);
        ArticleEntity entity3 = new ArticleEntity(article3, "test-crawler", testAuthor);
        
        // Set specific creation times
        entity1.createdAt = yesterday;
        entity2.createdAt = now;
        entity3.createdAt = now;
        
        entity1.persist();
        entity2.persist();
        entity3.persist();

        // When - filter by author
        List<ArticleEntity> byAuthor = ArticleEntity.findForAnalysis(testAuthor.id, null, null, null);
        
        // When - filter by title search
        List<ArticleEntity> byTitle = ArticleEntity.findForAnalysis(null, "java", null, null);
        
        // When - filter by date range
        List<ArticleEntity> byDate = ArticleEntity.findForAnalysis(null, null, yesterday.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        
        // When - combined filters
        List<ArticleEntity> combined = ArticleEntity.findForAnalysis(testAuthor.id, "Programming", null, null);

        // Then
        assertEquals(2, byAuthor.size()); // testAuthor has 2 articles
        assertEquals(2, byTitle.size()); // "Java" and "JavaScript" match
        assertEquals(3, byDate.size()); // All articles in date range
        assertEquals(1, combined.size()); // Only "Java Programming" by testAuthor
        assertEquals("JavaScript Tips", byAuthor.get(0).title); // Should be ordered by createdAt DESC
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize input data")
    void shouldSanitizeInputData() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article(
            "<script>alert('xss')</script>Title",
            "https://example.com/article" + "x".repeat(2100), // Too long URL
            "Content with <script> tags"
        );
        String longCrawlerSource = "crawler-" + "x".repeat(50); // Too long

        // When
        ArticleEntity articleEntity = new ArticleEntity(article, longCrawlerSource, testAuthor);
        articleEntity.persist();

        // Then
        assertFalse(articleEntity.title.contains("<script>"));
        assertTrue(articleEntity.title.contains("&lt;script&gt;"));
        assertEquals(2048, articleEntity.url.length()); // Should be truncated
        assertEquals(50, articleEntity.crawlerSource.length()); // Should be truncated
        assertEquals("Content with <script> tags", articleEntity.text); // Text should not be HTML escaped
    }

    @Test
    @TestTransaction
    @DisplayName("Should convert to Article record")
    void shouldConvertToArticleRecord() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article originalArticle = new Article("Test Title", "https://example.com/article", "Test content");
        ArticleEntity articleEntity = new ArticleEntity(originalArticle, "test-crawler", testAuthor);
        articleEntity.persist();

        // When
        Article convertedArticle = articleEntity.toArticle();

        // Then
        assertEquals(originalArticle.title(), convertedArticle.title());
        assertEquals(originalArticle.url(), convertedArticle.url());
        assertEquals(originalArticle.text(), convertedArticle.text());
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle empty list results for invalid inputs")
    void shouldHandleEmptyListResultsForInvalidInputs() {
        // When
        List<ArticleEntity> byCrawlerSource = ArticleEntity.findByCrawlerSource(null);
        List<ArticleEntity> byAuthor = ArticleEntity.findByAuthor(null);
        List<ArticleEntity> byAuthorName = ArticleEntity.findByAuthorName(null);

        // Then
        assertTrue(byCrawlerSource.isEmpty());
        assertTrue(byAuthor.isEmpty());
        assertTrue(byAuthorName.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article("Test Title", "https://example.com/article", "Test content");
        ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", testAuthor);
        articleEntity.persist();

        // When
        String toString = articleEntity.toString();

        // Then
        assertTrue(toString.contains("ArticleEntity"));
        assertTrue(toString.contains("Test Title"));
        assertTrue(toString.contains("https://example.com/article"));
        assertTrue(toString.contains("test-crawler"));
        assertTrue(toString.contains("Test Author"));
        assertTrue(toString.contains("id=" + articleEntity.id));
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle whitespace in URLs and trim properly")
    void shouldHandleWhitespaceInUrlsAndTrimProperly() {
        // Given
        AuthorEntity testAuthor = createTestAuthor();
        Article article = new Article("Test Title", "  https://example.com/article  ", "Test content");

        // When
        ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", testAuthor);
        articleEntity.persist();

        // Then
        assertEquals("https://example.com/article", articleEntity.url);
        
        // Should be findable by trimmed URL
        ArticleEntity found = ArticleEntity.findByUrl("https://example.com/article");
        assertNotNull(found);
        assertEquals(articleEntity.id, found.id);
    }
}