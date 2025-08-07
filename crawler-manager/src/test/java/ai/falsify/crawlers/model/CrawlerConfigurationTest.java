package ai.falsify.crawlers.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrawlerConfigurationTest {

    @Test
    void testCrawlerConfigurationWithAuthorInfo() {
        // Given
        String id = "test-crawler";
        String name = "Test Crawler";
        String baseUrl = "http://localhost:8080";
        Integer port = 8080;
        String healthEndpoint = "/health";
        String crawlEndpoint = "/crawl";
        String statusEndpoint = "/status";
        Boolean enabled = true;
        String authorName = "Test Author";
        String authorAvatarUrl = "https://example.com/avatar.jpg";

        // When
        CrawlerConfiguration config = new CrawlerConfiguration(
                id, name, baseUrl, port, healthEndpoint, crawlEndpoint, 
                statusEndpoint, enabled, authorName, authorAvatarUrl
        );

        // Then
        assertEquals(id, config.id);
        assertEquals(name, config.name);
        assertEquals(baseUrl, config.baseUrl);
        assertEquals(port, config.port);
        assertEquals(healthEndpoint, config.healthEndpoint);
        assertEquals(crawlEndpoint, config.crawlEndpoint);
        assertEquals(statusEndpoint, config.statusEndpoint);
        assertEquals(enabled, config.enabled);
        assertEquals(authorName, config.authorName);
        assertEquals(authorAvatarUrl, config.authorAvatarUrl);
    }

    @Test
    void testCrawlerConfigurationWithoutAuthorInfo() {
        // Given
        String id = "test-crawler";
        String name = "Test Crawler";
        String baseUrl = "http://localhost:8080";
        Integer port = 8080;
        String healthEndpoint = "/health";
        String crawlEndpoint = "/crawl";
        String statusEndpoint = "/status";
        Boolean enabled = true;

        // When
        CrawlerConfiguration config = new CrawlerConfiguration(
                id, name, baseUrl, port, healthEndpoint, crawlEndpoint, 
                statusEndpoint, enabled
        );

        // Then
        assertEquals(id, config.id);
        assertEquals(name, config.name);
        assertEquals(baseUrl, config.baseUrl);
        assertEquals(port, config.port);
        assertEquals(healthEndpoint, config.healthEndpoint);
        assertEquals(crawlEndpoint, config.crawlEndpoint);
        assertEquals(statusEndpoint, config.statusEndpoint);
        assertEquals(enabled, config.enabled);
        assertNull(config.authorName);
        assertNull(config.authorAvatarUrl);
    }

    @Test
    void testToStringIncludesAuthorInfo() {
        // Given
        CrawlerConfiguration config = new CrawlerConfiguration(
                "test", "Test", "http://localhost:8080", 8080, 
                "/health", "/crawl", "/status", true, 
                "Test Author", "https://example.com/avatar.jpg"
        );

        // When
        String toString = config.toString();

        // Then
        assertTrue(toString.contains("authorName='Test Author'"));
        assertTrue(toString.contains("authorAvatarUrl='https://example.com/avatar.jpg'"));
    }
}