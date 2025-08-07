package ai.falsify.prediction.service;

import ai.falsify.prediction.model.PredictionResult;
import java.util.List;

/**
 * Interface for extracting predictions from article content.
 * Implementations can use different approaches such as mock data generation,
 * LLM-based extraction, or rule-based pattern matching.
 */
public interface PredictionExtractor {
    
    /**
     * Extracts predictions from the given article text and title.
     * 
     * @param articleText the full text content of the article
     * @param articleTitle the title of the article
     * @return list of prediction results found in the article
     */
    List<PredictionResult> extractPredictions(String articleText, String articleTitle);
    
    /**
     * Checks if the prediction extractor is available and ready to use.
     * 
     * @return true if the extractor is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Gets the type of this prediction extractor.
     * 
     * @return the extractor type (e.g., "mock", "llm", "rule-based")
     */
    String getExtractorType();
    
    /**
     * Gets the configuration or status information for this extractor.
     * 
     * @return configuration/status information
     */
    default String getConfiguration() {
        return "Type: " + getExtractorType() + ", Available: " + isAvailable();
    }
}