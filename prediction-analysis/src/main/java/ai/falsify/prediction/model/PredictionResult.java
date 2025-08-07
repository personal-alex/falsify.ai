package ai.falsify.prediction.model;

import java.math.BigDecimal;

/**
 * Record representing a prediction extraction result.
 * Contains the prediction text, metadata, and confidence/rating information.
 */
public record PredictionResult(
    String predictionText,
    String predictionType,
    Integer rating,
    BigDecimal confidenceScore,
    String context,
    String timeframe,
    String subject
) {
    
    /**
     * Creates a PredictionResult with validation.
     * 
     * @param predictionText the prediction text (required)
     * @param predictionType the type/category of prediction (optional)
     * @param rating the rating from 1-5 stars (required)
     * @param confidenceScore the confidence score from 0.0-1.0 (optional, defaults to 0.0)
     * @param context the surrounding context text (optional)
     * @param timeframe the timeframe for the prediction (optional)
     * @param subject the subject of the prediction (optional)
     */
    public PredictionResult {
        if (predictionText == null || predictionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Prediction text cannot be null or empty");
        }
        
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + rating);
        }
        
        if (confidenceScore == null) {
            confidenceScore = BigDecimal.ZERO;
        } else if (confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0, got: " + confidenceScore);
        }
        
        // Sanitize text fields
        predictionText = sanitizeText(predictionText);
        predictionType = sanitizeText(predictionType);
        context = sanitizeText(context);
        timeframe = sanitizeText(timeframe);
        subject = sanitizeText(subject);
    }
    
    /**
     * Creates a simple PredictionResult with just text and rating.
     * 
     * @param predictionText the prediction text
     * @param rating the rating (1-5)
     * @return new PredictionResult
     */
    public static PredictionResult simple(String predictionText, Integer rating) {
        return new PredictionResult(predictionText, null, rating, null, null, null, null);
    }
    
    /**
     * Creates a PredictionResult with text, type, and rating.
     * 
     * @param predictionText the prediction text
     * @param predictionType the prediction type
     * @param rating the rating (1-5)
     * @return new PredictionResult
     */
    public static PredictionResult withType(String predictionText, String predictionType, Integer rating) {
        return new PredictionResult(predictionText, predictionType, rating, null, null, null, null);
    }
    
    /**
     * Creates a PredictionResult with full metadata.
     * 
     * @param predictionText the prediction text
     * @param predictionType the prediction type
     * @param rating the rating (1-5)
     * @param confidenceScore the confidence score (0.0-1.0)
     * @param context the context text
     * @return new PredictionResult
     */
    public static PredictionResult full(String predictionText, String predictionType, Integer rating, 
                                       BigDecimal confidenceScore, String context) {
        return new PredictionResult(predictionText, predictionType, rating, confidenceScore, context, null, null);
    }
    
    /**
     * Gets the star rating as a formatted string.
     * 
     * @return star rating string (e.g., "★★★☆☆")
     */
    public String getStarRating() {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }
        return stars.toString();
    }
    
    /**
     * Gets the confidence score as a percentage.
     * 
     * @return confidence percentage (0-100), or 0.0 if no confidence score
     */
    public Double getConfidencePercentage() {
        if (confidenceScore == null) {
            return 0.0;
        }
        return confidenceScore.multiply(BigDecimal.valueOf(100)).doubleValue();
    }
    
    /**
     * Checks if this is a high-confidence prediction (>= 0.7).
     * 
     * @return true if confidence score >= 0.7
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(BigDecimal.valueOf(0.7)) >= 0;
    }
    
    /**
     * Checks if this is a high-rated prediction (>= 4 stars).
     * 
     * @return true if rating >= 4
     */
    public boolean isHighRated() {
        return rating >= 4;
    }
    
    /**
     * Sanitizes text input to prevent security issues.
     * 
     * @param text the text to sanitize
     * @return sanitized text or null if input was null
     */
    private static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        
        String sanitized = text.trim();
        
        // Basic HTML escaping to prevent XSS
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        return sanitized.isEmpty() ? null : sanitized;
    }
}