package ai.falsify.prediction.service;

import ai.falsify.prediction.config.LLMConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Factory for prediction extractors in the prediction-analysis module.
 * This factory provides access to both mock and Gemini extractors.
 */
@ApplicationScoped
public class PredictionAnalysisExtractorFactory {
    
    private static final Logger LOG = Logger.getLogger(PredictionAnalysisExtractorFactory.class);
    
    @ConfigProperty(name = "prediction.extractor.type", defaultValue = "gemini")
    String extractorType;
    
    @ConfigProperty(name = "prediction.extractor.auto-fallback", defaultValue = "true")
    boolean autoFallback;
    
    @Inject
    MockPredictionExtractor mockExtractor;
    
    @Inject
    GeminiPredictionExtractor geminiExtractor;
    
    @Inject
    LLMConfiguration llmConfiguration;
    
    /**
     * Gets the primary prediction extractor based on configuration.
     * 
     * @return the configured prediction extractor
     */
    public PredictionExtractor getPrimaryExtractor() {
        PredictionExtractor extractor = getExtractorByType(extractorType);
        
        if (extractor != null && extractor.isAvailable()) {
            LOG.debugf("Using primary extractor: %s", extractor.getExtractorType());
            return extractor;
        }
        
        if (autoFallback) {
            LOG.infof("Primary extractor (%s) not available, attempting fallback", extractorType);
            return getFallbackExtractor();
        }
        
        LOG.warnf("Primary extractor (%s) not available and auto-fallback disabled", extractorType);
        return extractor; // Return even if not available for error handling
    }
    
    /**
     * Gets the batch prediction extractor for the specified type.
     * 
     * @param type the extractor type
     * @return the batch extractor instance or null if not available
     */
    public BatchPredictionExtractor getBatchExtractor(String type) {
        PredictionExtractor extractor = getExtractorByType(type);
        if (extractor instanceof BatchPredictionExtractor) {
            return (BatchPredictionExtractor) extractor;
        }
        return null;
    }
    
    /**
     * Gets the best available prediction extractor with intelligent selection.
     * 
     * @return the best available prediction extractor
     */
    public PredictionExtractor getBestAvailableExtractor() {
        // Try Gemini first if available
        if (geminiExtractor.isAvailable()) {
            LOG.debugf("Using Gemini extractor (best available)");
            return geminiExtractor;
        }
        
        // Try the configured primary extractor
        PredictionExtractor primary = getExtractorByType(extractorType);
        if (primary != null && primary.isAvailable()) {
            LOG.debugf("Using configured primary extractor: %s", extractorType);
            return primary;
        }
        
        // Fallback to mock
        if (mockExtractor.isAvailable()) {
            LOG.debugf("Falling back to mock extractor");
            return mockExtractor;
        }
        
        LOG.warn("No prediction extractors are available");
        return mockExtractor; // Return mock as last resort
    }
    
    /**
     * Gets a fallback extractor when the primary is not available.
     * 
     * @return fallback prediction extractor
     */
    public PredictionExtractor getFallbackExtractor() {
        // Try extractors in order of preference
        String[] fallbackOrder = {"gemini", "mock"};
        
        for (String type : fallbackOrder) {
            if (!type.equals(extractorType)) { // Skip the primary type
                PredictionExtractor extractor = getExtractorByType(type);
                if (extractor != null && extractor.isAvailable()) {
                    LOG.infof("Using fallback extractor: %s", type);
                    return extractor;
                }
            }
        }
        
        // If no fallback is available, return mock as last resort
        LOG.warn("No fallback extractors available, returning mock extractor");
        return mockExtractor;
    }
    
    /**
     * Gets a specific extractor by type.
     * 
     * @param type the extractor type ("mock", "gemini", "llm")
     * @return the extractor instance or null if type is unknown
     */
    public PredictionExtractor getExtractorByType(String type) {
        if (type == null) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "mock" -> mockExtractor;
            case "gemini", "llm" -> geminiExtractor; // Both gemini and llm use the Gemini extractor
            default -> {
                LOG.warnf("Unknown extractor type: %s", type);
                yield null;
            }
        };
    }
    
    /**
     * Checks if a specific extractor type is available.
     * 
     * @param type the extractor type to check
     * @return true if the extractor is available
     */
    public boolean isExtractorAvailable(String type) {
        PredictionExtractor extractor = getExtractorByType(type);
        return extractor != null && extractor.isAvailable();
    }
    
    /**
     * Gets the status of all available extractors.
     * 
     * @return map of extractor types to their availability status
     */
    public java.util.Map<String, Object> getExtractorStatus() {
        return java.util.Map.of(
            "mock", java.util.Map.of(
                "available", mockExtractor.isAvailable(),
                "configuration", mockExtractor.getConfiguration()
            ),
            "gemini", java.util.Map.of(
                "available", geminiExtractor.isAvailable(),
                "configuration", geminiExtractor.getConfiguration()
            ),
            "primary", extractorType,
            "autoFallback", autoFallback
        );
    }
}