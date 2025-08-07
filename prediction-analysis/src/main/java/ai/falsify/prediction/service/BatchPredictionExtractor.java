package ai.falsify.prediction.service;

import ai.falsify.prediction.model.PredictionResult;
import java.util.List;
import java.util.Map;

/**
 * Interface for batch prediction extraction from multiple articles.
 * This interface extends the basic PredictionExtractor to support
 * batch processing for cost optimization and efficiency.
 */
public interface BatchPredictionExtractor extends PredictionExtractor {
    
    /**
     * Extracts predictions from multiple articles in a single batch request.
     * This method is designed to optimize API costs by combining multiple
     * articles into a single LLM request.
     * 
     * @param articles map of article ID to ArticleData containing text and title
     * @return map of article ID to list of prediction results
     */
    Map<String, List<PredictionResult>> extractPredictionsBatch(Map<String, ArticleData> articles);
    
    /**
     * Checks if batch processing is enabled and available.
     * 
     * @return true if batch processing is supported and enabled
     */
    boolean isBatchModeEnabled();
    
    /**
     * Gets the maximum batch size supported by this extractor.
     * 
     * @return maximum number of articles that can be processed in a single batch
     */
    int getMaxBatchSize();
    
    /**
     * Gets the recommended batch size for optimal performance and cost.
     * 
     * @return recommended batch size
     */
    default int getRecommendedBatchSize() {
        return Math.min(10, getMaxBatchSize());
    }
    
    /**
     * Data class representing an article for batch processing.
     */
    record ArticleData(
        String text,
        String title,
        Map<String, Object> metadata
    ) {
        public ArticleData(String text, String title) {
            this(text, title, Map.of());
        }
        
        /**
         * Gets a preview of the article for logging purposes.
         * 
         * @return truncated title or "Untitled"
         */
        public String getPreview() {
            if (title != null && !title.trim().isEmpty()) {
                return title.length() > 50 ? title.substring(0, 50) + "..." : title;
            }
            return "Untitled";
        }
        
        /**
         * Gets the word count of the article text.
         * 
         * @return approximate word count
         */
        public int getWordCount() {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            return text.trim().split("\\s+").length;
        }
        
        /**
         * Validates that the article has sufficient content for processing.
         * 
         * @return true if article has valid content
         */
        public boolean isValid() {
            return text != null && !text.trim().isEmpty() && getWordCount() >= 10;
        }
    }
    
    /**
     * Result class for batch processing operations.
     */
    record BatchProcessingResult(
        Map<String, List<PredictionResult>> predictions,
        int totalArticlesProcessed,
        int totalPredictionsFound,
        long processingTimeMs,
        double estimatedCost,
        Map<String, String> errors
    ) {
        /**
         * Gets the success rate of the batch processing.
         * 
         * @return success rate as a percentage (0.0 to 1.0)
         */
        public double getSuccessRate() {
            if (totalArticlesProcessed == 0) {
                return 0.0;
            }
            int successfulArticles = predictions.size();
            return (double) successfulArticles / totalArticlesProcessed;
        }
        
        /**
         * Gets the average predictions per article.
         * 
         * @return average number of predictions per successfully processed article
         */
        public double getAveragePredictionsPerArticle() {
            if (predictions.isEmpty()) {
                return 0.0;
            }
            return (double) totalPredictionsFound / predictions.size();
        }
        
        /**
         * Checks if the batch processing was completely successful.
         * 
         * @return true if all articles were processed without errors
         */
        public boolean isCompletelySuccessful() {
            return errors.isEmpty() && predictions.size() == totalArticlesProcessed;
        }
    }
}