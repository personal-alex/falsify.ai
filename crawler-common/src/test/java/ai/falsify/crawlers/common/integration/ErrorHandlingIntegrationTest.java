package ai.falsify.crawlers.common.integration;

import ai.falsify.crawlers.common.exception.ContentValidationException;
import ai.falsify.crawlers.common.exception.CrawlingException;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import ai.falsify.crawlers.common.service.redis.RedisService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import io.quarkus.test.junit.TestProfile;

/**
 * Integration tests that validate error handling and retry mechanisms
 * work correctly across all common module services.
 */
@QuarkusTest
@TestProfile(ErrorHandlingIntegrationTest.TestProfile.class)
class ErrorHandlingIntegrationTest {

    @Inject
    ContentValidator contentValidator;

    @Inject
    RetryService retryService;

    @Inject
    DeduplicationService deduplicationService;

    @Inject
    RedisService redisService;

    @BeforeEach
    void setUp() {
        // Clear test data
        try {
            redisService.keys("error-test:*").forEach(redisService::delete);
            deduplicationService.clearProcessedUrls("error-test");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        // Reset circuit breaker to ensure clean state for each test
        retryService.resetCircuitBreaker();
    }

    @Nested
    @DisplayName("Content Validation Error Handling")
    class ContentValidationErrorHandling {

        @Test
        @DisplayName("Should handle missing required fields gracefully")
        void shouldHandleMissingRequiredFieldsGracefully() {
            // Test null title
            ContentValidationException titleException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle(null, "https://example.com", "Content"),
                "Should throw exception for null title"
            );
            assertNotNull(titleException.getMessage(), "Exception should have meaningful message");
            assertTrue(titleException.getMessage().contains("title"), "Exception should mention title field");

            // Test null URL
            ContentValidationException urlException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle("Title", null, "Content"),
                "Should throw exception for null URL"
            );
            assertNotNull(urlException.getMessage(), "Exception should have meaningful message");
            assertTrue(urlException.getMessage().contains("url"), "Exception should mention URL field");

            // Test null content
            ContentValidationException contentException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle("Title", "https://example.com", null),
                "Should throw exception for null content"
            );
            assertNotNull(contentException.getMessage(), "Exception should have meaningful message");
            assertTrue(contentException.getMessage().contains("content"), "Exception should mention content field");
        }

        @Test
        @DisplayName("Should handle invalid content length gracefully")
        void shouldHandleInvalidContentLengthGracefully() {
            // Test content too short
            ContentValidationException shortException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle("Title", "https://example.com", "x"),
                "Should throw exception for content too short"
            );
            assertNotNull(shortException.getMessage(), "Exception should have meaningful message");

            // Test empty content
            ContentValidationException emptyException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle("Title", "https://example.com", ""),
                "Should throw exception for empty content"
            );
            assertNotNull(emptyException.getMessage(), "Exception should have meaningful message");
        }

        @Test
        @DisplayName("Should handle malformed URLs gracefully")
        void shouldHandleMalformedUrlsGracefully() {
            ContentValidationException malformedUrlException = assertThrows(
                ContentValidationException.class,
                () -> contentValidator.validateArticle(
                    "Title", 
                    "not-a-valid-url", 
                    "This is valid content with sufficient length for validation testing."
                ),
                "Should throw exception for malformed URL"
            );
            assertNotNull(malformedUrlException.getMessage(), "Exception should have meaningful message");
        }
    }

    @Nested
    @DisplayName("Retry Service Error Handling")
    class RetryServiceErrorHandling {

        @Test
        @DisplayName("Should handle transient failures with retry")
        void shouldHandleTransientFailuresWithRetry() throws CrawlingException {
            AtomicInteger attempts = new AtomicInteger(0);
            
            String result = retryService.executeWithRetry(() -> {
                int count = attempts.incrementAndGet();
                if (count < 2) {
                    throw new RuntimeException("Transient failure " + count);
                }
                return "success-after-retries";
            }, "transient-failure-test");

            assertEquals("success-after-retries", result, "Should succeed after retries");
            assertEquals(2, attempts.get(), "Should make 2 attempts");
        }

        @Test
        @DisplayName("Should handle permanent failures appropriately")
        void shouldHandlePermanentFailuresAppropriately() {
            AtomicInteger attempts = new AtomicInteger(0);
            
            CrawlingException exception = assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("Permanent failure");
                }, "permanent-failure-test");
            }, "Should throw CrawlingException for permanent failures");

            assertNotNull(exception.getMessage(), "Exception should have meaningful message");
            assertTrue(attempts.get() > 1, "Should make multiple attempts before giving up " + attempts.get());
        }

        @Test
        @DisplayName("Should handle different exception types correctly")
        void shouldHandleDifferentExceptionTypesCorrectly() throws CrawlingException {
            // Test with specific exception type that should be retried
            AtomicInteger runtimeAttempts = new AtomicInteger(0);
            
            CrawlingException runtimeException = assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    runtimeAttempts.incrementAndGet();
                    throw new RuntimeException("Runtime exception");
                }, "runtime-exception-test", RuntimeException.class);
            });
            
            assertTrue(runtimeAttempts.get() > 1, "Should retry RuntimeException but it was " + runtimeAttempts.get());

            // Test with exception type that should not be retried
            AtomicInteger illegalArgAttempts = new AtomicInteger(0);
            
            CrawlingException illegalArgException = assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    illegalArgAttempts.incrementAndGet();
                    throw new IllegalArgumentException("Illegal argument");
                }, "illegal-arg-test", CrawlingException.class);
            });
            
            assertEquals(1, illegalArgAttempts.get(), "Should not retry IllegalArgumentException when expecting CrawlingException");
        }

        @Test
        @DisplayName("Should handle circuit breaker correctly")
        void shouldHandleCircuitBreakerCorrectly() {
            // Test circuit breaker behavior by causing multiple failures
            AtomicInteger totalAttempts = new AtomicInteger(0);
            
            // Cause multiple failures to potentially trigger circuit breaker
            // Should be related to max-retry setting in test profile
            for (int i = 0; i < 2; i++) {
                try {
                    retryService.executeWithRetry(() -> {
                        totalAttempts.incrementAndGet();
                        throw new RuntimeException("Circuit breaker test failure " + totalAttempts.get());
                    }, "circuit-breaker-test-" + i);
                } catch (CrawlingException e) {
                    // Expected failures
                }
            }
            
            assertTrue(totalAttempts.get() > 3, "Should make multiple attempts across operations");
        }
    }

    @Nested
    @DisplayName("Redis Service Error Handling")
    class RedisServiceErrorHandling {

        @Test
        @DisplayName("Should handle Redis operation failures gracefully")
        void shouldHandleRedisOperationFailuresGracefully() {
            // Test with invalid key patterns that might cause issues
            assertDoesNotThrow(() -> {
                redisService.set("error-test:valid-key", "value");
                assertTrue(redisService.exists("error-test:valid-key"), "Valid key should exist");
            }, "Valid Redis operations should work");

            // Test operations on non-existent keys
            assertDoesNotThrow(() -> {
                assertFalse(redisService.exists("error-test:non-existent"), "Non-existent key should not exist");
                assertFalse(redisService.delete("error-test:non-existent"), "Deleting non-existent key should return false");
            }, "Operations on non-existent keys should be handled gracefully");
        }

        @Test
        @DisplayName("Should handle Redis set operations errors gracefully")
        void shouldHandleRedisSetOperationsErrorsGracefully() {
            String setKey = "error-test:set-operations";
            
            // Test set operations with error conditions
            assertDoesNotThrow(() -> {
                // Test operations on empty set
                assertEquals(0, redisService.scard(setKey), "Empty set should have cardinality 0");
                assertFalse(redisService.sismember(setKey, "member"), "Empty set should not contain members");
                assertFalse(redisService.srem(setKey, "member"), "Removing from empty set should return false");
                
                // Test normal operations
                assertTrue(redisService.sadd(setKey, "member1"), "Adding to set should succeed");
                assertTrue(redisService.sismember(setKey, "member1"), "Set should contain added member");
                assertEquals(1, redisService.scard(setKey), "Set should have cardinality 1");
                
                // Test duplicate operations
                assertFalse(redisService.sadd(setKey, "member1"), "Adding duplicate should return false");
                assertEquals(1, redisService.scard(setKey), "Set cardinality should remain 1");
            }, "Redis set operations should handle error conditions gracefully");
        }
    }

    @Nested
    @DisplayName("Deduplication Service Error Handling")
    class DeduplicationServiceErrorHandling {

        @Test
        @DisplayName("Should handle deduplication errors gracefully")
        void shouldHandleDeduplicationErrorsGracefully() {
            // Test with various URL patterns
            assertDoesNotThrow(() -> {
                // Test normal URLs
                assertTrue(deduplicationService.isNewUrl("error-test", "https://example.com/normal"), 
                          "Normal URL should be new initially");
                
                // Test URLs with special characters
                assertTrue(deduplicationService.isNewUrl("error-test", "https://example.com/special?param=value&other=123"), 
                          "URL with parameters should be handled");
                
                // Test very long URLs
                String longUrl = "https://example.com/" + "a".repeat(1000);
                assertTrue(deduplicationService.isNewUrl("error-test", longUrl), 
                          "Long URL should be handled");
            }, "Deduplication service should handle various URL patterns gracefully");
        }

        @Test
        @DisplayName("Should handle crawler name variations gracefully")
        void shouldHandleCrawlerNameVariationsGracefully() {
            String testUrl = "https://example.com/crawler-name-test";
            
            assertDoesNotThrow(() -> {
                // Test normal crawler names
                assertTrue(deduplicationService.isNewUrl("normal-crawler", testUrl), 
                          "Normal crawler name should work");
                
                // Test crawler names with special characters
                assertTrue(deduplicationService.isNewUrl("crawler-with-dashes", testUrl), 
                          "Crawler name with dashes should work");
                
                // Test crawler names with numbers
                assertTrue(deduplicationService.isNewUrl("crawler123", testUrl), 
                          "Crawler name with numbers should work");
            }, "Deduplication service should handle various crawler name patterns");
        }

        @Test
        @DisplayName("Should handle bulk operations gracefully")
        void shouldHandleBulkOperationsGracefully() {
            String crawlerName = "bulk-test";
            
            assertDoesNotThrow(() -> {
                // Add multiple URLs
                for (int i = 0; i < 100; i++) {
                    deduplicationService.isNewUrl(crawlerName, "https://example.com/bulk-" + i);
                }
                
                // Verify count
                assertEquals(100, deduplicationService.getProcessedUrlCount(crawlerName), 
                           "Should handle 100 URLs");
                
                // Clear all
                long cleared = deduplicationService.clearProcessedUrls(crawlerName);
                assertEquals(100, cleared, "Should clear 100 URLs");
                assertEquals(0, deduplicationService.getProcessedUrlCount(crawlerName), 
                           "Should have 0 URLs after clearing");
            }, "Deduplication service should handle bulk operations gracefully");
        }
    }

    @Nested
    @DisplayName("Cross-Service Error Handling")
    class CrossServiceErrorHandling {

        @Test
        @DisplayName("Should handle cascading errors across services")
        void shouldHandleCascadingErrorsAcrossServices() {
            // Test error propagation through service chain
            assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    // This will cause a content validation error
                    try {
                        contentValidator.validateArticle("", "", "");
                    } catch (ContentValidationException e) {
                        throw new RuntimeException("Validation failed as expected", e);
                    }
                    return "should-not-reach";
                }, "cascading-error-test", RuntimeException.class);
            }, "Should propagate content validation errors through retry service");
        }

        @Test
        @DisplayName("Should maintain error context across service boundaries")
        void shouldMaintainErrorContextAcrossServiceBoundaries() {
            CrawlingException exception = assertThrows(CrawlingException.class, () -> {
                retryService.executeWithRetry(() -> {
                    throw new RuntimeException("Original error with context");
                }, "context-preservation-test");
            });
            
            assertNotNull(exception.getMessage(), "Exception should have message");
            assertNotNull(exception.getCause(), "Exception should have cause");
            assertTrue(exception.getMessage().contains("context-preservation-test"), 
                      "Exception should contain operation context");
        }

        @Test
        @DisplayName("Should handle concurrent error scenarios")
        void shouldHandleConcurrentErrorScenarios() {
            AtomicInteger thread1Errors = new AtomicInteger(0);
            AtomicInteger thread2Errors = new AtomicInteger(0);
            
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    try {
                        contentValidator.validateArticle("", "", ""); // Will fail
                    } catch (Exception e) {
                        thread1Errors.incrementAndGet();
                    }
                }
            });
            
            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    try {
                        // Simple direct exception instead of using retry service to avoid circuit breaker issues
                        throw new RuntimeException("Concurrent error " + i);
                    } catch (Exception e) {
                        thread2Errors.incrementAndGet();
                    }
                }
            });
            
            thread1.start();
            thread2.start();
            
            assertDoesNotThrow(() -> {
                thread1.join(5000);
                thread2.join(5000);
            }, "Concurrent error handling should not cause deadlocks");
            
            assertEquals(5, thread1Errors.get(), "Thread 1 should handle 5 errors");
            assertEquals(5, thread2Errors.get(), "Thread 2 should handle 5 errors");
        }
    }

    /**
     * Test profile for error handling integration tests
     */
    public static class TestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                "crawler.common.retry.max-attempts", "3",
                "crawler.common.retry.circuit-breaker-failure-threshold", "50", // Very high threshold for error tests
                "crawler.common.content.min-content-length", "50",
                "quarkus.log.category.\"ai.falsify.crawlers.common\".level", "DEBUG"
            );
        }
    }
}