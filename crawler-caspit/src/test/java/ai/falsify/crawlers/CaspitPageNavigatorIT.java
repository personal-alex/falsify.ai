package ai.falsify.crawlers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CaspitPageNavigator with real WebDriver testing.
 * Tests getAllArticleLinks method against sample HTML pages with real browser interaction.
 * Verifies four-articles-in-row section targeting, link extraction, and pagination functionality.
 * Uses Quarkus REST Assured for serving HTML fixtures.
 */
@QuarkusTest
@TestProfile(CaspitPageNavigatorIT.IntegrationTestProfile.class)
class CaspitPageNavigatorIT {

    @Inject
    CaspitPageNavigator navigator;

    @Inject 
    CaspitCrawlerConfig config;

    @BeforeAll
    static void setupTestServer() {
        // Use Quarkus test server URL - port will be set by Quarkus during test execution
        System.out.println("Using Quarkus test server");
    }
    
    private String getTestBaseUrl() {
        return "http://localhost:" + RestAssured.port;
    }

    @BeforeEach
    void setUp() {
        // Ensure navigator is properly initialized
        assertNotNull(navigator, "CaspitPageNavigator should be injected");
        assertNotNull(config, "CaspitCrawlerConfig should be injected");
    }

    @AfterEach
    void tearDown() {
        // Clean up any WebDriver resources if needed
        try {
            if (navigator != null) {
                // Allow some time for any ongoing operations to complete
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Should extract article links from four-articles-in-row sections")
    void testGetAllArticleLinksFromFourArticlesInRowSections() {
        // Test against the sample HTML page with articles
        String testUrl = getTestBaseUrl() + "/html-fixtures/caspit-author-page-with-articles.html";
        
        List<String> articleLinks = navigator.getAllArticleLinks(testUrl);
        
        // Verify that articles were found
        assertFalse(articleLinks.isEmpty(), "Should find article links");
        
        // Verify expected article links are present (only valid ones)
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-1"), 
                  "Should contain article-1");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-2"), 
                  "Should contain article-2");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-3"), 
                  "Should contain article-3");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-4"), 
                  "Should contain article-4");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-5"), 
                  "Should contain article-5");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/article-6"), 
                  "Should contain article-6");
        
        // Verify invalid links are filtered out
        assertFalse(articleLinks.contains("https://www.maariv.co.il/category/politics"), 
                   "Should not contain category links");
        assertFalse(articleLinks.contains("https://www.maariv.co.il/tag/election"), 
                   "Should not contain tag links");
        
        // Verify we found the expected number of valid articles (6 valid out of 8 total)
        assertEquals(6, articleLinks.size(), "Should find exactly 6 valid article links");
        
        System.out.println("Found article links: " + articleLinks);
    }

    @Test
    @DisplayName("Should handle pagination and load more button functionality")
    void testGetAllArticleLinksWithPagination() {
        // Test against the pagination HTML page
        String testUrl = getTestBaseUrl() + "/html-fixtures/caspit-author-page-with-pagination.html";
        
        List<String> articleLinks = navigator.getAllArticleLinks(testUrl);
        
        // Verify that articles were found
        assertFalse(articleLinks.isEmpty(), "Should find article links from pagination page");
        
        // Verify expected article links from the pagination page
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/page2-article-1"), 
                  "Should contain page2-article-1");
        assertTrue(articleLinks.contains("https://www.maariv.co.il/Ben-Caspit/page2-article-2"), 
                  "Should contain page2-article-2");
        
        // The test page has JavaScript that simulates infinite scroll
        // The navigator should attempt to trigger it
        System.out.println("Found article links from pagination page: " + articleLinks);
    }

    @Test
    @DisplayName("Should handle WebDriver lifecycle management properly")
    void testWebDriverLifecycleManagement() {
        // Test multiple consecutive calls to ensure WebDriver is managed properly
        String testUrl = getTestBaseUrl() + "/html-fixtures/caspit-author-page-with-articles.html";
        
        // First call
        List<String> firstCall = navigator.getAllArticleLinks(testUrl);
        assertFalse(firstCall.isEmpty(), "First call should return results");
        
        // Second call - should work without issues
        List<String> secondCall = navigator.getAllArticleLinks(testUrl);
        assertFalse(secondCall.isEmpty(), "Second call should return results");
        
        // Results should be consistent
        assertEquals(firstCall.size(), secondCall.size(), 
                    "Multiple calls should return consistent results");
        
        System.out.println("WebDriver lifecycle test completed successfully");
    }

    @Test
    @DisplayName("Should handle error conditions gracefully")
    void testErrorHandling() {
        // Test with invalid URL
        String invalidUrl = getTestBaseUrl() + "/non-existent-page.html";
        
        List<String> result = navigator.getAllArticleLinks(invalidUrl);
        
        // Should return empty list, not throw exception
        assertNotNull(result, "Should return non-null result even for invalid URL");
        assertTrue(result.isEmpty(), "Should return empty list for invalid URL");
        
        System.out.println("Error handling test completed - returned empty list for invalid URL");
    }

    @Test
    @DisplayName("Should reinitialize WebDriver when requested")
    void testWebDriverReinitialization() {
        // Test the reinitializeWebDriver method
        String testUrl = getTestBaseUrl() + "/html-fixtures/caspit-author-page-with-articles.html";
        
        // Get initial results
        List<String> initialResults = navigator.getAllArticleLinks(testUrl);
        assertFalse(initialResults.isEmpty(), "Initial call should return results");
        
        // Reinitialize WebDriver
        assertDoesNotThrow(() -> navigator.reinitializeWebDriver(), 
                          "WebDriver reinitialization should not throw exception");
        
        // Test that navigator still works after reinitialization
        List<String> afterReinitResults = navigator.getAllArticleLinks(testUrl);
        assertFalse(afterReinitResults.isEmpty(), "Should return results after reinitialization");
        
        // Results should be consistent
        assertEquals(initialResults.size(), afterReinitResults.size(), 
                    "Results should be consistent after reinitialization");
        
        System.out.println("WebDriver reinitialization test completed successfully");
    }

    @Test
    @DisplayName("Should respect configuration settings")
    void testConfigurationRespect() {
        // Verify that the navigator respects configuration settings
        assertTrue(config.webdriver().headless(), "Should be configured for headless mode in tests");
        assertTrue(config.maxPages() > 0, "Should have positive max pages configuration");
        assertTrue(config.pageLoadTimeout() > 0, "Should have positive page load timeout");
        
        // Test with a URL and verify it respects max pages (indirectly)
        String testUrl = getTestBaseUrl() + "/html-fixtures/caspit-author-page-with-articles.html";
        List<String> results = navigator.getAllArticleLinks(testUrl);
        
        // Should complete without timing out (respecting timeout configuration)
        assertNotNull(results, "Should complete within configured timeout");
        
        System.out.println("Configuration respect test completed successfully");
    }

    @Test
    @DisplayName("Should handle empty pages gracefully")
    void testEmptyPageHandling() {
        // Use the REST endpoint for empty page
        String emptyPageUrl = getTestBaseUrl() + "/html-fixtures/empty-page.html";
        List<String> results = navigator.getAllArticleLinks(emptyPageUrl);
        
        // Should return empty list, not throw exception
        assertNotNull(results, "Should return non-null result for empty page");
        assertTrue(results.isEmpty(), "Should return empty list for page with no articles");
        
        System.out.println("Empty page handling test completed successfully");
    }



    /**
     * Test profile for integration tests
     */
    public static class IntegrationTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                // Use headless mode for CI/CD environments
                "caspit.crawler.webdriver.headless", "true",
                // Reduce timeouts for faster tests
                "caspit.crawler.page-load-timeout", "5000",
                "caspit.crawler.webdriver.implicit-wait", "1",
                "caspit.crawler.webdriver.element-wait", "3",
                "caspit.crawler.crawling.page-delay", "500",
                "caspit.crawler.crawling.scroll-delay", "1000",
                // Limit pages for testing
                "caspit.crawler.max-pages", "5"
            );
        }
    }
}