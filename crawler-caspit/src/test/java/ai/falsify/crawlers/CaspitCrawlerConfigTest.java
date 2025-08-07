package ai.falsify.crawlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for configuration properties validation.
 * This test validates the configuration structure without requiring full Quarkus context.
 */
class CaspitCrawlerConfigTest {

    @Test
    void testConfigurationStructure() {
        // Test that the configuration class is properly structured
        // This validates the configuration class structure and methods
        
        // Verify that the configuration class exists and has the expected methods
        assertFalse(CaspitCrawlerConfig.class.isInterface());
        assertTrue(CaspitCrawlerConfig.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class));
        
        // Verify nested classes exist
        assertTrue(CaspitCrawlerConfig.WebDriverConfig.class.isMemberClass());
        assertTrue(CaspitCrawlerConfig.CrawlingConfig.class.isMemberClass());
        assertTrue(CaspitCrawlerConfig.AuthorConfig.class.isMemberClass());
        
        // Verify that the configuration class has the expected methods
        try {
            CaspitCrawlerConfig.class.getMethod("baseUrl");
            CaspitCrawlerConfig.class.getMethod("maxPages");
            CaspitCrawlerConfig.class.getMethod("pageLoadTimeout");
            CaspitCrawlerConfig.class.getMethod("webdriver");
            CaspitCrawlerConfig.class.getMethod("crawling");
            CaspitCrawlerConfig.class.getMethod("author");
            
            // WebDriver config methods
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("headless");
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("windowWidth");
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("windowHeight");
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("userAgent");
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("implicitWait");
            CaspitCrawlerConfig.WebDriverConfig.class.getMethod("elementWait");
            
            // Crawling config methods
            CaspitCrawlerConfig.CrawlingConfig.class.getMethod("pageDelay");
            CaspitCrawlerConfig.CrawlingConfig.class.getMethod("scrollDelay");
            CaspitCrawlerConfig.CrawlingConfig.class.getMethod("connectionTimeout");
            CaspitCrawlerConfig.CrawlingConfig.class.getMethod("minContentLength");
            
            // Author config methods
            CaspitCrawlerConfig.AuthorConfig.class.getMethod("name");
            CaspitCrawlerConfig.AuthorConfig.class.getMethod("avatarUrl");
            CaspitCrawlerConfig.AuthorConfig.class.getMethod("fallbackName");
            
        } catch (NoSuchMethodException e) {
            fail("Configuration class is missing expected methods: " + e.getMessage());
        }
    }

    @Test
    void testConfigurationAnnotations() {
        // Verify that the configuration class has the required annotations
        assertTrue(CaspitCrawlerConfig.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class));
        
        // Verify that fields have @ConfigProperty annotations
        try {
            var baseUrlField = CaspitCrawlerConfig.class.getDeclaredField("baseUrl");
            assertTrue(baseUrlField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var maxPagesField = CaspitCrawlerConfig.class.getDeclaredField("maxPages");
            assertTrue(maxPagesField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var configProperty = maxPagesField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("caspit.crawler.max-pages", configProperty.name());
            assertEquals("50", configProperty.defaultValue());
            
        } catch (NoSuchFieldException e) {
            fail("Configuration class is missing expected fields: " + e.getMessage());
        }
    }

    @Test
    void testAuthorConfigurationFields() {
        // Verify that author configuration fields have @ConfigProperty annotations
        try {
            var authorNameField = CaspitCrawlerConfig.class.getDeclaredField("authorName");
            assertTrue(authorNameField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var configProperty = authorNameField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("caspit.crawler.author.name", configProperty.name());
            assertEquals("Unknown Author", configProperty.defaultValue());
            
            var authorAvatarUrlField = CaspitCrawlerConfig.class.getDeclaredField("authorAvatarUrl");
            assertTrue(authorAvatarUrlField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var avatarConfigProperty = authorAvatarUrlField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("caspit.crawler.author.avatar-url", avatarConfigProperty.name());
            
            var authorFallbackNameField = CaspitCrawlerConfig.class.getDeclaredField("authorFallbackName");
            assertTrue(authorFallbackNameField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var fallbackConfigProperty = authorFallbackNameField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("caspit.crawler.author.fallback-name", fallbackConfigProperty.name());
            assertEquals("Unknown Author", fallbackConfigProperty.defaultValue());
            
        } catch (NoSuchFieldException e) {
            fail("Author configuration fields are missing: " + e.getMessage());
        }
    }
}