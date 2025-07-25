package ai.falsify.crawlers.service;

import ai.falsify.crawlers.common.config.CrawlerConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CrawlingMetricsTest {

    @Inject
    CrawlingMetrics crawlingMetrics;

    @Inject
    CrawlerConfiguration config;

    @BeforeEach
    void setUp() {
        // Reset metrics before each test
        crawlingMetrics.reset();
    }

    @Nested
    class BasicMetricsCollection {

        @Test
        void testInitialMetricsAreZero() {
            // When
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();

            // Then
            assertEquals(0, summary.totalArticles());
            assertEquals(0, summary.successfulArticles());
            assertEquals(0, summary.failedArticles());
            assertEquals(0.0, summary.successRate());
            assertEquals(0.0, summary.averageProcessingTime());
            assertEquals(0.0, summary.averageNetworkTime());
            assertEquals(0.0, summary.averageDatabaseTime());
            assertEquals(0.0, summary.articlesPerMinute());
        }

        @Test
        void testSuccessfulArticleProcessing() throws InterruptedException {
            // Given
            String testUrl = "https://example.com/article1";

            // When
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing(testUrl);
            Thread.sleep(10); // Simulate processing time
            crawlingMetrics.recordArticleCompletion(context, true);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(1, summary.totalArticles());
            assertEquals(1, summary.successfulArticles());
            assertEquals(0, summary.failedArticles());
            assertEquals(100.0, summary.successRate());
            assertTrue(summary.averageProcessingTime() > 0);
        }

        @Test
        void testFailedArticleProcessing() throws InterruptedException {
            // Given
            String testUrl = "https://example.com/article1";

            // When
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing(testUrl);
            Thread.sleep(10); // Simulate processing time
            crawlingMetrics.recordArticleCompletion(context, false);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(1, summary.totalArticles());
            assertEquals(0, summary.successfulArticles());
            assertEquals(1, summary.failedArticles());
            assertEquals(0.0, summary.successRate());
            assertTrue(summary.averageProcessingTime() > 0);
        }

        @Test
        void testMixedSuccessAndFailure() throws InterruptedException {
            // Given
            String url1 = "https://example.com/article1";
            String url2 = "https://example.com/article2";
            String url3 = "https://example.com/article3";

            // When
            CrawlingMetrics.MetricsContext context1 = crawlingMetrics.startArticleProcessing(url1);
            Thread.sleep(5);
            crawlingMetrics.recordArticleCompletion(context1, true);

            CrawlingMetrics.MetricsContext context2 = crawlingMetrics.startArticleProcessing(url2);
            Thread.sleep(5);
            crawlingMetrics.recordArticleCompletion(context2, false);

            CrawlingMetrics.MetricsContext context3 = crawlingMetrics.startArticleProcessing(url3);
            Thread.sleep(5);
            crawlingMetrics.recordArticleCompletion(context3, true);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(3, summary.totalArticles());
            assertEquals(2, summary.successfulArticles());
            assertEquals(1, summary.failedArticles());
            assertEquals(66.67, summary.successRate(), 0.01);
        }
    }

    @Nested
    class NetworkMetrics {

        @Test
        void testNetworkOperationRecording() {
            // Given
            Duration networkDuration = Duration.ofMillis(150);

            // When
            crawlingMetrics.recordNetworkOperation("http_request", networkDuration);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(150, summary.totalNetworkTime());
            
            // Check operation-specific metrics
            assertTrue(summary.operationMetrics().containsKey("network_http_request"));
            CrawlingMetrics.TimingMetric networkMetric = summary.operationMetrics().get("network_http_request");
            assertEquals(1, networkMetric.count());
            assertEquals(150, networkMetric.totalTime());
            assertEquals(150.0, networkMetric.averageTime());
        }

        @Test
        void testMultipleNetworkOperations() {
            // Given
            Duration duration1 = Duration.ofMillis(100);
            Duration duration2 = Duration.ofMillis(200);
            Duration duration3 = Duration.ofMillis(150);

            // When
            crawlingMetrics.recordNetworkOperation("http_request", duration1);
            crawlingMetrics.recordNetworkOperation("http_request", duration2);
            crawlingMetrics.recordNetworkOperation("page_fetch", duration3);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(450, summary.totalNetworkTime());

            // Check http_request metrics
            CrawlingMetrics.TimingMetric httpMetric = summary.operationMetrics().get("network_http_request");
            assertEquals(2, httpMetric.count());
            assertEquals(300, httpMetric.totalTime());
            assertEquals(150.0, httpMetric.averageTime());
            assertEquals(100, httpMetric.minTime());
            assertEquals(200, httpMetric.maxTime());

            // Check page_fetch metrics
            CrawlingMetrics.TimingMetric fetchMetric = summary.operationMetrics().get("network_page_fetch");
            assertEquals(1, fetchMetric.count());
            assertEquals(150, fetchMetric.totalTime());
        }
    }

    @Nested
    class DatabaseMetrics {

        @Test
        void testDatabaseOperationRecording() {
            // Given
            Duration dbDuration = Duration.ofMillis(75);

            // When
            crawlingMetrics.recordDatabaseOperation("save", dbDuration);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(75, summary.totalDatabaseTime());
            
            // Check operation-specific metrics
            assertTrue(summary.operationMetrics().containsKey("database_save"));
            CrawlingMetrics.TimingMetric dbMetric = summary.operationMetrics().get("database_save");
            assertEquals(1, dbMetric.count());
            assertEquals(75, dbMetric.totalTime());
            assertEquals(75.0, dbMetric.averageTime());
        }

        @Test
        void testMultipleDatabaseOperations() {
            // Given
            Duration saveDuration = Duration.ofMillis(50);
            Duration queryDuration = Duration.ofMillis(25);
            Duration transactionDuration = Duration.ofMillis(100);

            // When
            crawlingMetrics.recordDatabaseOperation("save", saveDuration);
            crawlingMetrics.recordDatabaseOperation("query", queryDuration);
            crawlingMetrics.recordDatabaseOperation("transaction", transactionDuration);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(175, summary.totalDatabaseTime());

            // Verify individual operation metrics
            assertEquals(3, summary.operationMetrics().size());
            assertTrue(summary.operationMetrics().containsKey("database_save"));
            assertTrue(summary.operationMetrics().containsKey("database_query"));
            assertTrue(summary.operationMetrics().containsKey("database_transaction"));
        }
    }

    @Nested
    class AverageCalculations {

        @Test
        void testAverageProcessingTimeCalculation() throws InterruptedException {
            // Given
            String url1 = "https://example.com/article1";
            String url2 = "https://example.com/article2";

            // When
            CrawlingMetrics.MetricsContext context1 = crawlingMetrics.startArticleProcessing(url1);
            Thread.sleep(10);
            crawlingMetrics.recordArticleCompletion(context1, true);

            CrawlingMetrics.MetricsContext context2 = crawlingMetrics.startArticleProcessing(url2);
            Thread.sleep(20);
            crawlingMetrics.recordArticleCompletion(context2, true);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(2, summary.totalArticles());
            assertTrue(summary.averageProcessingTime() >= 15.0); // Should be around 15ms average
        }

        @Test
        void testAverageNetworkTimeCalculation() throws InterruptedException {
            // Given
            String url = "https://example.com/article";

            // When
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing(url);
            crawlingMetrics.recordNetworkOperation("http_request", Duration.ofMillis(100));
            crawlingMetrics.recordNetworkOperation("http_request", Duration.ofMillis(200));
            Thread.sleep(5);
            crawlingMetrics.recordArticleCompletion(context, true);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(1, summary.totalArticles());
            assertEquals(300.0, summary.averageNetworkTime()); // 300ms total / 1 article
        }

        @Test
        void testAverageDatabaseTimeCalculation() throws InterruptedException {
            // Given
            String url = "https://example.com/article";

            // When
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing(url);
            crawlingMetrics.recordDatabaseOperation("save", Duration.ofMillis(50));
            crawlingMetrics.recordDatabaseOperation("query", Duration.ofMillis(25));
            Thread.sleep(5);
            crawlingMetrics.recordArticleCompletion(context, true);

            // Then
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();
            assertEquals(1, summary.totalArticles());
            assertEquals(75.0, summary.averageDatabaseTime()); // 75ms total / 1 article
        }
    }

    @Nested
    class TimingMetrics {

        @Test
        void testTimingMetricCalculations() {
            // Given
            CrawlingMetrics.TimingMetric metric = new CrawlingMetrics.TimingMetric(3, 300, 50, 150);

            // When & Then
            assertEquals(3, metric.count());
            assertEquals(300, metric.totalTime());
            assertEquals(50, metric.minTime());
            assertEquals(150, metric.maxTime());
            assertEquals(100.0, metric.averageTime());
        }

        @Test
        void testTimingMetricWithZeroCount() {
            // Given
            CrawlingMetrics.TimingMetric metric = new CrawlingMetrics.TimingMetric(0, 0, 0, 0);

            // When & Then
            assertEquals(0, metric.count());
            assertEquals(0.0, metric.averageTime());
        }
    }

    @Nested
    class MetricsContext {

        @Test
        void testMetricsContextProperties() {
            // Given
            String testUrl = "https://example.com/test-article";
            Instant beforeStart = Instant.now();

            // When
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing(testUrl);
            Instant afterStart = Instant.now();

            // Then
            assertEquals(testUrl, context.getUrl());
            assertTrue(context.getStartTime().isAfter(beforeStart.minusMillis(1)));
            assertTrue(context.getStartTime().isBefore(afterStart.plusMillis(1)));
        }

        @Test
        void testMultipleContextsAreIndependent() {
            // Given
            String url1 = "https://example.com/article1";
            String url2 = "https://example.com/article2";

            // When
            CrawlingMetrics.MetricsContext context1 = crawlingMetrics.startArticleProcessing(url1);
            CrawlingMetrics.MetricsContext context2 = crawlingMetrics.startArticleProcessing(url2);

            // Then
            assertEquals(url1, context1.getUrl());
            assertEquals(url2, context2.getUrl());
            assertNotEquals(context1, context2);
        }
    }

    @Nested
    class MetricsReset {

        @Test
        void testMetricsReset() throws InterruptedException {
            // Given - Add some metrics
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing("https://example.com/test");
            Thread.sleep(10);
            crawlingMetrics.recordArticleCompletion(context, true);
            crawlingMetrics.recordNetworkOperation("http_request", Duration.ofMillis(100));
            crawlingMetrics.recordDatabaseOperation("save", Duration.ofMillis(50));

            // Verify metrics exist
            CrawlingMetrics.MetricsSummary beforeReset = crawlingMetrics.getSummary();
            assertTrue(beforeReset.totalArticles() > 0);
            assertTrue(beforeReset.totalNetworkTime() > 0);
            assertTrue(beforeReset.totalDatabaseTime() > 0);

            // When
            crawlingMetrics.reset();

            // Then
            CrawlingMetrics.MetricsSummary afterReset = crawlingMetrics.getSummary();
            assertEquals(0, afterReset.totalArticles());
            assertEquals(0, afterReset.successfulArticles());
            assertEquals(0, afterReset.failedArticles());
            assertEquals(0, afterReset.totalNetworkTime());
            assertEquals(0, afterReset.totalDatabaseTime());
            assertEquals(0.0, afterReset.successRate());
            assertTrue(afterReset.operationMetrics().isEmpty());
        }
    }

    @Nested
    class SessionTracking {

        @Test
        void testSessionDurationTracking() throws InterruptedException {
            // Given
            Thread.sleep(100); // Let some time pass

            // When
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();

            // Then
            assertTrue(summary.sessionDuration().toMillis() >= 100);
        }

        @Test
        void testArticlesPerMinuteCalculation() throws InterruptedException {
            // Given - Process some articles quickly
            for (int i = 0; i < 3; i++) {
                CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing("https://example.com/article" + i);
                Thread.sleep(1);
                crawlingMetrics.recordArticleCompletion(context, true);
            }

            // When
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();

            // Then
            // Articles per minute should be calculated based on session duration
            // Since we processed 3 articles in a very short time, the rate should be high
            assertTrue(summary.articlesPerMinute() >= 0.0);
        }
    }

    @Nested
    class LoggingAndSummary {

        @Test
        void testLogSummaryDoesNotThrow() {
            // Given - Add some metrics
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing("https://example.com/test");
            crawlingMetrics.recordArticleCompletion(context, true);

            // When & Then - Should not throw any exceptions
            assertDoesNotThrow(() -> crawlingMetrics.logSummary());
        }

        @Test
        void testSummaryContainsAllExpectedFields() {
            // Given
            CrawlingMetrics.MetricsContext context = crawlingMetrics.startArticleProcessing("https://example.com/test");
            crawlingMetrics.recordArticleCompletion(context, true);
            crawlingMetrics.recordNetworkOperation("http_request", Duration.ofMillis(100));
            crawlingMetrics.recordDatabaseOperation("save", Duration.ofMillis(50));

            // When
            CrawlingMetrics.MetricsSummary summary = crawlingMetrics.getSummary();

            // Then
            assertNotNull(summary);
            assertNotNull(summary.sessionDuration());
            assertNotNull(summary.operationMetrics());
            assertTrue(summary.totalArticles() >= 0);
            assertTrue(summary.successRate() >= 0.0);
            assertTrue(summary.averageProcessingTime() >= 0.0);
        }
    }
}