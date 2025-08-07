package ai.falsify.prediction.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration class for LLM (Large Language Model) integration settings.
 * Provides centralized configuration management for LLM-based prediction
 * extraction.
 * 
 * This class handles configuration validation, security considerations for API
 * keys,
 * and provides defaults for various LLM providers.
 */
@ApplicationScoped
public class LLMConfiguration {

    private static final Logger LOG = Logger.getLogger(LLMConfiguration.class);

    // Core LLM Settings
    @ConfigProperty(name = "prediction.llm.enabled", defaultValue = "false")
    public boolean enabled;

    @ConfigProperty(name = "prediction.llm.provider", defaultValue = "gemini")
    public String provider;

    @ConfigProperty(name = "prediction.llm.model", defaultValue = "gemini-2.5-flash")
    public String model;

    // API Configuration
    @ConfigProperty(name = "prediction.llm.api-key")
    public Optional<String> apiKey;

    @ConfigProperty(name = "prediction.llm.api-url")
    public Optional<String> apiUrl;

    @ConfigProperty(name = "prediction.llm.organization-id")
    public Optional<String> organizationId;

    // Request Configuration
    @ConfigProperty(name = "prediction.llm.timeout-seconds", defaultValue = "30")
    public int timeoutSeconds;

    @ConfigProperty(name = "prediction.llm.max-tokens", defaultValue = "1000")
    public int maxTokens;

    @ConfigProperty(name = "prediction.llm.temperature", defaultValue = "0.3")
    public double temperature;

    @ConfigProperty(name = "prediction.llm.top-p", defaultValue = "1.0")
    public double topP;

    @ConfigProperty(name = "prediction.llm.frequency-penalty", defaultValue = "0.0")
    public double frequencyPenalty;

    @ConfigProperty(name = "prediction.llm.presence-penalty", defaultValue = "0.0")
    public double presencePenalty;

    // Retry and Rate Limiting
    @ConfigProperty(name = "prediction.llm.retry-attempts", defaultValue = "3")
    public int retryAttempts;

    @ConfigProperty(name = "prediction.llm.retry-delay-seconds", defaultValue = "1")
    public int retryDelaySeconds;

    @ConfigProperty(name = "prediction.llm.rate-limit-per-minute", defaultValue = "60")
    public int rateLimitPerMinute;

    @ConfigProperty(name = "prediction.llm.rate-limit-per-hour", defaultValue = "1000")
    public int rateLimitPerHour;

    // Fallback and Error Handling
    @ConfigProperty(name = "prediction.llm.fallback-to-mock", defaultValue = "true")
    public boolean fallbackToMock;

    @ConfigProperty(name = "prediction.llm.fail-fast", defaultValue = "false")
    public boolean failFast;

    // Cost Management
    @ConfigProperty(name = "prediction.llm.max-cost-per-request", defaultValue = "0.10")
    public double maxCostPerRequest;

    @ConfigProperty(name = "prediction.llm.daily-cost-limit", defaultValue = "10.00")
    public double dailyCostLimit;

    // Caching
    @ConfigProperty(name = "prediction.llm.enable-caching", defaultValue = "true")
    public boolean enableCaching;

    @ConfigProperty(name = "prediction.llm.cache-ttl-hours", defaultValue = "24")
    public int cacheTtlHours;

    // Batch Processing Configuration
    @ConfigProperty(name = "prediction.llm.batch-mode", defaultValue = "true")
    public boolean batchMode;

    @ConfigProperty(name = "prediction.llm.max-batch-size", defaultValue = "10")
    public int maxBatchSize;

    @ConfigProperty(name = "prediction.llm.batch-timeout-seconds", defaultValue = "60")
    public int batchTimeoutSeconds;

    // Monitoring and Logging
    @ConfigProperty(name = "prediction.llm.enable-metrics", defaultValue = "true")
    public boolean enableMetrics;

    @ConfigProperty(name = "prediction.llm.log-requests", defaultValue = "false")
    public boolean logRequests;

    @ConfigProperty(name = "prediction.llm.log-responses", defaultValue = "false")
    public boolean logResponses;

    @jakarta.annotation.PostConstruct
    void init() {
        LOG.infof("LLMConfiguration initialized: enabled=%s, provider=%s, model=%s, apiKey=%s",
                enabled, provider, model, apiKey.isPresent() ? "***set***" : "not set");
    }

    /**
     * Validates the current LLM configuration.
     * 
     * @return true if configuration is valid for LLM operations
     */
    public boolean isValid() {
        if (!enabled) {
            return false;
        }

        // Check required fields
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            return false;
        }

        if (provider == null || provider.trim().isEmpty()) {
            return false;
        }

        if (model == null || model.trim().isEmpty()) {
            return false;
        }

        // Validate numeric ranges
        if (timeoutSeconds <= 0 || timeoutSeconds > 300) {
            return false;
        }

