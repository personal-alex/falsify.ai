package ai.falsify.prediction.service;

import ai.falsify.prediction.model.PredictionResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of PredictionExtractor for testing and demonstration purposes.
 * Generates realistic sample predictions based on keyword analysis and configurable patterns.
 */
@ApplicationScoped
public class MockPredictionExtractor implements PredictionExtractor {
    
    private static final Logger LOG = Logger.getLogger(MockPredictionExtractor.class);
    
    @ConfigProperty(name = "prediction.mock.enabled", defaultValue = "true")
    boolean mockEnabled;
    
    @ConfigProperty(name = "prediction.mock.processing-delay-ms", defaultValue = "500")
    int processingDelayMs;
    
    @ConfigProperty(name = "prediction.mock.min-predictions", defaultValue = "1")
    int minPredictions;
    
    @ConfigProperty(name = "prediction.mock.max-predictions", defaultValue = "5")
    int maxPredictions;
    
    @ConfigProperty(name = "prediction.mock.min-confidence", defaultValue = "0.3")
    double minConfidence;
    
    @ConfigProperty(name = "prediction.mock.max-confidence", defaultValue = "0.95")
    double maxConfidence;
    
    // Keyword patterns for different prediction types
    private static final Map<String, List<String>> PREDICTION_KEYWORDS = Map.of(
        "political", Arrays.asList(
            "election", "vote", "campaign", "government", "policy", "minister", "parliament", 
            "coalition", "opposition", "democracy", "legislation", "reform", "budget"
        ),
        "economic", Arrays.asList(
            "economy", "market", "inflation", "recession", "growth", "gdp", "unemployment", 
            "interest", "bank", "finance", "investment", "stock", "currency", "trade"
        ),
        "sports", Arrays.asList(
            "team", "player", "match", "game", "season", "championship", "tournament", 
            "coach", "score", "victory", "defeat", "league", "competition"
        ),
        "technology", Arrays.asList(
            "technology", "software", "ai", "artificial intelligence", "innovation", "startup", 
            "digital", "internet", "computer", "mobile", "app", "platform", "data"
        ),
        "social", Arrays.asList(
            "society", "community", "social", "culture", "education", "health", "environment", 
            "climate", "protest", "movement", "rights", "justice", "equality"
        )
    );
    
    // Prediction templates for different types
    private static final Map<String, List<String>> PREDICTION_TEMPLATES = Map.of(
        "political", Arrays.asList(
            "The upcoming election will likely result in {outcome}",
            "Government policy on {subject} is expected to change within {timeframe}",
            "Coalition negotiations will {outcome} by {timeframe}",
            "The opposition's stance on {subject} will {outcome}",
            "Parliamentary approval for {subject} is {outcome}"
        ),
        "economic", Arrays.asList(
            "Market conditions suggest {outcome} in the {timeframe}",
            "Economic indicators point to {outcome} by {timeframe}",
            "Interest rates are likely to {outcome} within {timeframe}",
            "The {subject} sector will experience {outcome}",
            "Inflation is expected to {outcome} over the {timeframe}"
        ),
        "sports", Arrays.asList(
            "The team's performance indicates {outcome} this {timeframe}",
            "{subject} will likely {outcome} in the upcoming {timeframe}",
            "Based on current form, {outcome} is expected",
            "The championship race will {outcome} by {timeframe}",
            "Player transfers will {outcome} the team's chances"
        ),
        "technology", Arrays.asList(
            "Technological advances in {subject} will {outcome} within {timeframe}",
            "The {subject} market is expected to {outcome}",
            "Innovation in {subject} will likely {outcome}",
            "Digital transformation will {outcome} by {timeframe}",
            "AI development in {subject} will {outcome}"
        ),
        "social", Arrays.asList(
            "Social trends indicate {outcome} in {timeframe}",
            "Community response to {subject} will {outcome}",
            "Environmental policies will {outcome} by {timeframe}",
            "Educational reforms are expected to {outcome}",
            "Healthcare improvements will {outcome} within {timeframe}"
        )
    );
    
    // Outcome variations
    private static final List<String> OUTCOMES = Arrays.asList(
        "succeed", "fail", "improve significantly", "face challenges", "show mixed results",
        "exceed expectations", "fall short", "stabilize", "experience volatility", "gain momentum"
    );
    
    // Timeframe variations
    private static final List<String> TIMEFRAMES = Arrays.asList(
        "next quarter", "coming months", "next year", "short term", "medium term",
        "next election cycle", "current season", "upcoming period", "near future", "next phase"
    );
    
