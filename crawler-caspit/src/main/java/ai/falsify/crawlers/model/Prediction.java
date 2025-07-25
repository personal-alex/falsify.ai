package ai.falsify.crawlers.model;

public record Prediction(
        String text,
        String timeframe,
        String subject,
        double confidence
) {}