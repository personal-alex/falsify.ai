package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Entity representing a specific instance of a prediction found in an analysis job.
 * This tracks the relationship between a prediction, an article, and the analysis job
 * that extracted it, along with confidence scores and ratings.
 */
@Entity
@Table(name = "prediction_instances", indexes = {
    @Index(name = "idx_prediction_instance_prediction", columnList = "prediction_id"),
    @Index(name = "idx_prediction_instance_article", columnList = "article_id"),
    @Index(name = "idx_prediction_instance_job", columnList = "analysis_job_id"),
    @Index(name = "idx_prediction_instance_rating", columnList = "rating"),
    @Index(name = "idx_prediction_instance_extracted_at", columnList = "extracted_at")
})
public class PredictionInstanceEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    public PredictionEntity prediction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    public ArticleEntity article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_job_id", nullable = false)
    public AnalysisJobEntity analysisJob;

    @DecimalMin(value = "0.0", message = "Confidence score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be between 0.0 and 1.0")
    @Column(name = "confidence_score", precision = 3, scale = 2)
    public BigDecimal confidenceScore;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Column(name = "rating", nullable = false)
    public Integer rating; // 1-5 stars

    @Column(name = "extracted_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant extractedAt;

    @Column(columnDefinition = "TEXT")
    public String context; // Surrounding text for context

    /**
     * Default constructor for JPA.
     */
    public PredictionInstanceEntity() {
        this.extractedAt = Instant.now();
    }

    /**
     * Constructor with all required fields.
     * 
     * @param prediction the prediction entity
     * @param article the article entity
     * @param analysisJob the analysis job entity
     * @param rating the rating (1-5)
     * @param confidenceScore the confidence score (0.0-1.0)
     * @param context the surrounding context text
     */
    public PredictionInstanceEntity(PredictionEntity prediction, ArticleEntity article, 
                                  AnalysisJobEntity analysisJob, Integer rating, 
                                  BigDecimal confidenceScore, String context) {
        this();
        this.prediction = prediction;
        this.article = article;
        this.analysisJob = analysisJob;
        this.rating = validateRating(rating);
        this.confidenceScore = validateConfidenceScore(confidenceScore);
        this.context = sanitizeContext(context);
    }

    /**
     * Finds prediction instances by prediction.
     * 
     * @param prediction the prediction entity
     * @return list of instances for the specified prediction
     */
    public static List<PredictionInstanceEntity> findByPrediction(PredictionEntity prediction) {
        if (prediction == null) {
            return List.of();
        }
        return list("prediction", prediction);
    }

    /**
     * Finds prediction instances by article.
     * 
     * @param article the article entity
     * @return list of instances for the specified article
     */
    public static List<PredictionInstanceEntity> findByArticle(ArticleEntity article) {
        if (article == null) {
            return List.of();
        }
        return list("article", article);
    }

    /**
     * Finds prediction instances by analysis job.
     * 
     * @param analysisJob the analysis job entity
     * @return list of instances for the specified analysis job
     */
    public static List<PredictionInstanceEntity> findByAnalysisJob(AnalysisJobEntity analysisJob) {
        if (analysisJob == null) {
            return List.of();
        }
        return list("analysisJob", analysisJob);
    }

    /**
     * Finds prediction instances by rating.
     * 
     * @param rating the rating (1-5)
     * @return list of instances with the specified rating
     */
    public static List<PredictionInstanceEntity> findByRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            return List.of();
        }
        return list("rating", rating);
    }

    /**
     * Finds prediction instances with rating greater than or equal to specified value.
     * 
     * @param minRating the minimum rating
     * @return list of instances with rating >= minRating
     */
    public static List<PredictionInstanceEntity> findByMinRating(Integer minRating) {
        if (minRating == null || minRating < 1 || minRating > 5) {
            return List.of();
        }
        return list("rating >= ?1", minRating);
    }

    /**
     * Finds prediction instances with confidence score greater than or equal to specified value.
     * 
     * @param minConfidence the minimum confidence score
     * @return list of instances with confidence >= minConfidence
     */
    public static List<PredictionInstanceEntity> findByMinConfidence(BigDecimal minConfidence) {
        if (minConfidence == null) {
            return List.of();
        }
        return list("confidenceScore >= ?1", minConfidence);
    }

    /**
     * Finds the most recent prediction instances.
     * 
     * @param limit the maximum number of instances to return
     * @return list of recent prediction instances
     */
    public static List<PredictionInstanceEntity> findRecent(int limit) {
        return find("ORDER BY extractedAt DESC").page(0, limit).list();
    }

    /**
     * Finds prediction instances for a specific article and analysis job.
     * 
     * @param article the article entity
     * @param analysisJob the analysis job entity
     * @return list of instances for the specified article and job
     */
    public static List<PredictionInstanceEntity> findByArticleAndJob(ArticleEntity article, AnalysisJobEntity analysisJob) {
        if (article == null || analysisJob == null) {
            return List.of();
        }
        return list("article = ?1 AND analysisJob = ?2", article, analysisJob);
    }

    /**
     * Calculates the average rating for a specific prediction.
     * 
     * @param prediction the prediction entity
     * @return the average rating, or null if no instances exist
     */
    public static Double getAverageRatingForPrediction(PredictionEntity prediction) {
        if (prediction == null) {
            return null;
        }
        return getEntityManager()
            .createQuery("SELECT AVG(pi.rating) FROM PredictionInstanceEntity pi WHERE pi.prediction = :prediction", Double.class)
            .setParameter("prediction", prediction)
            .getSingleResult();
    }

    /**
     * Calculates the average confidence score for a specific prediction.
     * 
     * @param prediction the prediction entity
     * @return the average confidence score, or null if no instances exist
     */
    public static BigDecimal getAverageConfidenceForPrediction(PredictionEntity prediction) {
        if (prediction == null) {
            return null;
        }
        Double result = getEntityManager()
            .createQuery("SELECT AVG(pi.confidenceScore) FROM PredictionInstanceEntity pi WHERE pi.prediction = :prediction", Double.class)
            .setParameter("prediction", prediction)
            .getSingleResult();
        return result != null ? BigDecimal.valueOf(result) : null;
    }

    /**
     * Counts prediction instances by analysis job.
     * 
     * @param analysisJob the analysis job entity
     * @return count of instances for the specified job
     */
    public static long countByAnalysisJob(AnalysisJobEntity analysisJob) {
        if (analysisJob == null) {
            return 0;
        }
        return count("analysisJob", analysisJob);
    }

    /**
     * Counts prediction instances by rating.
     * 
     * @param rating the rating (1-5)
     * @return count of instances with the specified rating
     */
    public static long countByRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            return 0;
        }
        return count("rating", rating);
    }

    /**
     * Gets the star rating as a formatted string.
     * 
     * @return star rating string (e.g., "★★★☆☆")
     */
    public String getStarRating() {
        if (rating == null) {
            return "☆☆☆☆☆";
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }
        return stars.toString();
    }

    /**
     * Gets the confidence score as a percentage.
     * 
     * @return confidence percentage (0-100)
     */
    public Double getConfidencePercentage() {
        if (confidenceScore == null) {
            return null;
        }
        return confidenceScore.multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    /**
     * Validates the rating value.
     * 
     * @param rating the rating to validate
     * @return validated rating
     * @throws IllegalArgumentException if rating is invalid
     */
    private static Integer validateRating(Integer rating) {
        if (rating == null) {
            throw new IllegalArgumentException("Rating cannot be null");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + rating);
        }
        return rating;
    }

    /**
     * Validates the confidence score value.
     * 
     * @param confidenceScore the confidence score to validate
     * @return validated confidence score
     */
    private static BigDecimal validateConfidenceScore(BigDecimal confidenceScore) {
        if (confidenceScore == null) {
            return BigDecimal.ZERO;
        }
        if (confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0, got: " + confidenceScore);
        }
        return confidenceScore;
    }

    /**
     * Sanitizes the context text.
     * 
     * @param context the raw context text
     * @return sanitized context text
     */
    private static String sanitizeContext(String context) {
        if (context == null) {
            return null;
        }
        
        String sanitized = context.trim();
        
        // Basic HTML escaping to prevent XSS
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    @Override
    public String toString() {
        return "PredictionInstanceEntity{" +
                "id=" + id +
                ", prediction=" + (prediction != null ? prediction.id : "null") +
                ", article=" + (article != null ? article.id : "null") +
                ", analysisJob=" + (analysisJob != null ? analysisJob.jobId : "null") +
                ", rating=" + rating +
                ", confidenceScore=" + confidenceScore +
                ", extractedAt=" + extractedAt +
                '}';
    }
}