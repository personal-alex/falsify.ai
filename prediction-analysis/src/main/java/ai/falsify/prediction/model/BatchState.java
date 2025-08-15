package ai.falsify.prediction.model;

/**
 * Enumeration of batch job states for GenAI batch processing.
 * Represents the lifecycle states of a batch job from submission to completion.
 */
public enum BatchState {
    /**
     * Batch job has been submitted to the GenAI API but not yet started processing.
     */
    SUBMITTED,
    
    /**
     * Batch job is currently being processed by the GenAI API.
     */
    PROCESSING,
    
    /**
     * Batch job has completed successfully with all results available.
     */
    COMPLETED,
    
    /**
     * Batch job has failed due to an error during processing.
     */
    FAILED,
    
    /**
     * Batch job was cancelled before completion.
     */
    CANCELLED,
    
    /**
     * Batch job exceeded the configured timeout period.
     */
    TIMEOUT;
    
    /**
     * Checks if this state represents a terminal state (job is finished).
     * 
     * @return true if the job is in a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
    
    /**
     * Checks if this state represents a successful completion.
     * 
     * @return true if the job completed successfully
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Checks if this state represents an active processing state.
     * 
     * @return true if the job is currently being processed
     */
    public boolean isActive() {
        return this == SUBMITTED || this == PROCESSING;
    }
    
    /**
     * Checks if this state represents a failure state.
     * 
     * @return true if the job failed, was cancelled, or timed out
     */
    public boolean isFailure() {
        return this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
}