package ai.falsify.prediction.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Placeholder interface for LangChain4j integration.
 * This interface defines the contract for LLM operations that will be
 * implemented when LangChain4j dependencies are added to the project.
 * 
 * The interface is designed to support various LLM providers and operations
 * including text generation, embeddings, and structured data extraction.
 */
public interface LangChain4jClient {

    /**
     * Generates text using the configured LLM.
     * 
     * @param request the LLM request containing prompt and parameters
     * @return the LLM response
     * @throws LLMException if the request fails
     */
    LLMResponse generate(LLMRequest request) throws LLMException;
    
    /**
     * Generates text for multiple requests in a batch (if supported by provider).
     * 
     * @param requests list of LLM requests to process in batch
     * @return list of LLM responses corresponding to each request
     * @throws LLMException if the batch request fails
     */
    List<LLMResponse> generateBatch(List<LLMRequest> requests) throws LLMException;

    /**
     * Generates text asynchronously using the configured LLM.
     * 
     * @param request the LLM request containing prompt and parameters
     * @return CompletableFuture containing the LLM response
     */
    CompletableFuture<LLMResponse> generateAsync(LLMRequest request);
    
    /**
     * Generates text for multiple requests asynchronously in a batch.
     * 
     * @param requests list of LLM requests to process in batch
     * @return CompletableFuture containing list of LLM responses
     */
    CompletableFuture<List<LLMResponse>> generateBatchAsync(List<LLMRequest> requests);

    /**
     * Generates embeddings for the given text.
     * 
     * @param text the text to generate embeddings for
     * @return the embedding vector
     * @throws LLMException if the request fails
     */
    List<Double> generateEmbeddings(String text) throws LLMException;

    /**
     * Extracts structured data from text using the LLM.
     * 
     * @param text   the text to analyze
     * @param schema the expected output schema
     * @return structured data as a map
     * @throws LLMException if the extraction fails
     */
    Map<String, Object> extractStructuredData(String text, String schema) throws LLMException;

    /**
     * Checks if the LLM client is available and properly configured.
     * 
     * @return true if the client is ready for use
     */
    boolean isAvailable();
    
    /**
     * Checks if batch processing is supported by the current provider.
     * 
     * @return true if batch processing is supported
     */
    boolean isBatchProcessingSupported();

    /**
     * Gets the current configuration of the LLM client.
     * 
     * @return configuration information
     */
    Map<String, Object> getConfiguration();

    /**
     * Gets usage statistics for the LLM client.
     * 
     * @return usage statistics
     */
    LLMUsageStats getUsageStats();

    /**
     * Represents an LLM request with all necessary parameters.
     */
    class LLMRequest {
        private final String prompt;
        private final String model;
        private final int maxTokens;
        private final double temperature;
        private final double topP;
        private final Duration timeout;
        private final Map<String, Object> additionalParameters;

        private LLMRequest(Builder builder) {
            this.prompt = builder.prompt;
            this.model = builder.model;
            this.maxTokens = builder.maxTokens;
            this.temperature = builder.temperature;
            this.topP = builder.topP;
            this.timeout = builder.timeout;
            this.additionalParameters = Map.copyOf(builder.additionalParameters);
        }

        public String getPrompt() {
            return prompt;
        }

        public String getModel() {
            return model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getTopP() {
            return topP;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public Map<String, Object> getAdditionalParameters() {
            return additionalParameters;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String prompt;
            private String model = "gpt-3.5-turbo";
            private int maxTokens = 1000;
            private double temperature = 0.3;
            private double topP = 1.0;
            private Duration timeout = Duration.ofSeconds(30);
            private Map<String, Object> additionalParameters = new java.util.HashMap<>();

            public Builder prompt(String prompt) {
                this.prompt = prompt;
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder topP(double topP) {
                this.topP = topP;
                return this;
            }

            public Builder timeout(Duration timeout) {
                this.timeout = timeout;
                return this;
            }

            public Builder additionalParameter(String key, Object value) {
                this.additionalParameters.put(key, value);
                return this;
            }

            public LLMRequest build() {
                if (prompt == null || prompt.trim().isEmpty()) {
                    throw new IllegalArgumentException("Prompt cannot be null or empty");
                }
                return new LLMRequest(this);
            }
        }
    }

    /**
     * Represents an LLM response with metadata.
     */
    class LLMResponse {
        private final String text;
        private final String model;
        private final int tokensUsed;
        private final Duration processingTime;
        private final Map<String, Object> metadata;

        public LLMResponse(String text, String model, int tokensUsed, Duration processingTime,
                Map<String, Object> metadata) {
            this.text = text;
            this.model = model;
            this.tokensUsed = tokensUsed;
            this.processingTime = processingTime;
            this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public String getText() {
            return text;
        }

        public String getModel() {
            return model;
        }

        public int getTokensUsed() {
            return tokensUsed;
        }

        public Duration getProcessingTime() {
            return processingTime;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Represents usage statistics for the LLM client.
     */
    class LLMUsageStats {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final long totalTokensUsed;
        private final double totalCost;
        private final Duration averageResponseTime;

        public LLMUsageStats(long totalRequests, long successfulRequests, long failedRequests,
                long totalTokensUsed, double totalCost, Duration averageResponseTime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.totalTokensUsed = totalTokensUsed;
            this.totalCost = totalCost;
            this.averageResponseTime = averageResponseTime;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public long getSuccessfulRequests() {
            return successfulRequests;
        }

        public long getFailedRequests() {
            return failedRequests;
        }

        public long getTotalTokensUsed() {
            return totalTokensUsed;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public Duration getAverageResponseTime() {
            return averageResponseTime;
        }

        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "totalRequests", totalRequests,
                    "successfulRequests", successfulRequests,
                    "failedRequests", failedRequests,
                    "successRate", getSuccessRate(),
                    "totalTokensUsed", totalTokensUsed,
                    "totalCost", totalCost,
                    "averageResponseTimeMs", averageResponseTime.toMillis());
        }
    }

    /**
     * Exception thrown when LLM operations fail.
     */
    class LLMException extends Exception {
        private final String errorCode;
        private final Map<String, Object> details;

        public LLMException(String message) {
            super(message);
            this.errorCode = "UNKNOWN";
            this.details = Map.of();
        }

        public LLMException(String message, Throwable cause) {
            super(message, cause);
            this.errorCode = "UNKNOWN";
            this.details = Map.of();
        }

        public LLMException(String message, String errorCode, Map<String, Object> details) {
            super(message);
            this.errorCode = errorCode;
            this.details = details != null ? Map.copyOf(details) : Map.of();
        }

        public LLMException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.details = details != null ? Map.copyOf(details) : Map.of();
        }

        public String getErrorCode() {
            return errorCode;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}