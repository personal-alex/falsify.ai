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
class AuthorEntityTest {

    @Inject
    EntityManager entityManager;

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

    @Test
    @TestTransaction
    @DisplayName("Should create author with name and avatar URL")
    void shouldCreateAuthorWithNameAndAvatarUrl() {
        // Given
        String name = "John Doe";
        String avatarUrl = "https://example.com/avatar.jpg";

        // When
        AuthorEntity author = new AuthorEntity(name, avatarUrl);
        author.persist();

        // Then
        assertNotNull(author.id);
        assertEquals(name, author.name);
        assertEquals(avatarUrl, author.avatarUrl);
        assertNotNull(author.createdAt);
        assertNotNull(author.updatedAt);
        assertTrue(author.createdAt.isBefore(Instant.now().plusSeconds(1)));
        assertTrue(author.updatedAt.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @TestTransaction
    @DisplayName("Should create author with name only")
    void shouldCreateAuthorWithNameOnly() {
        // Given
        String name = "Jane Smith";

        // When
        AuthorEntity author = new AuthorEntity(name);
        author.persist();

        // Then
        assertNotNull(author.id);
        assertEquals(name, author.name);
        assertNull(author.avatarUrl);
        assertNotNull(author.createdAt);
        assertNotNull(author.updatedAt);
    }

    @Test
    @TestTransaction
    @DisplayName("Should find author by name case-insensitive")
    void shouldFindAuthorByNameCaseInsensitive() {
        // Given
        String name = "Test Author";
        AuthorEntity author = new AuthorEntity(name);
        author.persist();

        // When
        AuthorEntity foundLower = AuthorEntity.findByName("test author");
        AuthorEntity foundUpper = AuthorEntity.findByName("TEST AUTHOR");
        AuthorEntity foundMixed = AuthorEntity.findByName("Test Author");

        // Then
        assertNotNull(foundLower);
        assertNotNull(foundUpper);
        assertNotNull(foundMixed);
        assertEquals(author.id, foundLower.id);
        assertEquals(author.id, foundUpper.id);
        assertEquals(author.id, foundMixed.id);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return null when finding non-existent author")
    void shouldReturnNullWhenFindingNonExistentAuthor() {
        // When
        AuthorEntity found = AuthorEntity.findByName("Non-existent Author");

        // Then
        assertNull(found);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return null when finding author with null or empty name")
    void shouldReturnNullWhenFindingAuthorWithNullOrEmptyName() {
        // When
        AuthorEntity foundNull = AuthorEntity.findByName(null);
        AuthorEntity foundEmpty = AuthorEntity.findByName("");
        AuthorEntity foundWhitespace = AuthorEntity.findByName("   ");

        // Then
        assertNull(foundNull);
        assertNull(foundEmpty);
        assertNull(foundWhitespace);
    }

    @Test
    @TestTransaction
    @DisplayName("Should find or create existing author")
    void shouldFindOrCreateExistingAuthor() {
        // Given
        String name = "Existing Author";
        String originalAvatarUrl = "https://example.com/original.jpg";
        AuthorEntity original = new AuthorEntity(name, originalAvatarUrl);
        original.persist();
        Long originalId = original.id;

        // When
        AuthorEntity found = AuthorEntity.findOrCreate(name, "https://example.com/new.jpg");

        // Then
        assertEquals(originalId, found.id);
        assertEquals(name, found.name);
        assertEquals("https://example.com/new.jpg", found.avatarUrl); // Should update avatar URL
    }

    @Test
    @TestTransaction
    @DisplayName("Should create new author when not found")
    void shouldCreateNewAuthorWhenNotFound() {
        // Given
        String name = "New Author";
        String avatarUrl = "https://example.com/new.jpg";

        // When
        AuthorEntity created = AuthorEntity.findOrCreate(name, avatarUrl);

        // Then
        assertNotNull(created.id);
        assertEquals(name, created.name);
        assertEquals(avatarUrl, created.avatarUrl);
        
        // Verify it was persisted
        AuthorEntity found = AuthorEntity.findByName(name);
        assertNotNull(found);
        assertEquals(created.id, found.id);
    }

    @Test
    @TestTransaction
    @DisplayName("Should return unknown author for null or empty name")
    void shouldReturnUnknownAuthorForNullOrEmptyName() {
        // When
        AuthorEntity unknownForNull = AuthorEntity.findOrCreate(null, "https://example.com/avatar.jpg");
        AuthorEntity unknownForEmpty = AuthorEntity.findOrCreate("", "https://example.com/avatar.jpg");
        AuthorEntity unknownForWhitespace = AuthorEntity.findOrCreate("   ", "https://example.com/avatar.jpg");

        // Then
        assertNotNull(unknownForNull);
        assertNotNull(unknownForEmpty);
        assertNotNull(unknownForWhitespace);
        assertEquals("Unknown Author", unknownForNull.name);
        assertEquals("Unknown Author", unknownForEmpty.name);
        assertEquals("Unknown Author", unknownForWhitespace.name);
        
        // Should be the same instance
        assertEquals(unknownForNull.id, unknownForEmpty.id);
        assertEquals(unknownForEmpty.id, unknownForWhitespace.id);
    }

    @Test
    @TestTransaction
    @DisplayName("Should get or create unknown author")
    void shouldGetOrCreateUnknownAuthor() {
        // When
        AuthorEntity unknown1 = AuthorEntity.getUnknownAuthor();
        AuthorEntity unknown2 = AuthorEntity.getUnknownAuthor();

        // Then
        assertNotNull(unknown1);
        assertNotNull(unknown2);
        assertEquals("Unknown Author", unknown1.name);
        assertEquals("Unknown Author", unknown2.name);
        assertEquals(unknown1.id, unknown2.id); // Should be the same instance
    }

    @Test
    @TestTransaction
    @DisplayName("Should find all authors ordered by name")
    void shouldFindAllAuthorsOrderedByName() {
        // Given
        AuthorEntity author1 = new AuthorEntity("Charlie");
        AuthorEntity author2 = new AuthorEntity("Alice");
        AuthorEntity author3 = new AuthorEntity("Bob");
        author1.persist();
        author2.persist();
        author3.persist();

        // When
        List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();

        // Then
        assertEquals(3, authors.size());
        assertEquals("Alice", authors.get(0).name);
        assertEquals("Bob", authors.get(1).name);
        assertEquals("Charlie", authors.get(2).name);
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize author name to prevent XSS")
    void shouldSanitizeAuthorNameToPreventXSS() {
        // Given
        String maliciousName = "<script>alert('xss')</script>John Doe";

        // When
        AuthorEntity author = new AuthorEntity(maliciousName);
        author.persist();

        // Then
        assertFalse(author.name.contains("<script>"));
        assertFalse(author.name.contains("</script>"));
        assertTrue(author.name.contains("&lt;script&gt;"));
        assertTrue(author.name.contains("&lt;/script&gt;"));
        assertTrue(author.name.contains("John Doe"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should sanitize avatar URL")
    void shouldSanitizeAvatarUrl() {
        // Given
        String validUrl = "https://example.com/avatar.jpg";
        String invalidUrl = "javascript:alert('xss')";
        String tooLongUrl = "https://example.com/" + "a".repeat(1000);

        // When
        AuthorEntity author1 = new AuthorEntity("User1", validUrl);
        AuthorEntity author2 = new AuthorEntity("User2", invalidUrl);
        AuthorEntity author3 = new AuthorEntity("User3", tooLongUrl);
        author1.persist();
        author2.persist();
        author3.persist();

        // Then
        assertEquals(validUrl, author1.avatarUrl);
        assertNull(author2.avatarUrl); // Invalid URL should be null
        assertEquals(1000, author3.avatarUrl.length()); // Should be truncated
        assertTrue(author3.avatarUrl.startsWith("https://example.com/"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle long author names")
    void shouldHandleLongAuthorNames() {
        // Given
        String longName = "A".repeat(300); // Longer than 255 characters

        // When
        AuthorEntity author = new AuthorEntity(longName);
        author.persist();

        // Then
        assertEquals(255, author.name.length()); // Should be truncated
        assertTrue(author.name.startsWith("A"));
    }

    @Test
    @TestTransaction
    @DisplayName("Should handle whitespace in author names")
    void shouldHandleWhitespaceInAuthorNames() {
        // Given
        String nameWithWhitespace = "  John   Doe  ";

        // When
        AuthorEntity author = new AuthorEntity(nameWithWhitespace);
        author.persist();

        // Then
        assertEquals("John   Doe", author.name); // Should trim outer whitespace but preserve inner
    }

    @Test
    @TestTransaction
    @DisplayName("Should count articles for author")
    void shouldCountArticlesForAuthor() {
        // Given
        AuthorEntity author = new AuthorEntity("Test Author");
        author.persist();

        Article article1 = new Article("Title 1", "http://example.com/1", "Content 1");
        Article article2 = new Article("Title 2", "http://example.com/2", "Content 2");
        
        ArticleEntity articleEntity1 = new ArticleEntity(article1, "test-crawler", author);
        ArticleEntity articleEntity2 = new ArticleEntity(article2, "test-crawler", author);
        articleEntity1.persist();
        articleEntity2.persist();

        // When - Use optimized count method instead of loading articles
        long articleCount = author.getArticleCount();

        // Then
        assertEquals(2, articleCount);
    }

    @Test
    @TestTransaction
    @DisplayName("Should efficiently count articles using count field instead of loading articles")
    void shouldEfficientlyCountArticlesUsingCountField() {
        // Given
        AuthorEntity author1 = new AuthorEntity("Author 1");
        AuthorEntity author2 = new AuthorEntity("Author 2");
        author1.persist();
        author2.persist();

        // Create multiple articles for each author
        for (int i = 1; i <= 5; i++) {
            Article article = new Article("Title " + i, "http://example.com/" + i, "Content " + i);
            ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", author1);
            articleEntity.persist();
        }

        for (int i = 6; i <= 8; i++) {
            Article article = new Article("Title " + i, "http://example.com/" + i, "Content " + i);
            ArticleEntity articleEntity = new ArticleEntity(article, "test-crawler", author2);
            articleEntity.persist();
        }

        // When - Use optimized count method (this uses a COUNT query instead of loading all articles)
        long author1Count = author1.getArticleCount();
        long author2Count = author2.getArticleCount();

        // Then
        assertEquals(5, author1Count);
        assertEquals(3, author2Count);

        // Verify this is more efficient than loading articles and counting them
        // The getArticleCount() method uses: ArticleEntity.count("author", this)
        // which is much more efficient than loading all articles into memory
        
        // This would be the inefficient way (commented out to show the difference):
        // List<ArticleEntity> author1Articles = ArticleEntity.list("author", author1);
        // assertEquals(5, author1Articles.size()); // This loads all articles into memory!
    }

    @Test
    @TestTransaction
    @DisplayName("Should return zero article count for author with no articles")
    void shouldReturnZeroArticleCountForAuthorWithNoArticles() {
        // Given
        AuthorEntity author = new AuthorEntity("Author With No Articles");
        author.persist();

        // When
        long articleCount = author.getArticleCount();

        // Then
        assertEquals(0, articleCount);
    }

    @Test
    @TestTransaction
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        // Given
        AuthorEntity author = new AuthorEntity("Test Author", "https://example.com/avatar.jpg");
        author.persist();

        // When
        String toString = author.toString();

        // Then
        assertTrue(toString.contains("AuthorEntity"));
        assertTrue(toString.contains("Test Author"));
        assertTrue(toString.contains("https://example.com/avatar.jpg"));
        assertTrue(toString.contains("id=" + author.id));
    }
}