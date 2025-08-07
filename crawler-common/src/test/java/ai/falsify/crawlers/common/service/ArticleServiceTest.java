package ai.falsify.crawlers.common.service;

import ai.falsify.crawlers.common.model.*;
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
class ArticleServiceTest {

    @Inject
    ArticleService articleService;

    @Inject
    EntityManager entityManager;

    private AuthorEntity testAuthor1;
    private AuthorEntity testAuthor2;
    private ArticleEntity testArticle1;
    private ArticleEntity testArticle2;
    private ArticleEntity testArticle3;

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean up any existing test data in proper order to avoid foreign key
        // constraint violations
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

    private void createTestData() {
        // Create test authors
        testAuthor1 = new AuthorEntity("John Doe", "https://example.com/john.jpg");
        testAuthor1.persist();

        testAuthor2 = new AuthorEntity("Jane Smith", "https://example.com/jane.jpg");
        testAuthor2.persist();

        // Create test articles
        Article article1 = new Article("Test Article 1", "https://example.com/article1", "Content of article 1");
        testArticle1 = new ArticleEntity(article1, "test-crawler", testAuthor1);
        testArticle1.createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        testArticle1.persist();

        Article article2 = new Article("Another Test Article", "https://example.com/article2", "Content of article 2");
        testArticle2 = new ArticleEntity(article2, "test-crawler", testAuthor2);
        testArticle2.createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        testArticle2.persist();

        Article article3 = new Article("Third Article", "https://example.com/article3", "Content of article 3");
        testArticle3 = new ArticleEntity(article3, "test-crawler", testAuthor1);
        testArticle3.createdAt = Instant.now();
        testArticle3.persist();
    }