        if (maxTokens <= 0 || maxTokens > 8000) {
            return false;
        }

        if (temperature < 0.0 || temperature > 2.0) {
            return false;
        }

        if (retryAttempts < 0 || retryAttempts > 10) {
            return false;
        }

        return true;
    }

    /**
     * Gets the timeout as a Duration object.
     * 
     * @return timeout duration
     */
    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(timeoutSeconds);
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
     * Gets the cache TTL as a Duration object.
     * 
     * @return cache TTL duration
     */
    public Duration getCacheTtlDuration() {
        return Duration.ofHours(cacheTtlHours);
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
     * Gets the effective API URL based on provider and configuration.
     * 
     * @return API URL to use
     */
    public String getEffectiveApiUrl() {
        if (apiUrl.isPresent() && !apiUrl.get().trim().isEmpty()) {
            return apiUrl.get();
        }

        // Return default URLs based on provider
        return switch (provider.toLowerCase()) {
            case "openai" -> "https://api.openai.com/v1";
            case "anthropic" -> "https://api.anthropic.com/v1";
            case "azure" -> "https://your-resource.openai.azure.com";
            case "huggingface" -> "https://api-inference.huggingface.co/models";
            case "gemini" -> "https://generativelanguage.googleapis.com/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    /**
     * Gets provider-specific model defaults.
     * 
     * @return recommended model for the configured provider
     */
    public String getRecommendedModel() {
        return switch (provider.toLowerCase()) {
            case "openai" -> "gpt-3.5-turbo";
            case "anthropic" -> "claude-3-sonnet-20240229";
            case "azure" -> "gpt-35-turbo";
            case "huggingface" -> "microsoft/DialoGPT-medium";
            case "gemini" -> "gemini-1.5-flash";
            default -> model;
        };
    }

    /**
     * Gets configuration summary for monitoring and debugging.
     * 
     * @return configuration summary map
     */
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("enabled", enabled);
        summary.put("provider", provider);
        summary.put("model", model);
        summary.put("hasApiKey", apiKey.isPresent() && !apiKey.get().trim().isEmpty());
        summary.put("apiUrl", getEffectiveApiUrl());
        summary.put("timeoutSeconds", timeoutSeconds);
        summary.put("maxTokens", maxTokens);
        summary.put("temperature", temperature);
        summary.put("retryAttempts", retryAttempts);
        summary.put("rateLimitPerMinute", rateLimitPerMinute);
        summary.put("fallbackToMock", fallbackToMock);
        summary.put("enableCaching", enableCaching);
        summary.put("valid", isValid());
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
        security.put("hasOrganizationId", organizationId.isPresent());
        security.put("logRequests", logRequests);
        security.put("logResponses", logResponses);
        security.put("enableCaching", enableCaching);
        security.put("maxCostPerRequest", maxCostPerRequest);
        security.put("dailyCostLimit", dailyCostLimit);
        return security;
    }

    /**
     * Validates API key format based on provider.
     * 
     * @return true if API key format is valid for the provider
     */
    public boolean isApiKeyFormatValid() {
        if (!apiKey.isPresent() || apiKey.get().trim().isEmpty()) {
            return false;
        }

        String key = apiKey.get().trim();

        return switch (provider.toLowerCase()) {
            case "openai" -> key.startsWith("sk-") && key.length() > 20;
            case "anthropic" -> key.startsWith("sk-ant-") && key.length() > 20;
            case "azure" -> key.length() == 32; // Azure keys are typically 32 characters
            case "huggingface" -> key.startsWith("hf_") && key.length() > 20;
            case "gemini" -> key.startsWith("AIza") && key.length() > 30; // Google API keys start with AIza
            default -> key.length() > 10; // Generic validation
        };
    }

    /**
     * Gets estimated cost per token based on provider and model.
     * 
     * @return estimated cost per token in USD
     */
    public double getEstimatedCostPerToken() {
        // These are approximate costs and should be updated based on current pricing
        return switch (provider.toLowerCase()) {
            case "openai" -> switch (model.toLowerCase()) {
                case "gpt-4" -> 0.00003;
                case "gpt-3.5-turbo" -> 0.000002;
                default -> 0.000002;
            };
            case "anthropic" -> switch (model.toLowerCase()) {
                case "claude-3-opus-20240229" -> 0.000015;
                case "claude-3-sonnet-20240229" -> 0.000003;
                default -> 0.000003;
            };
            case "gemini" -> switch (model.toLowerCase()) {
                case "gemini-1.5-pro" -> 0.0000035; // $3.50 per 1M input tokens
                case "gemini-1.5-flash" -> 0.00000035; // $0.35 per 1M input tokens
                default -> 0.00000035;
            };
            default -> 0.000002; // Default estimate
        };
    }
}