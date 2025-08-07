package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CrawlerConfigurationServiceAuthorTest {

    @Inject
    CrawlerConfigurationService configurationService;

    @Test
    void testLoadCrawlerConfigurationsWithAuthorInfo() {
        // When
        List<CrawlerConfiguration> configurations = configurationService.getAllCrawlers();

        // Then
        assertFalse(configurations.isEmpty(), "Should have at least one crawler configuration");

        // Find caspit crawler
        Optional<CrawlerConfiguration> caspitConfig = configurations.stream()
                .filter(config -> "caspit".equals(config.id))
                .findFirst();

        assertTrue(caspitConfig.isPresent(), "Caspit crawler configuration should be present");
        assertEquals("Ben Caspit", caspitConfig.get().authorName);
        assertEquals("https://www.maariv.co.il/images/authors/ben-caspit.jpg", caspitConfig.get().authorAvatarUrl);

        // Find drucker crawler
        Optional<CrawlerConfiguration> druckerConfig = configurations.stream()
                .filter(config -> "drucker".equals(config.id))
                .findFirst();

        assertTrue(druckerConfig.isPresent(), "Drucker crawler configuration should be present");
        assertEquals("Raviv Drucker", druckerConfig.get().authorName);
        assertEquals("https://drucker10.net/images/raviv-drucker.jpg", druckerConfig.get().authorAvatarUrl);
    }

    @Test
    void testGetCrawlerConfigurationWithAuthorInfo() {
        // When
        Optional<CrawlerConfiguration> caspitConfig = configurationService.getCrawlerConfiguration("caspit");

        // Then
        assertTrue(caspitConfig.isPresent(), "Caspit configuration should be present");
        assertEquals("Ben Caspit", caspitConfig.get().authorName);
        assertEquals("https://www.maariv.co.il/images/authors/ben-caspit.jpg", caspitConfig.get().authorAvatarUrl);
    }
}