    @Test
    @DisplayName("Should get all articles for analysis without filters")
    @TestTransaction
    void testGetArticlesForAnalysisNoFilters() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.withDefaults();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(3, articles.size());
        // Should be ordered by createdAt descending
        assertEquals(testArticle3.id, articles.get(0).id);
        assertEquals(testArticle2.id, articles.get(1).id);
        assertEquals(testArticle1.id, articles.get(2).id);
    }

    @Test
    @DisplayName("Should filter articles by author")
    @TestTransaction
    void testGetArticlesForAnalysisByAuthor() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(testAuthor1.id)
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().allMatch(a -> a.author.id.equals(testAuthor1.id)));
        // Should be ordered by createdAt descending
        assertEquals(testArticle3.id, articles.get(0).id);
        assertEquals(testArticle1.id, articles.get(1).id);
    }

    @Test
    @DisplayName("Should filter articles by title search")
    @TestTransaction
    void testGetArticlesForAnalysisByTitleSearch() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.builder()
                .titleSearch("Test")
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().allMatch(a -> a.title.toLowerCase().contains("test")));
    }

    @Test
    @DisplayName("Should filter articles by date range")
    @TestTransaction
    void testGetArticlesForAnalysisByDateRange() {
        // Given
        createTestData();
        Instant fromDate = Instant.now().minus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS);
        Instant toDate = Instant.now().minus(1, ChronoUnit.HOURS);

        ArticleFilter filter = ArticleFilter.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(1, articles.size());
        assertEquals(testArticle2.id, articles.get(0).id);
    }

    @Test
    @DisplayName("Should combine multiple filters")
    @TestTransaction
    void testGetArticlesForAnalysisMultipleFilters() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(testAuthor1.id)
                .titleSearch("Test")
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(1, articles.size());
        assertEquals(testArticle1.id, articles.get(0).id);
        assertEquals("Test Article 1", articles.get(0).title);
        assertEquals(testAuthor1.id, articles.get(0).author.id);
    }

    @Test
    @DisplayName("Should handle pagination")
    @TestTransaction
    void testGetArticlesForAnalysisPagination() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.builder()
                .page(0)
                .size(2)
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(2, articles.size());
        assertEquals(testArticle3.id, articles.get(0).id);
        assertEquals(testArticle2.id, articles.get(1).id);

        // Test second page
        ArticleFilter filter2 = ArticleFilter.builder()
                .page(1)
                .size(2)
                .build();

        List<ArticleEntity> articles2 = articleService.getArticlesForAnalysis(filter2);
        assertEquals(1, articles2.size());
        assertEquals(testArticle1.id, articles2.get(0).id);
    }

    @Test
    @DisplayName("Should get article count for analysis")
    @TestTransaction
    void testGetArticleCountForAnalysis() {
        // Given
        createTestData();
        ArticleFilter filterAll = ArticleFilter.withDefaults();
        ArticleFilter filterByAuthor = ArticleFilter.builder()
                .authorId(testAuthor1.id)
                .build();

        // When - Use count queries instead of loading articles and counting them
        long countAll = articleService.getArticleCountForAnalysis(filterAll);
        long countByAuthor = articleService.getArticleCountForAnalysis(filterByAuthor);

        // Then
        assertEquals(3, countAll);
        assertEquals(2, countByAuthor);

        // Note: This method uses optimized COUNT queries instead of loading all
        // articles
        // which is much more efficient for large datasets
    }

    @Test
    @DisplayName("Should get all authors")
    @TestTransaction
    void testGetAllAuthors() {
        // Given
        createTestData();

        // When
        List<AuthorEntity> authors = articleService.getAllAuthors();

        // Then
        assertEquals(2, authors.size());
        // Should be ordered by name
        assertEquals("Jane Smith", authors.get(0).name);
        assertEquals("John Doe", authors.get(1).name);
    }

    @Test
    @DisplayName("Should prefer authors with counts over loading articles when only count is needed")
    @TestTransaction
    void testPreferAuthorsWithCountsOverLoadingArticles() {
        // Given
        createTestData();

        // When - If we only need author names and article counts, use the optimized
        // method
        List<ArticleService.AuthorWithCount> authorsWithCounts = articleService.getAuthorsWithCounts();

        // Then - This is more efficient than loading all authors and then loading their
        // articles
        assertEquals(2, authorsWithCounts.size());

        // Verify we can get all the information we need without loading articles
        for (ArticleService.AuthorWithCount authorWithCount : authorsWithCounts) {
            assertNotNull(authorWithCount.author().name());
            assertNotNull(authorWithCount.author().id());
            assertTrue(authorWithCount.articleCount() >= 0);

            // This approach is more efficient than:
            // AuthorEntity author = AuthorEntity.findById(authorWithCount.author().id());
            // List<ArticleEntity> articles = ArticleEntity.list("author", author);
            // int count = articles.size(); // This would load all articles into memory!
        }

        // Verify the total count is correct
        long totalCount = authorsWithCounts.stream()
                .mapToLong(ArticleService.AuthorWithCount::articleCount)
                .sum();
        assertEquals(3, totalCount); // testAuthor1 has 2 articles, testAuthor2 has 1
    }

    @Test
    @DisplayName("Should get authors with counts")
    @TestTransaction
    void testGetAuthorsWithCounts() {
        // Given
        createTestData();

        // When
        List<ArticleService.AuthorWithCount> authorsWithCounts = articleService.getAuthorsWithCounts();

        // Then
        assertEquals(2, authorsWithCounts.size());

        // Find John Doe (should have 2 articles)
        ArticleService.AuthorWithCount johnDoe = authorsWithCounts.stream()
                .filter(awc -> "John Doe".equals(awc.author().name()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, johnDoe.articleCount());

        // Find Jane Smith (should have 1 article)
        ArticleService.AuthorWithCount janeSmith = authorsWithCounts.stream()
                .filter(awc -> "Jane Smith".equals(awc.author().name()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, janeSmith.articleCount());
    }

    @Test
    @DisplayName("Should get authors with counts efficiently without loading articles")
    @TestTransaction
    void testGetAuthorsWithCountsPerformance() {
        // Given
        createTestData();

        // When - Use the optimized method that uses count fields
        List<ArticleService.AuthorWithCount> authorsWithCounts = articleService.getAuthorsWithCounts();

        // Then - Verify we get the same results as if we loaded articles and counted
        // them
        // but without the performance overhead of loading all articles
        assertEquals(2, authorsWithCounts.size());

        // Verify total article count matches what we expect
        long totalArticleCount = authorsWithCounts.stream()
                .mapToLong(ArticleService.AuthorWithCount::articleCount)
                .sum();
        assertEquals(3, totalArticleCount);

        // Verify each author has the correct count using the optimized approach
        for (ArticleService.AuthorWithCount authorWithCount : authorsWithCounts) {
            // This uses the optimized getArticleCount() method instead of loading articles
            AuthorEntity author = AuthorEntity.findById(authorWithCount.author().id());
            assertEquals(authorWithCount.articleCount(), author.getArticleCount());
        }
    }

    @Test
    @DisplayName("Should create article with author")
    @TestTransaction
    void testCreateArticleWithAuthor() {
        // Given
        Article newArticle = new Article("New Article", "https://example.com/new", "New content");
        String authorName = "New Author";
        String avatarUrl = "https://example.com/new-author.jpg";

        // When
        ArticleEntity createdArticle = articleService.createArticleWithAuthor(
                newArticle, "test-crawler", authorName, avatarUrl);

        // Then
        assertNotNull(createdArticle);
        assertNotNull(createdArticle.id);
        assertEquals("New Article", createdArticle.title);
        assertEquals("https://example.com/new", createdArticle.url);
        assertEquals("test-crawler", createdArticle.crawlerSource);

        assertNotNull(createdArticle.author);
        assertEquals("New Author", createdArticle.author.name);
        assertEquals("https://example.com/new-author.jpg", createdArticle.author.avatarUrl);
    }

    @Test
    @DisplayName("Should reuse existing author when creating article")
    @TestTransaction
    void testCreateArticleWithExistingAuthor() {
        // Given
        createTestData();
        Article newArticle = new Article("Another Article", "https://example.com/another", "Another content");
        String existingAuthorName = "John Doe";

        // When
        ArticleEntity createdArticle = articleService.createArticleWithAuthor(
                newArticle, "test-crawler", existingAuthorName, null);

        // Then
        assertNotNull(createdArticle);
        assertEquals(testAuthor1.id, createdArticle.author.id);
        assertEquals("John Doe", createdArticle.author.name);
    }

    @Test
    @DisplayName("Should get articles by IDs")
    @TestTransaction
    void testGetArticlesByIds() {
        // Given
        createTestData();
        List<Long> articleIds = List.of(testArticle1.id, testArticle3.id);

        // When
        List<ArticleEntity> articles = articleService.getArticlesByIds(articleIds);

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().anyMatch(a -> a.id.equals(testArticle1.id)));
        assertTrue(articles.stream().anyMatch(a -> a.id.equals(testArticle3.id)));
    }

    @Test
    @DisplayName("Should handle empty ID list")
    @TestTransaction
    void testGetArticlesByIdsEmpty() {
        // Given
        List<Long> emptyIds = List.of();

        // When
        List<ArticleEntity> articles = articleService.getArticlesByIds(emptyIds);

        // Then
        assertTrue(articles.isEmpty());
    }

    @Test
    @DisplayName("Should get unanalyzed articles")
    @TestTransaction
    void testGetUnanalyzedArticles() {
        // Given - all articles are unanalyzed by default
        createTestData();
        ArticleFilter filter = ArticleFilter.withDefaults();

        // When
        List<ArticleEntity> unanalyzedArticles = articleService.getUnanalyzedArticles(filter);

        // Then
        assertEquals(3, unanalyzedArticles.size());
    }

    @Test
    @DisplayName("Should handle case-insensitive title search")
    @TestTransaction
    void testGetArticlesForAnalysisCaseInsensitiveSearch() {
        // Given
        createTestData();
        ArticleFilter filter = ArticleFilter.builder()
                .titleSearch("TEST")
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().allMatch(a -> a.title.toLowerCase().contains("test")));
    }

    @Test
    @DisplayName("Should handle null and empty title search")
    @TestTransaction
    void testGetArticlesForAnalysisNullTitleSearch() {
        // Given
        createTestData();
        ArticleFilter filterNull = ArticleFilter.builder()
                .titleSearch(null)
                .build();

        ArticleFilter filterEmpty = ArticleFilter.builder()
                .titleSearch("")
                .build();

        ArticleFilter filterWhitespace = ArticleFilter.builder()
                .titleSearch("   ")
                .build();

        // When
        List<ArticleEntity> articlesNull = articleService.getArticlesForAnalysis(filterNull);
        List<ArticleEntity> articlesEmpty = articleService.getArticlesForAnalysis(filterEmpty);
        List<ArticleEntity> articlesWhitespace = articleService.getArticlesForAnalysis(filterWhitespace);

        // Then - all should return all articles (no title filter applied)
        assertEquals(3, articlesNull.size());
        assertEquals(3, articlesEmpty.size());
        assertEquals(3, articlesWhitespace.size());
    }

    @Test
    @DisplayName("Should handle invalid author ID")
    @TestTransaction
    void testGetArticlesForAnalysisInvalidAuthorId() {
        // Given
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(999L) // Non-existent author ID
                .build();

        // When
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);

        // Then
        assertTrue(articles.isEmpty());
    }
}