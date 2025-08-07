package ai.falsify.crawlers.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ArticleFilterTest {

    @Test
    @DisplayName("Should create filter with defaults")
    void testWithDefaults() {
        // When
        ArticleFilter filter = ArticleFilter.withDefaults();

        // Then
        assertNull(filter.authorId());
        assertNull(filter.titleSearch());
        assertNull(filter.fromDate());
        assertNull(filter.toDate());
        assertEquals(0, filter.page());
        assertEquals(20, filter.size());
    }

    @Test
    @DisplayName("Should create filter with custom pagination")
    void testWithPagination() {
        // When
        ArticleFilter filter = ArticleFilter.withPagination(2, 50);

        // Then
        assertNull(filter.authorId());
        assertNull(filter.titleSearch());
        assertNull(filter.fromDate());
        assertNull(filter.toDate());
        assertEquals(2, filter.page());
        assertEquals(50, filter.size());
    }

    @Test
    @DisplayName("Should build filter using builder pattern")
    void testBuilderPattern() {
        // Given
        Long authorId = 123L;
        String titleSearch = "test title";
        Instant fromDate = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant toDate = Instant.now();

        // When
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(authorId)
                .titleSearch(titleSearch)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(1)
                .size(25)
                .build();

        // Then
        assertEquals(authorId, filter.authorId());
        assertEquals(titleSearch, filter.titleSearch());
        assertEquals(fromDate, filter.fromDate());
        assertEquals(toDate, filter.toDate());
        assertEquals(1, filter.page());
        assertEquals(25, filter.size());
    }

    @Test
    @DisplayName("Should get page with default value")
    void testGetPageOrDefault() {
        // Given
        ArticleFilter filterWithPage = ArticleFilter.builder().page(5).build();
        ArticleFilter filterWithoutPage = ArticleFilter.builder().page(null).build();

        // When & Then
        assertEquals(5, filterWithPage.getPageOrDefault());
        assertEquals(0, filterWithoutPage.getPageOrDefault());
    }

    @Test
    @DisplayName("Should get size with default value")
    void testGetSizeOrDefault() {
        // Given
        ArticleFilter filterWithSize = ArticleFilter.builder().size(100).build();
        ArticleFilter filterWithoutSize = ArticleFilter.builder().size(null).build();

        // When & Then
        assertEquals(100, filterWithSize.getSizeOrDefault());
        assertEquals(20, filterWithoutSize.getSizeOrDefault());
    }

    @Test
    @DisplayName("Should detect when filters are present")
    void testHasFilters() {
        // Given
        ArticleFilter emptyFilter = ArticleFilter.withDefaults();
        ArticleFilter filterWithAuthor = ArticleFilter.builder().authorId(123L).build();
        ArticleFilter filterWithTitle = ArticleFilter.builder().titleSearch("test").build();
        ArticleFilter filterWithFromDate = ArticleFilter.builder().fromDate(Instant.now()).build();
        ArticleFilter filterWithToDate = ArticleFilter.builder().toDate(Instant.now()).build();
        ArticleFilter filterWithEmptyTitle = ArticleFilter.builder().titleSearch("").build();
        ArticleFilter filterWithWhitespaceTitle = ArticleFilter.builder().titleSearch("   ").build();

        // When & Then
        assertFalse(emptyFilter.hasFilters());
        assertTrue(filterWithAuthor.hasFilters());
        assertTrue(filterWithTitle.hasFilters());
        assertTrue(filterWithFromDate.hasFilters());
        assertTrue(filterWithToDate.hasFilters());
        assertFalse(filterWithEmptyTitle.hasFilters());
        assertFalse(filterWithWhitespaceTitle.hasFilters());
    }

    @Test
    @DisplayName("Should sanitize title search")
    void testGetSanitizedTitleSearch() {
        // Given
        ArticleFilter filterWithTitle = ArticleFilter.builder().titleSearch("  test title  ").build();
        ArticleFilter filterWithEmptyTitle = ArticleFilter.builder().titleSearch("").build();
        ArticleFilter filterWithWhitespaceTitle = ArticleFilter.builder().titleSearch("   ").build();
        ArticleFilter filterWithNullTitle = ArticleFilter.builder().titleSearch(null).build();

        // When & Then
        assertEquals("test title", filterWithTitle.getSanitizedTitleSearch());
        assertNull(filterWithEmptyTitle.getSanitizedTitleSearch());
        assertNull(filterWithWhitespaceTitle.getSanitizedTitleSearch());
        assertNull(filterWithNullTitle.getSanitizedTitleSearch());
    }

    @Test
    @DisplayName("Should handle builder with partial data")
    void testBuilderPartialData() {
        // When
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(456L)
                .titleSearch("partial")
                .build();

        // Then
        assertEquals(456L, filter.authorId());
        assertEquals("partial", filter.titleSearch());
        assertNull(filter.fromDate());
        assertNull(filter.toDate());
        assertEquals(0, filter.page()); // Default from builder
        assertEquals(20, filter.size()); // Default from builder
    }

    @Test
    @DisplayName("Should handle builder method chaining")
    void testBuilderMethodChaining() {
        // Given
        Instant now = Instant.now();

        // When
        ArticleFilter filter = ArticleFilter.builder()
                .authorId(789L)
                .titleSearch("chained")
                .fromDate(now.minus(1, ChronoUnit.DAYS))
                .toDate(now)
                .page(3)
                .size(15)
                .build();

        // Then
        assertEquals(789L, filter.authorId());
        assertEquals("chained", filter.titleSearch());
        assertEquals(now.minus(1, ChronoUnit.DAYS), filter.fromDate());
        assertEquals(now, filter.toDate());
        assertEquals(3, filter.page());
        assertEquals(15, filter.size());
    }

    @Test
    @DisplayName("Should create multiple filters independently")
    void testMultipleFiltersIndependence() {
        // Given
        ArticleFilter.Builder builder = ArticleFilter.builder();

        // When
        ArticleFilter filter1 = builder.authorId(1L).build();
        ArticleFilter filter2 = ArticleFilter.builder().authorId(2L).build();

        // Then
        assertEquals(1L, filter1.authorId());
        assertEquals(2L, filter2.authorId());
        // Ensure they are independent
        assertNotEquals(filter1.authorId(), filter2.authorId());
    }
}