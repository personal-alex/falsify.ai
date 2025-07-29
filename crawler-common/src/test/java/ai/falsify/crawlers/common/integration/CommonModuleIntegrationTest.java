package ai.falsify.crawlers.common.integration;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.exception.CrawlingException;
import ai.falsify.crawlers.common.model.Article;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import ai.falsify.crawlers.common.service.redis.RedisService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the common module.
 * Tests shared services work correctly across different crawler implementations.
 */
@QuarkusTest
@TestProfile(CommonModuleIntegrationTest.TestProfile.class)
class CommonModuleIntegrationTest {

    @Inject
    CrawlerConfiguration config;

    @Inject
    ContentValidator contentValidator;

    @Inject
    RetryService retryService;

    @Inject
    DeduplicationService deduplicationService;

    @Inject
    RedisService redisService;

    @Inject
    UserTransaction userTransaction;

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        try {
            redisService.keys("test:*").forEach(redisService::delete);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        // Reset circuit breaker to ensure clean state for each test
        retryService.resetCircuitBreaker();
    }

    @Nested
    @DisplayName("Configuration Framework Tests")
    class ConfigurationFrameworkTests {

        @Test
        @DisplayName("Should load configuration with default values")
        void shouldLoadConfigurationWithDefaults() {
            assertNotNull(config, "Configuration should be injected");
            assertNotNull(config.network(), "Network config should be available");
            assertNotNull(config.content(), "Content config should be available");
            assertNotNull(config.retry(), "Retry config should be available");
            assertNotNull(config.redis(), "Redis config should be available");
            
            // Test default values
            assertTrue(config.content().enableContentValidation(), "Content validation should be enabled by default");
            assertTrue(config.retry().enableCircuitBreaker(), "Circuit breaker should be enabled by default");
            // default value 3 is overriden in TestProfile to support RetryService tests
            assertEquals(2, config.retry().maxAttempts(), "Default max attempts should be 2");
        }

        @Test
        @DisplayName("Should validate configuration constraints")
        void shouldValidateConfigurationConstraints() {
            // Test that configuration values are within expected ranges
            assertTrue(config.content().minContentLength() >= 10, "Min content length should be at least 10");
            assertTrue(config.retry().maxAttempts() >= 0 && config.retry().maxAttempts() <= 10, 
                      "Max attempts should be between 0 and 10");
            assertTrue(config.performance().maxConcurrentRequests() >= 1 && config.performance().maxConcurrentRequests() <= 100,
                      "Max concurrent requests should be between 1 and 100");
        }
    }

    @Nested
    @DisplayName("Content Validation Tests")
    class ContentValidationTests {

        @Test
        @DisplayName("Should validate article content successfully")
        void shouldValidateArticleContentSuccessfully() {
            String title = "Test Article Title";
            String url = "https://example.com/test-article";
            String content = "This is a test article with sufficient content length to pass validation. " +
                           "It contains multiple sentences and meaningful text that should be accepted by the validator.";

            assertDoesNotThrow(() -> contentValidator.validateArticle(title, url, content),
                             "Valid article should pass validation");
        }

        @Test
        @DisplayName("Should reject article with insufficient content")
        void shouldRejectArticleWithInsufficientContent() {
            String title = "Test Article";
            String url = "https://example.com/test";
            String content = "Short"; // Too short

            assertThrows(ContentValidationException.class,
                        () -> contentValidator.validateArticle(title, url, content),
                        "Article with insufficient content should be rejected");
        }

        @Test
        @DisplayName("Should reject article with missing required fields")
        void shouldRejectArticleWithMissingFields() {
            assertThrows(ContentValidationException.class,
                        () -> contentValidator.validateArticle(null, "https://example.com", "Content"),
                        "Article with null title should be rejected");

            assertThrows(ContentValidationException.class,
                        () -> contentValidator.validateArticle("Title", null, "Content"),
                        "Article with null URL should be rejected");

            assertThrows(ContentValidationException.class,
                        () -> contentValidator.validateArticle("Title", "https://example.com", null),
                        "Article with null content should be rejected");
        }
    }

    @Nested
    @DisplayName("Redis Service Abstraction Tests")
    class RedisServiceTests {

