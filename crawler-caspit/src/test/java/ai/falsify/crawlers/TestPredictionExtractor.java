package ai.falsify.crawlers;

import ai.falsify.crawlers.model.Prediction;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test implementation of PredictionExtractor for integration testing.
 * Provides controllable behavior for testing various scenarios including
 * successful prediction extraction, failures, and edge cases.
 */
@Mock
@ApplicationScoped
public class TestPredictionExtractor implements PredictionExtractor {

    private List<Prediction> predictionsToReturn = Collections.emptyList();
    private boolean shouldThrowException = false;
    private boolean extractPredictionsCalled = false;
    private int extractPredictionsCallCount = 0;
    private List<String> extractedTexts = new ArrayList<>();

    @Override
    public List<Prediction> extractPredictions(String articleText) {
        extractPredictionsCalled = true;
        extractPredictionsCallCount++;
        extractedTexts.add(articleText);

        if (shouldThrowException) {
            throw new RuntimeException("Test AI service failure");
        }

        return new ArrayList<>(predictionsToReturn);
    }

    // Test control methods

    public void setPredictionsToReturn(List<Prediction> predictions) {
        this.predictionsToReturn = predictions != null ? predictions : Collections.emptyList();
    }

    public void setShouldThrowException(boolean shouldThrow) {
        this.shouldThrowException = shouldThrow;
    }

    public boolean wasExtractPredictionsCalled() {
        return extractPredictionsCalled;
    }

    public int getExtractPredictionsCallCount() {
        return extractPredictionsCallCount;
    }

    public List<String> getExtractedTexts() {
        return new ArrayList<>(extractedTexts);
    }

    public void reset() {
        predictionsToReturn = Collections.emptyList();
        shouldThrowException = false;
        extractPredictionsCalled = false;
        extractPredictionsCallCount = 0;
        extractedTexts.clear();
    }
}