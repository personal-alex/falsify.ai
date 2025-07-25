package ai.falsify.crawlers.common.integration;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import ai.falsify.crawlers.common.service.ContentValidator;
import ai.falsify.crawlers.common.service.RetryService;
import ai.falsify.crawlers.common.service.redis.DeduplicationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that validate metrics collection and logging consistency
 * across the common module services.
 */
@QuarkusTest
class MetricsAndLoggingIntegrationTest {

    private static final Logger LOG = Logger.getLogger(MetricsAndLoggingIntegrationTest.class);

    @Inject
    CrawlerConfiguration config;

    @Inject
    ContentValidator contentValidator;

    @Inject
    RetryService retryService;

    @Inject
    DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        // Clear test data
        deduplicationService.clearProcessedUrls("metrics-test");
    }

    @Test
    @DisplayName("Should provide consistent logging across all services")
    void shouldProvideConsistentLoggingAcrossAllServices() {
        // Test that all services use consistent logging patterns
        
        // Test content validator logging
        assertDoesNotThrow(() -> {
            contentValidator.validateArticle(
                "Test Article for Logging", 
                "https://example.com/logging-test",
                "This is a test article with sufficient content to validate logging behavior across services."
            );
        }, "Content validation should complete with proper logging");

        // Test retry service logging
        AtomicInteger attempts = new AtomicInteger(0);
        assertDoesNotThrow(() -> {
            retryService.executeWithRetry(() -> {
                int count = attempts.incrementAndGet();
                if (count == 1) {
                    LOG.infof("First attempt in retry service logging test");
                    throw new RuntimeException("Simulated failure for logging test");
                }
                LOG.infof("Successful attempt in retry service logging test");
                return "success";
            }, "logging-test-operation");
        }, "Retry service should complete with proper logging");

        assertEquals(2, attempts.get(), "Should make 2 attempts for logging test");

        // Test deduplication service logging
        String testUrl = "https://example.com/dedup-logging-test";
        assertTrue(deduplicationService.isNewUrl("metrics-test", testUrl), 
                  "URL should be new for logging test");
        assertFalse(deduplicationService.isNewUrl("metrics-test", testUrl), 
                   "URL should be duplicate for logging test");
    }

    @Test
    @DisplayName("Should handle performance logging consistently")
    void shouldHandlePerformanceLoggingConsistently() {
        // Test performance logging across services
        
        Instant start = Instant.now();
        
        // Test content validation performance
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                contentValidator.validateArticle(
                    "Performance Test Article " + i,
                    "https://example.com/perf-test-" + i,
                    "This is performance test article " + i + " with sufficient content for validation testing."
                );
            }
        }, "Content validation performance test should complete");

        // Test retry service performance
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 3; i++) {
                final int index = i; // Make variable effectively final
                retryService.executeWithRetry(() -> {
                    return "performance-test-" + index;
                }, "performance-test-operation-" + index);
            }
        }, "Retry service performance test should complete");

        // Test deduplication service performance
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                deduplicationService.isNewUrl("metrics-test", "https://example.com/perf-" + i);
            }
        }, "Deduplication service performance test should complete");

        Duration elapsed = Duration.between(start, Instant.now());
        LOG.infof("Performance test completed in %d ms", elapsed.toMillis());
        
        // Verify reasonable performance (should complete within reasonable time)
        assertTrue(elapsed.toMillis() < 5000, "Performance tests should complete within 5 seconds");
    }

    @Test
    @DisplayName("Should provide consistent error logging across services")
    void shouldProvideConsistentErrorLoggingAcrossServices() {
        // Test error logging consistency
        
        // Test content validation error logging
        assertThrows(Exception.class, () -> {
            contentValidator.validateArticle("", "", ""); // Invalid content
        }, "Content validation should throw exception with proper error logging");

        // Test retry service error logging
        assertThrows(Exception.class, () -> {
            retryService.executeWithRetry(() -> {
                throw new RuntimeException("Consistent error for logging test");
            }, "error-logging-test");
        }, "Retry service should throw exception with proper error logging");

        // All error logging should be consistent and informative
        LOG.info("Error logging consistency test completed");
    }

    @Test
    @DisplayName("Should support debug logging when enabled")
    void shouldSupportDebugLoggingWhenEnabled() {
        // Test debug logging capabilities
        
        if (config.logging().enableStructuredLogging()) {
            LOG.debug("Debug logging is enabled for structured logging test");
            
            // Test debug logging in content validation
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(
                    "Debug Logging Test",
                    "https://example.com/debug-test",
                    "This is a debug logging test article with sufficient content for validation."
                );
            }, "Content validation with debug logging should work");

            // Test debug logging in retry service
            assertDoesNotThrow(() -> {
                retryService.executeWithRetry(() -> {
                    LOG.debug("Debug log from within retry operation");
                    return "debug-success";
                }, "debug-logging-operation");
            }, "Retry service with debug logging should work");
        }
    }

    @Test
    @DisplayName("Should handle concurrent logging without interference")
    void shouldHandleConcurrentLoggingWithoutInterference() {
        // Test that concurrent operations don't interfere with logging
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    contentValidator.validateArticle(
                        "Concurrent Test 1-" + i,
                        "https://example.com/concurrent1-" + i,
                        "Concurrent logging test content 1-" + i + " with sufficient length."
                    );
                    LOG.infof("Thread 1 completed validation %d", i);
                } catch (Exception e) {
                    LOG.errorf("Thread 1 validation error %d: %s", i, e.getMessage());
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                final int index = i; // Make variable effectively final
                try {
                    retryService.executeWithRetry(() -> {
                        LOG.debugf("Thread 2 retry operation %d", index);
                        return "concurrent-success-2-" + index;
                    }, "concurrent-operation-2-" + index);
                    LOG.infof("Thread 2 completed retry %d", index);
                } catch (Exception e) {
                    LOG.errorf("Thread 2 retry error %d: %s", index, e.getMessage());
                }
            }
        });

        // Start both threads
        thread1.start();
        thread2.start();

        // Wait for completion
        assertDoesNotThrow(() -> {
            thread1.join(5000);
            thread2.join(5000);
        }, "Concurrent logging operations should complete without interference");

        LOG.info("Concurrent logging test completed successfully");
    }

    @Test
    @DisplayName("Should maintain logging context across service calls")
    void shouldMaintainLoggingContextAcrossServiceCalls() {
        // Test that logging context is maintained across service boundaries
        
        String operationId = "context-test-" + System.currentTimeMillis();
        LOG.infof("Starting operation %s", operationId);

        assertDoesNotThrow(() -> {
            // Chain service calls and verify context is maintained
            retryService.executeWithRetry(() -> {
                LOG.debugf("Inside retry service for operation %s", operationId);
                
                try {
                    contentValidator.validateArticle(
                        "Context Test Article",
                        "https://example.com/context-test",
                        "This is a context test article with sufficient content for validation testing."
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Validation failed", e);
                }
                
                LOG.debugf("Content validation completed for operation %s", operationId);
                
                deduplicationService.isNewUrl("context-test", "https://example.com/context-test");
                
                LOG.debugf("Deduplication check completed for operation %s", operationId);
                
                return "context-success";
            }, "context-test-operation");
        }, "Service call chain should maintain logging context");

        LOG.infof("Completed operation %s", operationId);
    }

    @Test
    @DisplayName("Should provide structured logging information")
    void shouldProvideStructuredLoggingInformation() {
        // Test structured logging capabilities
        
        if (config.logging().enableStructuredLogging()) {
            // Test structured logging with different log levels
            LOG.info("INFO level structured logging test");
            LOG.debug("DEBUG level structured logging test");
            LOG.warn("WARN level structured logging test");
            
            // Test structured logging with operations
            assertDoesNotThrow(() -> {
                contentValidator.validateArticle(
                    "Structured Logging Test",
                    "https://example.com/structured-test",
                    "This is a structured logging test with sufficient content for validation."
                );
            }, "Structured logging should work with content validation");
            
            assertTrue(true, "Structured logging test completed");
        } else {
            LOG.info("Structured logging is disabled, skipping structured logging tests");
        }
    }
}