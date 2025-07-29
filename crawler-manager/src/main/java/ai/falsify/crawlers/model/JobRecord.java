package ai.falsify.crawlers.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Entity representing a crawler job record in the database.
 * Tracks the lifecycle and results of crawl operations.
 */
@Entity
@Table(name = "crawler_jobs")
public class JobRecord extends PanacheEntity {
    
    public enum JobStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    @NotNull
    @Column(name = "crawler_id", nullable = false)
    public String crawlerId;
    
    @NotNull
    @Column(name = "job_id", nullable = false, unique = true)
    public String jobId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public JobStatus status;
    
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "start_time", nullable = false)
    public Instant startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "end_time")
    public Instant endTime;
    
    @Column(name = "articles_processed", nullable = false)
    public int articlesProcessed = 0;
    
    @Column(name = "articles_skipped", nullable = false)
    public int articlesSkipped = 0;
    
    @Column(name = "articles_failed", nullable = false)
    public int articlesFailed = 0;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
    
    @Column(name = "request_id")
    public String requestId;
    
    @Column(name = "estimated_duration")
    public String estimatedDuration;
    
    @Column(name = "current_activity")
    public String currentActivity;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "last_updated")
    public Instant lastUpdated;
    
    public JobRecord() {
        // Default constructor for Panache
    }
    
    public JobRecord(String crawlerId, String jobId, String requestId) {
        this.crawlerId = crawlerId;
        this.jobId = jobId;
        this.requestId = requestId;
        this.status = JobStatus.RUNNING;
        this.startTime = Instant.now();
        this.lastUpdated = Instant.now();
        this.currentActivity = "Starting crawl operation";
    }
    
    /**
     * Calculates the duration of the job in milliseconds.
     * Returns null if the job hasn't ended yet.
     */
    public Long getDurationMs() {
        if (endTime == null) {
            return null;
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    /**
     * Calculates the current elapsed time in milliseconds.
     */
    public long getElapsedTimeMs() {
        Instant now = Instant.now();
        return now.toEpochMilli() - startTime.toEpochMilli();
    }
    
    /**
     * Updates the job progress with new metrics.
     */
    public void updateProgress(int articlesProcessed, int articlesSkipped, int articlesFailed, String currentActivity) {
        this.articlesProcessed = articlesProcessed;
        this.articlesSkipped = articlesSkipped;
        this.articlesFailed = articlesFailed;
        this.currentActivity = currentActivity;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Marks the job as completed successfully.
     */
    public void markCompleted(int finalArticlesProcessed, int finalArticlesSkipped, int finalArticlesFailed) {
        this.status = JobStatus.COMPLETED;
        this.endTime = Instant.now();
        this.lastUpdated = Instant.now();
        this.articlesProcessed = finalArticlesProcessed;
        this.articlesSkipped = finalArticlesSkipped;
        this.articlesFailed = finalArticlesFailed;
        this.currentActivity = "Completed successfully";
    }
    
    /**
     * Marks the job as failed with an error message.
     */
    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.endTime = Instant.now();
        this.lastUpdated = Instant.now();
        this.errorMessage = errorMessage;
        this.currentActivity = "Failed";
    }
    
    /**
     * Marks the job as cancelled.
     */
    public void markCancelled() {
        this.status = JobStatus.CANCELLED;
        this.endTime = Instant.now();
        this.lastUpdated = Instant.now();
        this.currentActivity = "Cancelled";
    }
    
    /**
     * Checks if the job is currently running.
     */
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }
    
    /**
     * Checks if the job has completed (successfully, failed, or cancelled).
     */
    public boolean isCompleted() {
        return status != JobStatus.RUNNING;
    }
    
    /**
     * Gets the total number of articles processed (including failed ones).
     */
    public int getTotalArticlesAttempted() {
        return articlesProcessed + articlesSkipped + articlesFailed;
    }
    
    /**
     * Calculates the success rate as a percentage.
     */
    public double getSuccessRate() {
        int total = getTotalArticlesAttempted();
        if (total == 0) {
            return 0.0;
        }
        return (double) articlesProcessed / total * 100.0;
    }
    
    /**
     * Finds a job record by job ID.
     */
    public static JobRecord findByJobId(String jobId) {
        return find("jobId", jobId).firstResult();
    }
    
    @Override
    public String toString() {
        return "JobRecord{" +
                "id=" + id +
                ", crawlerId='" + crawlerId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", articlesProcessed=" + articlesProcessed +
                ", articlesSkipped=" + articlesSkipped +
                ", articlesFailed=" + articlesFailed +
                ", requestId='" + requestId + '\'' +
                ", currentActivity='" + currentActivity + '\'' +
                '}';
    }
}