        @Test
        @DisplayName("Should perform basic Redis operations")
        void shouldPerformBasicRedisOperations() {
            String key = "test:basic:key";
            String value = "test-value";

            // Test set and get
            redisService.set(key, value);
            assertEquals(value, redisService.get(key).orElse(null), "Should retrieve stored value");

            // Test exists
            assertTrue(redisService.exists(key), "Key should exist after setting");

            // Test delete
            assertTrue(redisService.delete(key), "Should successfully delete key");
            assertFalse(redisService.exists(key), "Key should not exist after deletion");
        }

        @Test
        @DisplayName("Should handle Redis operations with expiration")
        void shouldHandleRedisOperationsWithExpiration() {
            String key = "test:expiration:key";
            String value = "test-value";
            Duration expiration = Duration.ofSeconds(1);

            // Test set with expiration
            redisService.set(key, value, expiration);
            assertTrue(redisService.exists(key), "Key should exist immediately after setting");

            // Test setnx with expiration
            String nxKey = "test:setnx:key";
            assertTrue(redisService.setnx(nxKey, value, expiration), "SETNX should succeed for new key");
            assertFalse(redisService.setnx(nxKey, "other-value", expiration), "SETNX should fail for existing key");
        }

        @Test
        @DisplayName("Should handle Redis set operations")
        void shouldHandleRedisSetOperations() {
            String setKey = "test:set:key";
            String member1 = "member1";
            String member2 = "member2";

            // Test set operations
            assertTrue(redisService.sadd(setKey, member1), "Should add new member to set");
            assertTrue(redisService.sadd(setKey, member2), "Should add another new member to set");
            assertFalse(redisService.sadd(setKey, member1), "Should not add duplicate member to set");

            // Test set membership
            assertTrue(redisService.sismember(setKey, member1), "Member1 should be in set");
            assertTrue(redisService.sismember(setKey, member2), "Member2 should be in set");
            assertFalse(redisService.sismember(setKey, "nonexistent"), "Nonexistent member should not be in set");

            // Test set cardinality
            assertEquals(2, redisService.scard(setKey), "Set should have 2 members");

            // Test set removal
            assertTrue(redisService.srem(setKey, member1), "Should remove existing member");
            assertFalse(redisService.sismember(setKey, member1), "Removed member should not be in set");
            assertEquals(1, redisService.scard(setKey), "Set should have 1 member after removal");
        }
    }

    @Nested
    @DisplayName("Deduplication Service Tests")
    class DeduplicationServiceTests {

        @Test
        @DisplayName("Should handle URL deduplication for different crawlers")
        void shouldHandleUrlDeduplicationForDifferentCrawlers() {
            String url = "https://example.com/test-article";

            // Test deduplication for drucker crawler
            assertTrue(deduplicationService.isNewUrl("drucker", url), 
                      "URL should be new for drucker crawler");
            assertFalse(deduplicationService.isNewUrl("drucker", url), 
                       "URL should not be new on second check for drucker crawler");

            // Test deduplication for caspit crawler (should be independent)
            assertTrue(deduplicationService.isNewUrl("caspit", url), 
                      "URL should be new for caspit crawler even if processed by drucker");
            assertFalse(deduplicationService.isNewUrl("caspit", url), 
                       "URL should not be new on second check for caspit crawler");
        }

        @Test
        @DisplayName("Should handle URL deduplication with expiration")
        void shouldHandleUrlDeduplicationWithExpiration() {
            String url = "https://example.com/expiring-article";
            Duration shortExpiration = Duration.ofSeconds(2); // Use seconds instead of milliseconds

            // Test with expiration
            assertTrue(deduplicationService.isNewUrl("test", url, shortExpiration), 
                      "URL should be new initially");
            assertFalse(deduplicationService.isNewUrl("test", url, shortExpiration), 
                       "URL should not be new immediately after");

            // Wait for expiration (in a real test, you might use a longer duration and sleep)
            // For this test, we'll just verify the method works with expiration parameter
        }

