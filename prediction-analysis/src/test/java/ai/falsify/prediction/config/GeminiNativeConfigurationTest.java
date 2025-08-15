package ai.falsify.prediction.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GeminiNativeConfiguration to verify configuration loading
 * and validation logic.
 */
@QuarkusTest
class GeminiNativeConfigurationTest {

    @Inject
    GeminiNativeConfiguration config;

    @Test
    void testConfigurationInjection() {
        assertNotNull(config, "Configuration should be injected");
    }

    @Test
    void testConfigurationValues() {
        // Test that configuration values are properly loaded
        // Note: Some values might be null in test environment due to configuration overrides
        assertNotNull(config.baseUrl, "Base URL should not be null");
        assertTrue(config.maxBatchSize > 0, "Max batch size should be positive");
        assertTrue(config.pollingIntervalSeconds > 0, "Polling interval should be positive");
        assertTrue(config.maxRetries >= 0, "Max retries should be non-negative");
        assertTrue(config.timeoutMinutes > 0, "Timeout should be positive");
        assertTrue(config.maxConcurrentJobs > 0, "Max concurrent jobs should be positive");
    }

    @Test
    void testConfigurationSummary() {
        var summary = config.getConfigurationSummary();
        
        assertNotNull(summary, "Configuration summary should not be null");
        assertTrue(summary.containsKey("enabled"), "Summary should contain enabled flag");
        assertTrue(summary.containsKey("model"), "Summary should contain model");
        assertTrue(summary.containsKey("maxBatchSize"), "Summary should contain max batch size");
        assertTrue(summary.containsKey("valid"), "Summary should contain validation status");
    }

    @Test
    void testSecurityConfiguration() {
        var security = config.getSecurityConfiguration();
        
        assertNotNull(security, "Security configuration should not be null");
        assertTrue(security.containsKey("hasApiKey"), "Security should contain API key status");
        assertTrue(security.containsKey("maskedApiKey"), "Security should contain masked API key");
        assertTrue(security.containsKey("apiKeyFormatValid"), "Security should contain API key format validation");
    }

    @Test
    void testBatchConfiguration() {
        var batch = config.getBatchConfiguration();
        
        assertNotNull(batch, "Batch configuration should not be null");
        assertTrue(batch.containsKey("enabled"), "Batch config should contain enabled flag");
        assertTrue(batch.containsKey("maxBatchSize"), "Batch config should contain max batch size");
        assertTrue(batch.containsKey("maxConcurrentJobs"), "Batch config should contain max concurrent jobs");
    }

    @Test
    void testDurationMethods() {
        assertNotNull(config.getTimeoutDuration(), "Timeout duration should not be null");
        assertNotNull(config.getPollingIntervalDuration(), "Polling interval duration should not be null");
        assertNotNull(config.getRetryDelayDuration(), "Retry delay duration should not be null");
        assertNotNull(config.getMaxRetryDelayDuration(), "Max retry delay duration should not be null");
        
        assertEquals(30, config.getTimeoutDuration().toMinutes(), "Timeout duration should be 30 minutes");
        assertEquals(10, config.getPollingIntervalDuration().getSeconds(), "Polling interval should be 10 seconds");
    }

    @Test
    void testApiKeyValidation() {
        // With test API key, format validation should fail (it's not a real Google API key format)
        assertFalse(config.isApiKeyFormatValid(), "Test API key format should be invalid");
        
        // With test API key, overall validation should pass (other settings are valid)
        assertTrue(config.isValid(), "Configuration should be valid with test settings");
    }

    @Test
    void testMaskedApiKey() {
        String maskedKey = config.getMaskedApiKey();
        assertNotNull(maskedKey, "Masked API key should not be null");
        assertTrue(maskedKey.contains("****"), "Masked key should contain asterisks");
        assertTrue(maskedKey.startsWith("test"), "Masked key should start with 'test'");
        assertTrue(maskedKey.endsWith("6789"), "Masked key should end with '6789'");
    }

    @Test
    void testCostEstimation() {
        double cost = config.getEstimatedCostPerToken();
        assertTrue(cost > 0, "Cost per token should be positive");
        assertEquals(0.00000035, cost, 0.0000001, "Cost should match gemini-1.5-flash pricing");
    }

    @Test
    void testApiUrls() {
        String apiUrl = config.getEffectiveApiUrl();
        assertNotNull(apiUrl, "API URL should not be null");
        assertTrue(apiUrl.contains("generativelanguage.googleapis.com"), "API URL should contain correct domain");
        assertTrue(apiUrl.contains("gemini-1.5-flash"), "API URL should contain model name");
        
        String batchUrl = config.getBatchApiUrl();
        assertNotNull(batchUrl, "Batch API URL should not be null");
        assertTrue(batchUrl.contains("batches"), "Batch URL should contain batches endpoint");
    }

    @Test
    void testProductionReadiness() {
        // With test API key (invalid format), should not be production ready
        assertFalse(config.isProductionReady(), "Should not be production ready with test API key");
    }
}