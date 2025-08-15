package ai.falsify.prediction.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration class for Gemini Native integration settings.
 * Provides centralized configuration management for native Google GenAI library
 * integration with support for true asynchronous batch processing.
 * 
 * This class handles configuration validation, security considerations for API
 * keys, and provides comprehensive settings for batch processing operations.
 */
@ApplicationScoped
public class GeminiNativeConfiguration {

    private static final Logger LOG = Logger.getLogger(GeminiNativeConfiguration.class);

    // Core Gemini Native Settings
    @ConfigProperty(name = "prediction.gemini-native.enabled", defaultValue = "false")
    public boolean enabled;

    @ConfigProperty(name = "prediction.gemini-native.api-key")
    public Optional<String> apiKey;

    @ConfigProperty(name = "prediction.gemini-native.model", defaultValue = "gemini-2.5-flash")
    public String model;

    @ConfigProperty(name = "prediction.gemini-native.base-url", defaultValue = "https://generativelanguage.googleapis.com")
    public String baseUrl;

    // Batch Processing Configuration
    @ConfigProperty(name = "prediction.gemini-native.max-batch-size", defaultValue = "20")
    public int maxBatchSize;

    @ConfigProperty(name = "prediction.gemini-native.polling-interval-seconds", defaultValue = "10")
    public int pollingIntervalSeconds;

    @ConfigProperty(name = "prediction.gemini-native.max-retries", defaultValue = "3")
    public int maxRetries;

    @ConfigProperty(name = "prediction.gemini-native.timeout-minutes", defaultValue = "30")
    public int timeoutMinutes;

    // Advanced Batch Configuration
    @ConfigProperty(name = "prediction.gemini-native.batch.enabled", defaultValue = "true")
    public boolean batchEnabled;

    @ConfigProperty(name = "prediction.gemini-native.batch.max-concurrent-jobs", defaultValue = "3")
    public int maxConcurrentJobs;

    @ConfigProperty(name = "prediction.gemini-native.batch.retry-delay-seconds", defaultValue = "5")
    public int retryDelaySeconds;

    @ConfigProperty(name = "prediction.gemini-native.batch.max-retry-delay-seconds", defaultValue = "300")
    public int maxRetryDelaySeconds;

    // Monitoring Configuration
    @ConfigProperty(name = "prediction.gemini-native.monitoring.enabled", defaultValue = "true")
    public boolean monitoringEnabled;

    @ConfigProperty(name = "prediction.gemini-native.monitoring.metrics-enabled", defaultValue = "true")
    public boolean metricsEnabled;

    @ConfigProperty(name = "prediction.gemini-native.monitoring.health-check-enabled", defaultValue = "true")
    public boolean healthCheckEnabled;

    @jakarta.annotation.PostConstruct
    void init() {
        LOG.infof("GeminiNativeConfiguration initialized: enabled=%s, model=%s, apiKey=%s, batchEnabled=%s",
                enabled, model, apiKey.isPresent() ? "***set***" : "not set", batchEnabled);
    }

    /**
     * Validates the current Gemini Native configuration.
     * 
     * @return true if configuration is valid for Gemini Native operations
     */
    public boolean isValid() {
        if (!enabled) {
            return false;
        }

        // Check required fields
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            LOG.warn("Gemini Native API key is not configured");
            return false;
        }

        if (model == null || model.trim().isEmpty()) {
            LOG.warn("Gemini Native model is not configured");
            return false;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            LOG.warn("Gemini Native base URL is not configured");
            return false;
        }

        // Validate numeric ranges
        if (maxBatchSize <= 0 || maxBatchSize > 100) {
            LOG.warn("Invalid max batch size: " + maxBatchSize);
            return false;
        }

        if (pollingIntervalSeconds <= 0 || pollingIntervalSeconds > 300) {
            LOG.warn("Invalid polling interval: " + pollingIntervalSeconds);
            return false;
        }

        if (maxRetries < 0 || maxRetries > 10) {
            LOG.warn("Invalid max retries: " + maxRetries);
            return false;
        }

        if (timeoutMinutes <= 0 || timeoutMinutes > 120) {
            LOG.warn("Invalid timeout minutes: " + timeoutMinutes);
            return false;
        }

        if (maxConcurrentJobs <= 0 || maxConcurrentJobs > 10) {
            LOG.warn("Invalid max concurrent jobs: " + maxConcurrentJobs);
            return false;
        }

