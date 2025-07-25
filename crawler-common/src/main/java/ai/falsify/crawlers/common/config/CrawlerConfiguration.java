package ai.falsify.crawlers.common.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/**
 * Base configuration interface for all crawler implementations.
 * Provides common configuration properties that can be extended by specific crawlers.
 */
@ConfigMapping(prefix = "crawler.common")
public interface CrawlerConfiguration {

    /**
     * Network configuration settings
     */
    NetworkConfig network();

    /**
     * Content processing configuration
     */
    ContentConfig content();

    /**
     * Performance and resource configuration
     */
    PerformanceConfig performance();

    /**
     * Retry and resilience configuration
     */
    RetryConfig retry();

    /**
     * Redis configuration
     */
    RedisConfig redis();

    /**
     * Logging configuration
     */
    LoggingConfig logging();

    interface NetworkConfig {
        
        @NotNull
        @WithName("connection-timeout")
        @WithDefault("PT30S")
        Duration connectionTimeout();

        @NotNull
        @WithName("read-timeout")
        @WithDefault("PT30S")
        Duration readTimeout();

        @NotBlank
        @WithName("user-agent")
        @WithDefault("Mozilla/5.0 (compatible; CrawlerCommon/1.0)")
        String userAgent();

        @Min(1)
        @Max(10)
        @WithName("max-redirects")
        @WithDefault("3")
        int maxRedirects();

        @WithName("follow-redirects")
        @WithDefault("true")
        boolean followRedirects();

        @WithName("ignore-ssl-errors")
        @WithDefault("false")
        boolean ignoreSslErrors();
    }

    interface ContentConfig {
        
        @Min(10)
        @WithName("min-content-length")
        @WithDefault("100")
        int minContentLength();

        @Min(1)
        @Max(100000)
        @WithName("max-content-length")
        @WithDefault("50000")
        int maxContentLength();

        @WithName("enable-content-validation")
        @WithDefault("true")
        boolean enableContentValidation();

        @WithName("enable-duplicate-detection")
        @WithDefault("true")
        boolean enableDuplicateDetection();

        @NotNull
        @WithName("duplicate-cache-ttl")
        @WithDefault("P7D")
        Duration duplicateCacheTtl();

        @Min(1)
        @Max(100)
        @WithName("min-word-count")
        @WithDefault("10")
        int minWordCount();

        @WithName("max-html-ratio")
        @WithDefault("0.3")
        double maxHtmlRatio();
    }

    interface PerformanceConfig {
        
        @Min(1)
        @Max(100)
        @WithName("max-concurrent-requests")
        @WithDefault("5")
        int maxConcurrentRequests();

        @NotNull
        @WithName("request-delay")
        @WithDefault("PT1S")
        Duration requestDelay();

        @Min(1)
        @Max(1000)
        @WithName("batch-size")
        @WithDefault("10")
        int batchSize();

        @WithName("enable-metrics")
        @WithDefault("true")
        boolean enableMetrics();

        @NotNull
        @WithName("metrics-flush-interval")
        @WithDefault("PT5M")
        Duration metricsFlushInterval();

        @Min(1)
        @Max(10000)
        @WithName("max-memory-usage-mb")
        @WithDefault("512")
        int maxMemoryUsageMb();
    }

    interface RetryConfig {
        
        @Min(0)
        @Max(10)
        @WithName("max-attempts")
        @WithDefault("3")
        int maxAttempts();

        @NotNull
        @WithName("initial-delay")
        @WithDefault("PT1S")
        Duration initialDelay();

        @NotNull
        @WithName("max-delay")
        @WithDefault("PT30S")
        Duration maxDelay();

        @WithName("backoff-multiplier")
        @WithDefault("2.0")
        double backoffMultiplier();

        @WithName("enable-circuit-breaker")
        @WithDefault("true")
        boolean enableCircuitBreaker();

        @Min(1)
        @Max(100)
        @WithName("circuit-breaker-failure-threshold")
        @WithDefault("5")
        int circuitBreakerFailureThreshold();

        @NotNull
        @WithName("circuit-breaker-timeout")
        @WithDefault("PT60S")
        Duration circuitBreakerTimeout();

        @WithName("enable-jitter")
        @WithDefault("true")
        boolean enableJitter();
    }

    interface RedisConfig {
        
        @WithName("enable-redis")
        @WithDefault("true")
        boolean enableRedis();

        @NotNull
        @WithName("key-prefix")
        @WithDefault("crawler")
        String keyPrefix();

        @NotNull
        @WithName("default-ttl")
        @WithDefault("P1D")
        Duration defaultTtl();

        @Min(1)
        @Max(10000)
        @WithName("max-connections")
        @WithDefault("20")
        int maxConnections();

        @NotNull
        @WithName("connection-timeout")
        @WithDefault("PT5S")
        Duration connectionTimeout();

        @WithName("enable-health-check")
        @WithDefault("true")
        boolean enableHealthCheck();

        @NotNull
        @WithName("health-check-interval")
        @WithDefault("PT30S")
        Duration healthCheckInterval();
    }

    interface LoggingConfig {
        
        @WithName("enable-structured-logging")
        @WithDefault("true")
        boolean enableStructuredLogging();

        @WithName("enable-performance-logging")
        @WithDefault("true")
        boolean enablePerformanceLogging();

        @WithName("log-request-details")
        @WithDefault("false")
        boolean logRequestDetails();

        @WithName("log-response-details")
        @WithDefault("false")
        boolean logResponseDetails();

        @NotNull
        @WithName("log-level")
        @WithDefault("INFO")
        String logLevel();

        @WithName("enable-metrics-logging")
        @WithDefault("true")
        boolean enableMetricsLogging();
    }
}