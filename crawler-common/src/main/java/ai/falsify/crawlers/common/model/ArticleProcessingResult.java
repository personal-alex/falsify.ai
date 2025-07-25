package ai.falsify.crawlers.common.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of processing a single article during crawling.
 */
public record ArticleProcessingResult(
    String url,
    boolean success,
    String errorMessage,
    long processingTimeMs,
    int retryCount,
    Instant startTime,
    Instant endTime,
    ProcessingStage failedStage
) {
    
    /**
     * Enumeration of processing stages where failures can occur.
     */
    public enum ProcessingStage {
        FETCH,
        PARSE,
        VALIDATE,
        PERSIST,
        COMPLETE
    }
    
    /**
     * Gets the processing duration.
     * 
     * @return Duration of the article processing
     */
    public Duration getProcessingDuration() {
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Creates a successful processing result.
     * 
     * @param url the article URL
     * @param startTime when processing started
     * @param endTime when processing ended
     * @return successful ArticleProcessingResult
     */
    public static ArticleProcessingResult success(String url, Instant startTime, Instant endTime) {
        return new ArticleProcessingResult(
            url,
            true,
            null,
            Duration.between(startTime, endTime).toMillis(),
            0,
            startTime,
            endTime,
            ProcessingStage.COMPLETE
        );
    }
    
    /**
     * Creates a successful processing result with retry count.
     * 
     * @param url the article URL
     * @param startTime when processing started
     * @param endTime when processing ended
     * @param retryCount number of retries performed
     * @return successful ArticleProcessingResult
     */
    public static ArticleProcessingResult success(String url, Instant startTime, Instant endTime, int retryCount) {
        return new ArticleProcessingResult(
            url,
            true,
            null,
            Duration.between(startTime, endTime).toMillis(),
            retryCount,
            startTime,
            endTime,
            ProcessingStage.COMPLETE
        );
    }
    
    /**
     * Creates a failed processing result.
     * 
     * @param url the article URL
     * @param errorMessage the error message
     * @param startTime when processing started
     * @param endTime when processing ended
     * @param failedStage the stage where processing failed
     * @return failed ArticleProcessingResult
     */
    public static ArticleProcessingResult failure(String url, String errorMessage, Instant startTime, Instant endTime, ProcessingStage failedStage) {
        return new ArticleProcessingResult(
            url,
            false,
            errorMessage,
            Duration.between(startTime, endTime).toMillis(),
            0,
            startTime,
            endTime,
            failedStage
        );
    }
    
    /**
     * Creates a failed processing result with retry count.
     * 
     * @param url the article URL
     * @param errorMessage the error message
     * @param startTime when processing started
     * @param endTime when processing ended
     * @param failedStage the stage where processing failed
     * @param retryCount number of retries performed
     * @return failed ArticleProcessingResult
     */
    public static ArticleProcessingResult failure(String url, String errorMessage, Instant startTime, Instant endTime, ProcessingStage failedStage, int retryCount) {
        return new ArticleProcessingResult(
            url,
            false,
            errorMessage,
            Duration.between(startTime, endTime).toMillis(),
            retryCount,
            startTime,
            endTime,
            failedStage
        );
    }
}