        return true;
    }

    /**
     * Validates API key format for Google GenAI.
     * 
     * @return true if API key format is valid
     */
    public boolean isApiKeyFormatValid() {
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            return false;
        }

        String key = apiKey.get().trim();
        // Google API keys typically start with "AIza" and are longer than 30 characters
        return key.startsWith("AIza") && key.length() > 30;
    }

    /**
     * Gets the timeout as a Duration object.
     * 
     * @return timeout duration
     */
    public Duration getTimeoutDuration() {
        return Duration.ofMinutes(timeoutMinutes);
    }

    /**
     * Gets the polling interval as a Duration object.
     * 
     * @return polling interval duration
     */
    public Duration getPollingIntervalDuration() {
        return Duration.ofSeconds(pollingIntervalSeconds);
    }

    /**
     * Gets the retry delay as a Duration object.
     * 
     * @return retry delay duration
     */
    public Duration getRetryDelayDuration() {
        return Duration.ofSeconds(retryDelaySeconds);
    }

    /**
     * Gets the max retry delay as a Duration object.
     * 
     * @return max retry delay duration
     */
    public Duration getMaxRetryDelayDuration() {
        return Duration.ofSeconds(maxRetryDelaySeconds);
    }

    /**
     * Gets the masked API key for logging (shows only first and last 4 characters).
     * 
     * @return masked API key or "not configured"
     */
    public String getMaskedApiKey() {
        if (!apiKey.isPresent() || apiKey.get().length() < 8) {
            return "not configured";
        }

        String key = apiKey.get();
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * Gets configuration summary for monitoring and debugging.
     * 
     * @return configuration summary map
     */
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("enabled", enabled);
        summary.put("model", model);
        summary.put("baseUrl", baseUrl);
        summary.put("hasApiKey", apiKey.isPresent() && !apiKey.get().trim().isEmpty());
        summary.put("maxBatchSize", maxBatchSize);
        summary.put("pollingIntervalSeconds", pollingIntervalSeconds);
        summary.put("maxRetries", maxRetries);
        summary.put("timeoutMinutes", timeoutMinutes);
        summary.put("batchEnabled", batchEnabled);
        summary.put("maxConcurrentJobs", maxConcurrentJobs);
        summary.put("monitoringEnabled", monitoringEnabled);
        summary.put("metricsEnabled", metricsEnabled);
        summary.put("healthCheckEnabled", healthCheckEnabled);
        summary.put("valid", isValid());
        summary.put("apiKeyFormatValid", isApiKeyFormatValid());
        return summary;
    }

    /**
     * Gets security-related configuration for audit purposes.
     * 
     * @return security configuration map
     */
    public Map<String, Object> getSecurityConfiguration() {
        Map<String, Object> security = new java.util.HashMap<>();
        security.put("hasApiKey", apiKey.isPresent() && !apiKey.get().trim().isEmpty());
        security.put("maskedApiKey", getMaskedApiKey());
        security.put("apiKeyFormatValid", isApiKeyFormatValid());
        security.put("baseUrl", baseUrl);
        security.put("monitoringEnabled", monitoringEnabled);
        security.put("metricsEnabled", metricsEnabled);
        return security;
    }

    /**
     * Gets batch processing configuration details.
     * 
     * @return batch configuration map
     */
    public Map<String, Object> getBatchConfiguration() {
        Map<String, Object> batch = new java.util.HashMap<>();
        batch.put("enabled", batchEnabled);
        batch.put("maxBatchSize", maxBatchSize);
        batch.put("maxConcurrentJobs", maxConcurrentJobs);
        batch.put("pollingIntervalSeconds", pollingIntervalSeconds);
        batch.put("retryDelaySeconds", retryDelaySeconds);
        batch.put("maxRetryDelaySeconds", maxRetryDelaySeconds);
        batch.put("timeoutMinutes", timeoutMinutes);
        batch.put("maxRetries", maxRetries);
        return batch;
    }

    /**
     * Checks if the configuration is ready for production use.
     * 
     * @return true if configuration meets production requirements
     */
    public boolean isProductionReady() {
        if (!isValid()) {
            return false;
        }

        // Additional production checks
        if (!isApiKeyFormatValid()) {
            LOG.warn("API key format is invalid for production use");
            return false;
        }

        if (timeoutMinutes < 5) {
            LOG.warn("Timeout too low for production use: " + timeoutMinutes);
            return false;
        }

        if (maxRetries < 2) {
            LOG.warn("Max retries too low for production use: " + maxRetries);
            return false;
        }

        return true;
    }

    /**
     * Gets estimated cost per token for Gemini models.
     * 
     * @return estimated cost per token in USD
     */
    public double getEstimatedCostPerToken() {
        return switch (model.toLowerCase()) {
            case "gemini-1.5-pro" -> 0.0000035; // $3.50 per 1M input tokens
            case "gemini-1.5-flash" -> 0.00000035; // $0.35 per 1M input tokens
            case "gemini-1.0-pro" -> 0.0000005; // $0.50 per 1M input tokens
            default -> 0.00000035; // Default to flash pricing
        };
    }

    /**
     * Gets the effective API endpoint URL for the configured model.
     * 
     * @return full API endpoint URL
     */
    public String getEffectiveApiUrl() {
        return baseUrl + "/v1/models/" + model + ":generateContent";
    }

    /**
     * Gets the batch API endpoint URL.
     * 
     * @return batch API endpoint URL
     */
    public String getBatchApiUrl() {
        return baseUrl + "/v1/batches";
    }
}