package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a prediction analysis job.
 * Tracks the lifecycle and progress of prediction extraction jobs.
 */
@Entity
@Table(name = "analysis_jobs", indexes = {
    @Index(name = "idx_analysis_job_id", columnList = "job_id"),
    @Index(name = "idx_analysis_status", columnList = "status"),
    @Index(name = "idx_analysis_started_at", columnList = "started_at")
})
public class AnalysisJobEntity extends PanacheEntity {

    @NotBlank(message = "Job ID cannot be empty")
    @Size(max = 100, message = "Job ID cannot exceed 100 characters")
    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    public String jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public AnalysisStatus status;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "total_articles")
    public Integer totalArticles;

    @Column(name = "processed_articles")
    public Integer processedArticles;

    @Column(name = "predictions_found")
    public Integer predictionsFound;

    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    @Size(max = 50, message = "Analysis type cannot exceed 50 characters")
    @Column(name = "analysis_type", length = 50)
    public String analysisType; // "mock" or "llm"

    // Relationships
    @OneToMany(mappedBy = "analysisJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    public List<PredictionInstanceEntity> predictionInstances = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "analysis_job_articles",
        joinColumns = @JoinColumn(name = "analysis_job_id"),
        inverseJoinColumns = @JoinColumn(name = "article_id")
    )
    public List<ArticleEntity> analyzedArticles = new ArrayList<>();

    /**
     * Default constructor for JPA.
     */
    public AnalysisJobEntity() {
        this.jobId = UUID.randomUUID().toString();
        this.status = AnalysisStatus.PENDING;
        this.startedAt = Instant.now();
        this.totalArticles = 0;
        this.processedArticles = 0;
        this.predictionsFound = 0;
        this.analysisType = "mock";
    }

    /**
     * Constructor with analysis type.
     * 
     * @param analysisType the type of analysis ("mock" or "llm")
     */
    public AnalysisJobEntity(String analysisType) {
        this();
        this.analysisType = analysisType != null ? analysisType : "mock";
    }

    /**
     * Finds an analysis job by its job ID.
     * 
     * @param jobId the job ID
     * @return the AnalysisJobEntity or null if not found
     */
    public static AnalysisJobEntity findByJobId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return null;
        }
        return find("jobId", jobId.trim()).firstResult();
    }

    /**
     * Finds analysis jobs by status.
     * 
     * @param status the analysis status
     * @return list of jobs with the specified status
     */
    public static List<AnalysisJobEntity> findByStatus(AnalysisStatus status) {
        if (status == null) {
            return List.of();
        }
        return list("status", status);
    }

    /**
     * Finds recent analysis jobs ordered by start time.
     * 
     * @param limit the maximum number of jobs to return
     * @return list of recent analysis jobs
     */
    public static List<AnalysisJobEntity> findRecent(int limit) {
        return find("ORDER BY startedAt DESC").page(0, limit).list();
    }

    /**
     * Counts jobs by status.
     * 
     * @param status the analysis status
     * @return count of jobs with the specified status
     */
    public static long countByStatus(AnalysisStatus status) {
        if (status == null) {
            return 0;
        }
        return count("status", status);
    }

    /**
     * Marks the job as started.
     */
    public void markStarted() {
        this.status = AnalysisStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    /**
     * Marks the job as completed successfully.
     */
    public void markCompleted() {
        this.status = AnalysisStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Marks the job as failed with an error message.
     * 
     * @param errorMessage the error message
     */
    public void markFailed(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Marks the job as cancelled.
     */
    public void markCancelled() {
        this.status = AnalysisStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * Updates the progress of the job.
     * 
     * @param processedArticles number of articles processed
     * @param predictionsFound number of predictions found
     */
    public void updateProgress(int processedArticles, int predictionsFound) {
        this.processedArticles = processedArticles;
        this.predictionsFound = predictionsFound;
    }

    /**
     * Calculates the progress percentage.
     * 
     * @return progress percentage (0-100)
     */
    public double getProgressPercentage() {
        if (totalArticles == null || totalArticles == 0) {
            return 0.0;
        }
        if (processedArticles == null) {
            return 0.0;
        }
        return Math.min(100.0, (processedArticles.doubleValue() / totalArticles.doubleValue()) * 100.0);
    }

    /**
     * Checks if the job is in a terminal state.
     * 
     * @return true if the job is completed, failed, or cancelled
     */
    public boolean isTerminal() {
        return status == AnalysisStatus.COMPLETED || 
               status == AnalysisStatus.FAILED || 
               status == AnalysisStatus.CANCELLED;
    }

    /**
     * Gets the duration of the job in milliseconds.
     * 
     * @return duration in milliseconds, or null if not completed
     */
    public Long getDurationMillis() {
        if (startedAt == null) {
            return null;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.toEpochMilli() - startedAt.toEpochMilli();
    }

    /**
     * Deletes old completed analysis jobs older than the specified number of days.
     * This method helps maintain database performance by removing old job records.
     * 
     * @param daysOld the number of days old jobs should be to be considered for deletion
     * @return the number of jobs deleted
     */
    public static long deleteOldJobs(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("Days old must be positive");
        }
        
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24 * 60 * 60L);
        
        // Only delete terminal jobs (completed, failed, or cancelled) that are older than cutoff
        return delete("(status = ?1 OR status = ?2 OR status = ?3) AND completedAt < ?4", 
                     AnalysisStatus.COMPLETED, AnalysisStatus.FAILED, AnalysisStatus.CANCELLED, cutoffDate);
    }

    /**
     * Deletes old completed analysis jobs older than 30 days by default.
     * 
     * @return the number of jobs deleted
     */
    public static long deleteOldJobs() {
        return deleteOldJobs(30); // Default to 30 days
    }

    /**
     * Finds jobs that are eligible for cleanup (old terminal jobs).
     * 
     * @param daysOld the number of days old jobs should be to be considered for cleanup
     * @return list of jobs eligible for cleanup
     */
    public static List<AnalysisJobEntity> findJobsEligibleForCleanup(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("Days old must be positive");
        }
        
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24 * 60 * 60L);
        
        return list("(status = ?1 OR status = ?2 OR status = ?3) AND completedAt < ?4 ORDER BY completedAt ASC", 
                   AnalysisStatus.COMPLETED, AnalysisStatus.FAILED, AnalysisStatus.CANCELLED, cutoffDate);
    }

    /**
     * Counts jobs that are eligible for cleanup.
     * 
     * @param daysOld the number of days old jobs should be to be considered for cleanup
     * @return count of jobs eligible for cleanup
     */
    public static long countJobsEligibleForCleanup(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("Days old must be positive");
        }
        
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24 * 60 * 60L);
        
        return count("(status = ?1 OR status = ?2 OR status = ?3) AND completedAt < ?4", 
                    AnalysisStatus.COMPLETED, AnalysisStatus.FAILED, AnalysisStatus.CANCELLED, cutoffDate);
    }

    @Override
    public String toString() {
        return "AnalysisJobEntity{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", status=" + status +
                ", analysisType='" + analysisType + '\'' +
                ", totalArticles=" + totalArticles +
                ", processedArticles=" + processedArticles +
                ", predictionsFound=" + predictionsFound +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}