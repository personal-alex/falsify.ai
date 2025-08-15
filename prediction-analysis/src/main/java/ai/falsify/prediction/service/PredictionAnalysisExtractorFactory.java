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
    
    @ConfigProperty(name = "prediction.extractor.type", defaultValue = "gemini-native")
    String extractorType;
    
    @ConfigProperty(name = "prediction.extractor.auto-fallback", defaultValue = "true")
    boolean autoFallback;
    
    @Inject
    MockPredictionExtractor mockExtractor;
    
    @Inject
    GeminiPredictionExtractor geminiExtractor;
    
    @Inject
    GeminiNativePredictionExtractor geminiNativeExtractor;
    
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
     * Gets an extractor based on configuration preference with intelligent fallback.
     * This method respects configuration while providing smart defaults.
     * 
     * @return the best configured prediction extractor
     */
    public PredictionExtractor getConfiguredExtractor() {
        // First try the explicitly configured extractor
        PredictionExtractor configured = getExtractorByType(extractorType);
        if (configured != null && configured.isAvailable()) {
            LOG.debugf("Using configured extractor: %s", extractorType);
            return configured;
        }
        
        // If configured extractor is not available, use intelligent selection
        if (autoFallback) {
            LOG.infof("Configured extractor (%s) not available, using best available", extractorType);
            return getBestAvailableExtractor();
        }
        
        // Return configured extractor even if not available (for error handling)
        return configured != null ? configured : mockExtractor;
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
     * Prefers gemini-native over other extractors for better performance.
     * 
     * @return the best available prediction extractor
     */
    public PredictionExtractor getBestAvailableExtractor() {
        // Try Gemini Native first if available (preferred for batch processing)
        if (geminiNativeExtractor.isAvailable()) {
            LOG.debugf("Using Gemini Native extractor (best available)");
            return geminiNativeExtractor;
        }
        
        // Try the configured primary extractor
        PredictionExtractor primary = getExtractorByType(extractorType);
        if (primary != null && primary.isAvailable()) {
            LOG.debugf("Using configured primary extractor: %s", extractorType);
            return primary;
        }
        
        // Try Gemini (LangChain4j) as fallback
        if (geminiExtractor.isAvailable()) {
            LOG.debugf("Using Gemini extractor (fallback)");
            return geminiExtractor;
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
        // Try extractors in order of preference (gemini-native preferred over gemini)
        String[] fallbackOrder = {"gemini-native", "gemini", "mock"};
        
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
     * @param type the extractor type ("mock", "gemini", "gemini-native", "llm")
     * @return the extractor instance or null if type is unknown
     */
    public PredictionExtractor getExtractorByType(String type) {
        LOG.infof("Extracting predictor for type %s", type);
        if (type == null) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "mock" -> mockExtractor;
            case "gemini" -> geminiExtractor;
            case "gemini-native" -> geminiNativeExtractor;
            case "llm" -> {
                // For "llm" type, use the configured primary extractor type
                // This allows the client to use "llm" as a generic type while respecting configuration
                LOG.infof("LLM type requested, using configured primary extractor: %s", extractorType);
                yield getExtractorByType(extractorType);
            }
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
     * Performs health check on a specific extractor.
     * 
     * @param type the extractor type to check
     * @return health status information
     */
    public java.util.Map<String, Object> checkExtractorHealth(String type) {
        PredictionExtractor extractor = getExtractorByType(type);
        if (extractor == null) {
            return java.util.Map.of(
                "type", type,
                "exists", false,
                "available", false,
                "error", "Unknown extractor type"
            );
        }
        
        boolean available = extractor.isAvailable();
        String configuration = extractor.getConfiguration();
        
        return java.util.Map.of(
            "type", type,
            "exists", true,
            "available", available,
            "configuration", configuration,
            "extractorType", extractor.getExtractorType()
        );
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
            "gemini-native", java.util.Map.of(
                "available", geminiNativeExtractor.isAvailable(),
                "configuration", geminiNativeExtractor.getConfiguration()
            ),
            "primary", extractorType,
            "autoFallback", autoFallback
        );
    }
    
    /**
     * Gets detailed status report for all extractors including health information.
     * 
     * @return comprehensive status report
     */
    public java.util.Map<String, Object> getDetailedExtractorStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        // Add individual extractor status
        status.put("extractors", java.util.Map.of(
            "mock", checkExtractorHealth("mock"),
            "gemini", checkExtractorHealth("gemini"),
            "gemini-native", checkExtractorHealth("gemini-native")
        ));
        
        // Add configuration information
        status.put("configuration", java.util.Map.of(
            "primaryType", extractorType,
            "autoFallback", autoFallback
        ));
        
        // Add selection information
        PredictionExtractor primary = getPrimaryExtractor();
        PredictionExtractor best = getBestAvailableExtractor();
        
        status.put("selection", java.util.Map.of(
            "primary", primary != null ? primary.getExtractorType() : "none",
            "bestAvailable", best != null ? best.getExtractorType() : "none",
            "primaryAvailable", primary != null && primary.isAvailable(),
            "bestAvailable", best != null && best.isAvailable()
        ));
        
        return status;
    }
    
    /**
     * Gets a summary of extractor availability for quick status checks.
     * 
     * @return availability summary
     */
    public java.util.Map<String, Boolean> getExtractorAvailabilitySummary() {
        return java.util.Map.of(
            "mock", mockExtractor.isAvailable(),
            "gemini", geminiExtractor.isAvailable(),
            "gemini-native", geminiNativeExtractor.isAvailable(),
            "anyAvailable", mockExtractor.isAvailable() || 
                           geminiExtractor.isAvailable() || 
                           geminiNativeExtractor.isAvailable()
        );
    }
}