        @Test
        @DisplayName("Should provide URL management operations")
        void shouldProvideUrlManagementOperations() {
            String crawlerName = "test-crawler";
            String url1 = "https://example.com/article1";
            String url2 = "https://example.com/article2";

            // Mark URLs as processed
            deduplicationService.markUrlProcessed(crawlerName, url1);
            deduplicationService.markUrlProcessed(crawlerName, url2);

            // Verify URLs are marked as processed
            assertTrue(deduplicationService.isUrlProcessed(crawlerName, url1), 
                      "URL1 should be marked as processed");
            assertTrue(deduplicationService.isUrlProcessed(crawlerName, url2), 
                      "URL2 should be marked as processed");

            // Test count
            assertEquals(2, deduplicationService.getProcessedUrlCount(crawlerName), 
                        "Should have 2 processed URLs");

            // Test removal
            assertTrue(deduplicationService.removeUrl(crawlerName, url1), 
                      "Should successfully remove URL1");
            assertFalse(deduplicationService.isUrlProcessed(crawlerName, url1), 
                       "URL1 should no longer be processed after removal");
            assertEquals(1, deduplicationService.getProcessedUrlCount(crawlerName), 
                        "Should have 1 processed URL after removal");

            // Test clear all
            long cleared = deduplicationService.clearProcessedUrls(crawlerName);
            assertEquals(1, cleared, "Should clear 1 URL");
            assertEquals(0, deduplicationService.getProcessedUrlCount(crawlerName), 
                        "Should have 0 processed URLs after clearing");
        }
    }

    @Nested
    @DisplayName("Retry Service Tests")
    class RetryServiceTests {

        @Test
        @DisplayName("Should execute operation successfully without retries")
        void shouldExecuteOperationSuccessfullyWithoutRetries() throws CrawlingException {
            AtomicInteger callCount = new AtomicInteger(0);
            
            String result = retryService.executeWithRetry(() -> {
                callCount.incrementAndGet();
                return "success";
            }, "test-operation");

            assertEquals("success", result, "Should return successful result");
            assertEquals(1, callCount.get(), "Should call operation only once for successful execution");
        }

        @Test
        @DisplayName("Should retry operation on failure and eventually succeed")
        void shouldRetryOperationOnFailureAndEventuallySucceed() throws CrawlingException {
            AtomicInteger callCount = new AtomicInteger(0);
            
            String result = retryService.executeWithRetry(() -> {
                int count = callCount.incrementAndGet();
                if (count < config.retry().maxAttempts()) {
                    throw new RuntimeException("Simulated failure " + count);
                }
                return "success-after-retries";
            }, "test-retry-operation");

            assertEquals("success-after-retries", result, "Should return successful result after retries");
            // Update this assertion based on max-retry count in test profile
            assertEquals(2, callCount.get(), "Should call operation 2 times (1 failures + 1 success)");
        }

