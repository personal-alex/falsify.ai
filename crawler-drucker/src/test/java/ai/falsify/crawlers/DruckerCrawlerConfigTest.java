package ai.falsify.crawlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DruckerCrawlerConfig properties validation.
 * This test validates the configuration structure without requiring full Quarkus context.
 */
class DruckerCrawlerConfigTest {

    @Test
    void testConfigurationStructure() {
        // Test that the configuration class is properly structured
        // This validates the configuration class structure and methods
        
        // Verify that the configuration class exists and has the expected methods
        assertFalse(DruckerCrawlerConfig.class.isInterface());
        assertTrue(DruckerCrawlerConfig.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class));
        
        // Verify nested classes exist
        assertTrue(DruckerCrawlerConfig.AuthorConfig.class.isMemberClass());
        
        // Verify that the configuration class has the expected methods
        try {
            DruckerCrawlerConfig.class.getMethod("baseUrl");
            DruckerCrawlerConfig.class.getMethod("maxPages");
            DruckerCrawlerConfig.class.getMethod("pageDelay");
            DruckerCrawlerConfig.class.getMethod("enableEarlyTermination");
            DruckerCrawlerConfig.class.getMethod("emptyPageThreshold");
            DruckerCrawlerConfig.class.getMethod("author");
            
            // Author config methods
            DruckerCrawlerConfig.AuthorConfig.class.getMethod("name");
            DruckerCrawlerConfig.AuthorConfig.class.getMethod("avatarUrl");
            DruckerCrawlerConfig.AuthorConfig.class.getMethod("fallbackName");
            
        } catch (NoSuchMethodException e) {
            fail("Configuration class is missing expected methods: " + e.getMessage());
        }
    }

    @Test
    void testConfigurationAnnotations() {
        // Verify that the configuration class has the required annotations
        assertTrue(DruckerCrawlerConfig.class.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class));
        
        // Verify that fields have @ConfigProperty annotations
        try {
            var baseUrlField = DruckerCrawlerConfig.class.getDeclaredField("baseUrl");
            assertTrue(baseUrlField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var maxPagesField = DruckerCrawlerConfig.class.getDeclaredField("maxPages");
            assertTrue(maxPagesField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var configProperty = maxPagesField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("crawler.drucker.pagination.max-pages", configProperty.name());
            assertEquals("10", configProperty.defaultValue());
            
        } catch (NoSuchFieldException e) {
            fail("Configuration class is missing expected fields: " + e.getMessage());
        }
    }

    @Test
    void testAuthorConfigurationFields() {
        // Verify that author configuration fields have @ConfigProperty annotations
        try {
            var authorNameField = DruckerCrawlerConfig.class.getDeclaredField("authorName");
            assertTrue(authorNameField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var configProperty = authorNameField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("crawler.drucker.author.name", configProperty.name());
            assertEquals("Unknown Author", configProperty.defaultValue());
            
            var authorAvatarUrlField = DruckerCrawlerConfig.class.getDeclaredField("authorAvatarUrl");
            assertTrue(authorAvatarUrlField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var avatarConfigProperty = authorAvatarUrlField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("crawler.drucker.author.avatar-url", avatarConfigProperty.name());
            
            var authorFallbackNameField = DruckerCrawlerConfig.class.getDeclaredField("authorFallbackName");
            assertTrue(authorFallbackNameField.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class));
            
            var fallbackConfigProperty = authorFallbackNameField.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
            assertEquals("crawler.drucker.author.fallback-name", fallbackConfigProperty.name());
            assertEquals("Unknown Author", fallbackConfigProperty.defaultValue());
            
        } catch (NoSuchFieldException e) {
            fail("Author configuration fields are missing: " + e.getMessage());
        }
    }
}