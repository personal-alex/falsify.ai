package ai.falsify.crawlers.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * DTO representing the status of a crawler job for API responses.
 * Contains job execution information and progress details.
 */
public class JobStatus {
    
    @NotNull
    public String jobId;
    
    @NotNull
    public String crawlerId;
    
    @NotNull
    public JobRecord.JobStatus status;
    
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant endTime;
    
    public int articlesProcessed;
    
    public int articlesSkipped;
    
    public int articlesFailed;
    
    public String errorMessage;
    
    public String requestId;
    
    public String currentActivity;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant lastUpdated;
    
    public Long durationMs;
    
    public long elapsedTimeMs;
    
    public double successRate;
    
    public int totalArticlesAttempted;
    
    public JobStatus() {
        // Default constructor for Jackson
    }
    
    public JobStatus(JobRecord jobRecord) {
        this.jobId = jobRecord.jobId;
        this.crawlerId = jobRecord.crawlerId;
        this.status = jobRecord.status;
        this.startTime = jobRecord.startTime;
        this.endTime = jobRecord.endTime;
        this.articlesProcessed = jobRecord.articlesProcessed;
        this.articlesSkipped = jobRecord.articlesSkipped;
        this.articlesFailed = jobRecord.articlesFailed;
        this.errorMessage = jobRecord.errorMessage;
        this.requestId = jobRecord.requestId;
        this.currentActivity = jobRecord.currentActivity;
        this.lastUpdated = jobRecord.lastUpdated;
        this.durationMs = jobRecord.getDurationMs();
        this.elapsedTimeMs = jobRecord.getElapsedTimeMs();
        this.successRate = jobRecord.getSuccessRate();
        this.totalArticlesAttempted = jobRecord.getTotalArticlesAttempted();
    }
    
    /**
     * Creates a JobStatus from a JobRecord entity.
     */
    public static JobStatus fromJobRecord(JobRecord jobRecord) {
        return new JobStatus(jobRecord);
    }
    
    /**
     * Checks if the job is currently running.
     */
    public boolean isRunning() {
        return status == JobRecord.JobStatus.RUNNING;
    }
    
    /**
     * Checks if the job has completed (successfully, failed, or cancelled).
     */
    public boolean isCompleted() {
        return status != JobRecord.JobStatus.RUNNING;
    }
    
    /**
     * Gets a human-readable duration string.
     */
    public String getDurationString() {
        long duration = durationMs != null ? durationMs : elapsedTimeMs;
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    @Override
    public String toString() {
        return "JobStatus{" +
                "jobId='" + jobId + '\'' +
                ", crawlerId='" + crawlerId + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", articlesProcessed=" + articlesProcessed +
                ", currentActivity='" + currentActivity + '\'' +
                ", elapsedTimeMs=" + elapsedTimeMs +
                '}';
    }
}