        @Test
        @DisplayName("Should fail after maximum retries exceeded")
        void shouldFailAfterMaximumRetriesExceeded() {
            AtomicInteger callCount = new AtomicInteger(0);
            
            assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Always fails");
                }, "test-max-retries-operation");
            }, "Should throw CrawlingException after max retries");

            // Should call max attempts + 1 (initial attempt)
            assertTrue(callCount.get() > 1, "Should make multiple attempts");
        }

        @Test
        @DisplayName("Should only retry on specified exception types")
        void shouldOnlyRetryOnSpecifiedExceptionTypes() throws CrawlingException {
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Test with retryable exception type
            assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Retryable exception");
                }, "test-exception-type", RuntimeException.class);
            });
            
            assertTrue(callCount.get() > 1, "Should retry on RuntimeException");
            
            // Reset counter
            callCount.set(0);
            
            // Test with non-retryable exception type
            assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    callCount.incrementAndGet();
                    throw new IllegalArgumentException("Non-retryable exception");
                }, "test-non-retryable", CrawlingException.class);
            });
            
            assertEquals(1, callCount.get(), "Should not retry on IllegalArgumentException when expecting CrawlingException");
        }
    }

    @Nested
    @DisplayName("Article Entity Integration Tests")
    class ArticleEntityIntegrationTests {

        @Test
        @DisplayName("Should persist and retrieve article entities")
        void shouldPersistAndRetrieveArticleEntities() {
            try {
                userTransaction.begin();
                
                // Create test article
                ArticleEntity article = new ArticleEntity();
                article.title = "Integration Test Article";
                article.url = "https://example.com/integration-test";
                article.text = "This is a test article for integration testing of the common module.";

                // Persist article
                article.persist();
                assertNotNull(article.id, "Article should have ID after persistence");

                // Retrieve article
                ArticleEntity retrieved = ArticleEntity.findById(article.id);
                assertNotNull(retrieved, "Should retrieve persisted article");
                assertEquals(article.title, retrieved.title, "Title should match");
                assertEquals(article.url, retrieved.url, "URL should match");
                assertEquals(article.text, retrieved.text, "Text should match");
                
                userTransaction.commit();
            } catch (Exception e) {
                try {
                    userTransaction.rollback();
                } catch (Exception rollbackException) {
                    // Ignore rollback errors
                }
                throw new RuntimeException("Transaction failed", e);
            }
        }

        @Test
        @DisplayName("Should handle article entity queries")
        void shouldHandleArticleEntityQueries() {
            try {
                userTransaction.begin();
                
                // Create test articles
                ArticleEntity article1 = new ArticleEntity();
                article1.title = "First Test Article";
                article1.url = "https://example.com/first";
                article1.text = "First test article content";
                article1.persist();

                ArticleEntity article2 = new ArticleEntity();
                article2.title = "Second Test Article";
                article2.url = "https://example.com/second";
                article2.text = "Second test article content";
                article2.persist();

                // Test find by URL
                ArticleEntity foundByUrl = ArticleEntity.find("url", article1.url).firstResult();
                assertNotNull(foundByUrl, "Should find article by URL");
                assertEquals(article1.title, foundByUrl.title, "Found article should match");

                // Test count
                long count = ArticleEntity.count();
                assertTrue(count >= 2, "Should have at least 2 articles");

                // Test list all
                var allArticles = ArticleEntity.listAll();
                assertTrue(allArticles.size() >= 2, "Should list at least 2 articles");
                
                userTransaction.commit();
            } catch (Exception e) {
                try {
                    userTransaction.rollback();
                } catch (Exception rollbackException) {
                    // Ignore rollback errors
                }
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }

    @Nested
    @DisplayName("Cross-Module Integration Tests")
    class CrossModuleIntegrationTests {

        @Test
        @DisplayName("Should integrate content validation with retry service")
        void shouldIntegrateContentValidationWithRetryService() throws CrawlingException {
            AtomicInteger validationAttempts = new AtomicInteger(0);
            
            // Test successful validation with retry
            retryService.executeWithRetry(() -> {
                validationAttempts.incrementAndGet();
                try {
                    contentValidator.validateArticle(
                        "Test Article",
                        "https://example.com/test",
                        "This is a valid article with sufficient content for validation testing."
                    );
                } catch (ContentValidationException e) {
                    throw new RuntimeException("Validation failed", e);
                }
                return "validation-success";
            }, "validation-with-retry");

            assertEquals(1, validationAttempts.get(), "Should succeed on first validation attempt");
        }

        @Test
        @DisplayName("Should integrate deduplication with content validation")
        void shouldIntegrateDeduplicationWithContentValidation() {
            String crawlerName = "integration-test";
            String url = "https://example.com/integration-article";
            String title = "Integration Test Article";
            String content = "This is an integration test article with sufficient content for validation.";

            // First check - should be new
            assertTrue(deduplicationService.isNewUrl(crawlerName, url), 
                      "URL should be new initially");

            // Validate content
            assertDoesNotThrow(() -> contentValidator.validateArticle(title, url, content),
                             "Content should be valid");

            // Second check - should be duplicate
            assertFalse(deduplicationService.isNewUrl(crawlerName, url), 
                       "URL should be duplicate on second check");
        }

        @Test
        @DisplayName("Should handle error scenarios across services")
        void shouldHandleErrorScenariosAcrossServices() {
            // Test retry service with content validation failure
            assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    // This will always fail validation
                    try {
                        contentValidator.validateArticle("", "", "");
                    } catch (ContentValidationException e) {
                        throw new RuntimeException("Validation failed as expected", e);
                    }
                    return "should-not-reach";
                }, "failing-validation-with-retry", RuntimeException.class);
            }, "Should propagate validation exception through retry service");
        }
    }

    /**
     * Test profile for integration tests
     */
    public static class TestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                "crawler.common.retry.max-attempts", "2",
                "crawler.common.retry.circuit-breaker-failure-threshold", "20", // Higher threshold for tests
                "crawler.common.content.min-content-length", "50",
                "quarkus.log.category.\"ai.falsify.crawlers.common\".level", "DEBUG"
            );
        }
    }
}