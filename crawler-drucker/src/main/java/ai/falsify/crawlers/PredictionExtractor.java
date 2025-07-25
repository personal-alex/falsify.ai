package ai.falsify.crawlers;

import ai.falsify.crawlers.model.Prediction;
import java.util.List;

public interface PredictionExtractor {
    List<Prediction> extractPredictions(String articleText);
}