    // Subject variations by type
    private static final Map<String, List<String>> SUBJECTS = Map.of(
        "political", Arrays.asList("healthcare", "education", "defense", "taxation", "immigration"),
        "economic", Arrays.asList("housing", "employment", "trade", "manufacturing", "services"),
        "sports", Arrays.asList("football", "basketball", "tennis", "athletics", "swimming"),
        "technology", Arrays.asList("cybersecurity", "cloud computing", "mobile apps", "blockchain", "IoT"),
        "social", Arrays.asList("climate change", "social media", "urban planning", "public health", "diversity")
    );
    
    @Override
    public List<PredictionResult> extractPredictions(String articleText, String articleTitle) {
        if (!mockEnabled) {
            LOG.debug("Mock prediction extraction is disabled");
            return Collections.emptyList();
        }
        
        if (articleText == null || articleText.trim().isEmpty()) {
            LOG.debug("Article text is empty, no predictions to extract");
            return Collections.emptyList();
        }
        
        String titlePreview = articleTitle != null ? articleTitle.substring(0, Math.min(50, articleTitle.length())) : "Untitled";
        LOG.debug("Starting mock prediction extraction for article: " + titlePreview);
        
        // Simulate processing time
        simulateProcessingDelay();
        
        // Analyze content to determine prediction types and count
        String combinedText = (articleTitle != null ? articleTitle + " " : "") + articleText;
        List<String> detectedTypes = detectPredictionTypes(combinedText.toLowerCase());
        
        if (detectedTypes.isEmpty()) {
            // If no specific types detected, use a random type
            detectedTypes = Arrays.asList(getRandomPredictionType());
        }
        
        // Generate predictions
        List<PredictionResult> predictions = new ArrayList<>();
        int predictionCount = ThreadLocalRandom.current().nextInt(minPredictions, maxPredictions + 1);
        
        for (int i = 0; i < predictionCount; i++) {
            String predictionType = detectedTypes.get(i % detectedTypes.size());
            PredictionResult prediction = generatePrediction(predictionType, combinedText);
            if (prediction != null) {
                predictions.add(prediction);
            }
        }
        
        LOG.debug("Generated " + predictions.size() + " mock predictions of types: " + detectedTypes);
        return predictions;
    }
    
    @Override
    public boolean isAvailable() {
        return mockEnabled;
    }
    
    @Override
    public String getExtractorType() {
        return "mock";
    }
    
    @Override
    public String getConfiguration() {
        return String.format("Type: %s, Enabled: %s, Delay: %dms, Predictions: %d-%d, Confidence: %.2f-%.2f",
                getExtractorType(), mockEnabled, processingDelayMs, minPredictions, maxPredictions, 
                minConfidence, maxConfidence);
    }
    
