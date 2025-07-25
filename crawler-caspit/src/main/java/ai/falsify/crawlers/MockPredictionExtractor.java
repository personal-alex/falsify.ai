package ai.falsify.crawlers;

import ai.falsify.crawlers.model.Prediction;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.List;

/**
 * Mock implementation of PredictionExtractor for testing purposes.
 * This is a temporary implementation to allow configuration testing.
 */
@ApplicationScoped
public class MockPredictionExtractor implements PredictionExtractor {

    @Override
    public List<Prediction> extractPredictions(String articleText) {
        // Return empty list for testing - actual implementation would use AI
        return Collections.emptyList();
    }
}