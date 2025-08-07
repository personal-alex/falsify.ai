package ai.falsify.crawlers.common.service;

import ai.falsify.crawlers.common.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for ArticleService.
 * These tests are disabled by default as they create large datasets.
 * Enable them when testing performance characteristics.
 */
@QuarkusTest
@Disabled("Performance tests - enable manually when needed")
class ArticleServicePerformanceTest {

    @Inject
    ArticleService articleService;

    private static final int LARGE_DATASET_SIZE = 10000;
    private static final int AUTHOR_COUNT = 100;
    private final Random random = new Random(42); // Fixed seed for reproducible tests

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean up existing data
        ArticleEntity.deleteAll();
        AuthorEntity.deleteAll();
        
        // Create test dataset
        createLargeTestDataset();
    }

    @Test
    @DisplayName("Should perform well with large dataset - no filters")
    @TestTransaction
    void testPerformanceNoFilters() {
        // Given
        ArticleFilter filter = ArticleFilter.builder()
                .page(0)
                .size(100)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(100, articles.size());
        long executionTime = endTime - startTime;
        System.out.printf("No filters query took %d ms%n", executionTime);
        
        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue(executionTime < 1000, "Query should complete within 1 second");
    }

    @Test
    @DisplayName("Should perform well with author filter")
    @TestTransaction
    void testPerformanceAuthorFilter() {
        // Given
        List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();
        AuthorEntity testAuthor = authors.get(0);
        
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(testAuthor.id)
                .page(0)
                .size(100)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(articles.size() > 0);
        assertTrue(articles.stream().allMatch(a -> a.author.id.equals(testAuthor.id)));
        
        long executionTime = endTime - startTime;
        System.out.printf("Author filter query took %d ms%n", executionTime);
        
        // Should complete within reasonable time
        assertTrue(executionTime < 1000, "Author filter query should complete within 1 second");
    }

    @Test
    @DisplayName("Should perform well with title search")
    @TestTransaction
    void testPerformanceTitleSearch() {
        // Given
        ArticleFilter filter = ArticleFilter.builder()
                .titleSearch("Article")
                .page(0)
                .size(100)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(articles.size() > 0);
        assertTrue(articles.stream().allMatch(a -> a.title.toLowerCase().contains("article")));
        
        long executionTime = endTime - startTime;
        System.out.printf("Title search query took %d ms%n", executionTime);
        
        // Title search might be slower due to LIKE operation
        assertTrue(executionTime < 2000, "Title search query should complete within 2 seconds");
    }

    @Test
    @DisplayName("Should perform well with date range filter")
    @TestTransaction
    void testPerformanceDateRangeFilter() {
        // Given
        Instant fromDate = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toDate = Instant.now().minus(15, ChronoUnit.DAYS);
        
        ArticleFilter filter = ArticleFilter.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .page(0)
                .size(100)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(articles.stream().allMatch(a -> 
            a.createdAt.isAfter(fromDate) && a.createdAt.isBefore(toDate)));
        
        long executionTime = endTime - startTime;
        System.out.printf("Date range filter query took %d ms%n", executionTime);
        
        // Should complete within reasonable time
        assertTrue(executionTime < 1000, "Date range filter query should complete within 1 second");
    }

    @Test
    @DisplayName("Should perform well with combined filters")
    @TestTransaction
    void testPerformanceCombinedFilters() {
        // Given
        List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();
        AuthorEntity testAuthor = authors.get(0);
        Instant fromDate = Instant.now().minus(60, ChronoUnit.DAYS);
        
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(testAuthor.id)
                .titleSearch("Article")
                .fromDate(fromDate)
                .page(0)
                .size(50)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(articles.stream().allMatch(a -> 
            a.author.id.equals(testAuthor.id) && 
            a.title.toLowerCase().contains("article") &&
            a.createdAt.isAfter(fromDate)));
        
        long executionTime = endTime - startTime;
        System.out.printf("Combined filters query took %d ms%n", executionTime);
        
        // Combined filters might be slower
        assertTrue(executionTime < 2000, "Combined filters query should complete within 2 seconds");
    }

    @Test
    @DisplayName("Should perform well with count queries")
    @TestTransaction
    void testPerformanceCountQueries() {
        // Given
        List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();
        AuthorEntity testAuthor = authors.get(0);
        
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(testAuthor.id)
                .build();

        // When
        long startTime = System.currentTimeMillis();
        long count = articleService.getArticleCountForAnalysis(filter);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(count > 0);
        
        long executionTime = endTime - startTime;
        System.out.printf("Count query took %d ms%n", executionTime);
        
        // Count queries should be very fast
        assertTrue(executionTime < 500, "Count query should complete within 500ms");
    }

    @Test
    @DisplayName("Should perform well with pagination through large dataset")
    @TestTransaction
    void testPerformancePagination() {
        // Given
        int pageSize = 100;
        int totalPages = 10;

        // When
        long totalTime = 0;
        for (int page = 0; page < totalPages; page++) {
            ArticleFilter filter = ArticleFilter.builder()
                    .page(page)
                    .size(pageSize)
                    .build();

            long startTime = System.currentTimeMillis();
            List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
            long endTime = System.currentTimeMillis();

            totalTime += (endTime - startTime);
            
            // Each page should return the expected number of articles (except possibly the last)
            if (page < totalPages - 1) {
                assertEquals(pageSize, articles.size());
            }
        }

        // Then
        long averageTime = totalTime / totalPages;
        System.out.printf("Average pagination query took %d ms%n", averageTime);
        
        // Pagination should remain consistent
        assertTrue(averageTime < 1000, "Average pagination query should complete within 1 second");
    }

    @Test
    @DisplayName("Should perform well when getting authors with counts")
    @TestTransaction
    void testPerformanceAuthorsWithCounts() {
        // When
        long startTime = System.currentTimeMillis();
        List<ArticleService.AuthorWithCount> authorsWithCounts = articleService.getAuthorsWithCounts();
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(AUTHOR_COUNT, authorsWithCounts.size());
        assertTrue(authorsWithCounts.stream().allMatch(awc -> awc.articleCount() > 0));
        
        long executionTime = endTime - startTime;
        System.out.printf("Authors with counts query took %d ms%n", executionTime);
        
        // This operation involves multiple queries, so allow more time
        assertTrue(executionTime < 5000, "Authors with counts query should complete within 5 seconds");
    }

    /**
     * Creates a large test dataset for performance testing.
     */
    private void createLargeTestDataset() {
        System.out.printf("Creating large test dataset with %d articles and %d authors...%n", 
                         LARGE_DATASET_SIZE, AUTHOR_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        // Create authors
        List<AuthorEntity> authors = new ArrayList<>();
        for (int i = 0; i < AUTHOR_COUNT; i++) {
            AuthorEntity author = new AuthorEntity(
                "Author " + i, 
                "https://example.com/author" + i + ".jpg"
            );
            author.persist();
            authors.add(author);
        }

        // Create articles
        Instant baseTime = Instant.now().minus(90, ChronoUnit.DAYS);
        for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
            AuthorEntity randomAuthor = authors.get(random.nextInt(AUTHOR_COUNT));
            
            Article article = new Article(
                "Test Article " + i,
                "https://example.com/article" + i,
                "Content of test article " + i + " with some additional text for testing."
            );
            
            ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", randomAuthor);
            
            // Distribute articles across the time range
            articleEntity.createdAt = baseTime.plus(random.nextInt(90), ChronoUnit.DAYS)
                                              .plus(random.nextInt(24), ChronoUnit.HOURS);
            
            articleEntity.persist();
            
            // Flush periodically to avoid memory issues
            if (i % 1000 == 0) {
                ArticleEntity.getEntityManager().flush();
                ArticleEntity.getEntityManager().clear();
                System.out.printf("Created %d articles...%n", i + 1);
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("Dataset creation completed in %d ms%n", endTime - startTime);
    }
}