    /**
     * Detects prediction types based on keyword analysis.
     * 
     * @param text the text to analyze (should be lowercase)
     * @return list of detected prediction types
     */
    private List<String> detectPredictionTypes(String text) {
        List<String> detectedTypes = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : PREDICTION_KEYWORDS.entrySet()) {
            String type = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int keywordCount = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    keywordCount++;
                }
            }
            
            // If at least 2 keywords of this type are found, include the type
            if (keywordCount >= 2) {
                detectedTypes.add(type);
            }
        }
        
        return detectedTypes;
    }
    
    /**
     * Generates a single prediction for the given type.
     * 
     * @param predictionType the type of prediction to generate
     * @param sourceText the source text for context extraction
     * @return generated PredictionResult or null if generation fails
     */
    private PredictionResult generatePrediction(String predictionType, String sourceText) {
        try {
            List<String> templates = PREDICTION_TEMPLATES.get(predictionType);
            if (templates == null || templates.isEmpty()) {
                return null;
            }
            
            // Select random template
            String template = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
            
            // Fill template with random values
            String predictionText = fillTemplate(template, predictionType);
            
            // Generate rating (biased towards higher ratings for more realistic results)
            Integer rating = generateRating();
            
            // Generate confidence score
            BigDecimal confidenceScore = generateConfidenceScore();
            
            // Extract context from source text
            String context = extractContext(sourceText, predictionType);
            
            // Generate timeframe and subject
            String timeframe = getRandomTimeframe();
            String subject = getRandomSubject(predictionType);
            
            return new PredictionResult(
                predictionText, 
                predictionType, 
                rating, 
                confidenceScore, 
                context, 
                timeframe, 
                subject
            );
            
        } catch (Exception e) {
            LOG.warn("Failed to generate prediction for type: " + predictionType, e);
            return null;
        }
    }
    
    /**
     * Fills a prediction template with random values.
     * 
     * @param template the template string with placeholders
     * @param predictionType the prediction type
     * @return filled template
     */
    private String fillTemplate(String template, String predictionType) {
        String result = template;
        
        // Replace placeholders
        result = result.replace("{outcome}", getRandomOutcome());
        result = result.replace("{timeframe}", getRandomTimeframe());
        result = result.replace("{subject}", getRandomSubject(predictionType));
        
        return result;
    }
    
    /**
     * Generates a rating with bias towards higher values.
     * 
     * @return rating between 1-5
     */
    private Integer generateRating() {
        // Bias towards higher ratings: 20% chance for 1-2, 30% for 3, 50% for 4-5
        double random = ThreadLocalRandom.current().nextDouble();
        if (random < 0.1) return 1;
        if (random < 0.2) return 2;
        if (random < 0.5) return 3;
        if (random < 0.75) return 4;
        return 5;
    }
    
    /**
     * Generates a confidence score within configured range.
     * 
     * @return confidence score as BigDecimal
     */
    private BigDecimal generateConfidenceScore() {
        double confidence = ThreadLocalRandom.current().nextDouble(minConfidence, maxConfidence);
        return BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Extracts context from source text based on prediction type keywords.
     * 
     * @param sourceText the source text
     * @param predictionType the prediction type
     * @return extracted context or null
     */
    private String extractContext(String sourceText, String predictionType) {
        if (sourceText == null || sourceText.length() < 50) {
            return null;
        }
        
        List<String> keywords = PREDICTION_KEYWORDS.get(predictionType);
        if (keywords == null) {
            return extractRandomContext(sourceText);
        }
        
        // Try to find a sentence containing relevant keywords
        String[] sentences = sourceText.split("[.!?]+");
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase();
            for (String keyword : keywords) {
                if (lowerSentence.contains(keyword.toLowerCase())) {
                    return sentence.trim();
                }
            }
        }
        
        return extractRandomContext(sourceText);
    }
    
    /**
     * Extracts a random context snippet from the source text.
     * 
     * @param sourceText the source text
     * @return random context snippet
     */
    private String extractRandomContext(String sourceText) {
        if (sourceText.length() < 100) {
            return sourceText.substring(0, Math.min(sourceText.length(), 200));
        }
        
        int startPos = ThreadLocalRandom.current().nextInt(0, sourceText.length() - 100);
        int endPos = Math.min(startPos + 200, sourceText.length());
        return sourceText.substring(startPos, endPos).trim();
    }
    
    /**
     * Gets a random prediction type.
     * 
     * @return random prediction type
     */
    private String getRandomPredictionType() {
        List<String> types = new ArrayList<>(PREDICTION_KEYWORDS.keySet());
        return types.get(ThreadLocalRandom.current().nextInt(types.size()));
    }
    
    /**
     * Gets a random outcome.
     * 
     * @return random outcome
     */
    private String getRandomOutcome() {
        return OUTCOMES.get(ThreadLocalRandom.current().nextInt(OUTCOMES.size()));
    }
    
    /**
     * Gets a random timeframe.
     * 
     * @return random timeframe
     */
    private String getRandomTimeframe() {
        return TIMEFRAMES.get(ThreadLocalRandom.current().nextInt(TIMEFRAMES.size()));
    }
    
    /**
     * Gets a random subject for the given prediction type.
     * 
     * @param predictionType the prediction type
     * @return random subject
     */
    private String getRandomSubject(String predictionType) {
        List<String> subjects = SUBJECTS.get(predictionType);
        if (subjects == null || subjects.isEmpty()) {
            return "general topic";
        }
        return subjects.get(ThreadLocalRandom.current().nextInt(subjects.size()));
    }
    
    /**
     * Simulates processing delay to make the mock extraction more realistic.
     */
    private void simulateProcessingDelay() {
        if (processingDelayMs > 0) {
            try {
                // Add some randomness to the delay (±25%)
                int variance = processingDelayMs / 4;
                int actualDelay = processingDelayMs + ThreadLocalRandom.current().nextInt(-variance, variance + 1);
                Thread.sleep(Math.max(0, actualDelay));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.debug("Processing delay interrupted");
            }
        }
    }
}