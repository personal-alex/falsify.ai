package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a prediction extracted from articles.
 * Predictions can be associated with multiple articles through many-to-many relationships.
 */
@Entity
@Table(name = "predictions", indexes = {
    @Index(name = "idx_prediction_text", columnList = "prediction_text"),
    @Index(name = "idx_prediction_type", columnList = "prediction_type"),
    @Index(name = "idx_prediction_created_at", columnList = "created_at")
})
public class PredictionEntity extends PanacheEntity {

    @NotBlank(message = "Prediction text cannot be empty")
    @Column(name = "prediction_text", columnDefinition = "TEXT", nullable = false)
    public String predictionText;

    @Size(max = 50, message = "Prediction type cannot exceed 50 characters")
    @Column(name = "prediction_type", length = 50)
    public String predictionType; // e.g., "political", "economic", "sports"

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant createdAt;

    // Many-to-many relationship with articles
    @ManyToMany(mappedBy = "predictions", fetch = FetchType.LAZY)
    public List<ArticleEntity> articles = new ArrayList<>();

    // One-to-many relationship with prediction instances (analysis results)
    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    public List<PredictionInstanceEntity> instances = new ArrayList<>();

    /**
     * Default constructor for JPA.
     */
    public PredictionEntity() {
        this.createdAt = Instant.now();
    }

    /**
     * Constructor with prediction text and type.
     * 
     * @param predictionText the prediction text
     * @param predictionType the prediction type (optional)
     */
    public PredictionEntity(String predictionText, String predictionType) {
        this();
        this.predictionText = sanitizePredictionText(predictionText);
        this.predictionType = sanitizePredictionType(predictionType);
    }

    /**
     * Finds a prediction by its text.
     * 
     * @param predictionText the prediction text to search for
     * @return the PredictionEntity or null if not found
     */
    public static PredictionEntity findByText(String predictionText) {
        if (predictionText == null || predictionText.trim().isEmpty()) {
            return null;
        }
        return find("predictionText", predictionText.trim()).firstResult();
    }

    /**
     * Finds or creates a prediction with the given text and type.
     * 
     * @param predictionText the prediction text
     * @param predictionType the prediction type (optional)
     * @return existing or newly created PredictionEntity
     */
    @Transactional
    public static PredictionEntity findOrCreate(String predictionText, String predictionType) {
        if (predictionText == null || predictionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Prediction text cannot be null or empty");
        }

        PredictionEntity existing = findByText(predictionText);
        if (existing != null) {
            return existing;
        }

        PredictionEntity newPrediction = new PredictionEntity(predictionText, predictionType);
        newPrediction.persist();
        return newPrediction;
    }

    /**
     * Finds predictions by type.
     * 
     * @param predictionType the prediction type
     * @return list of predictions of the specified type
     */
    public static List<PredictionEntity> findByType(String predictionType) {
        if (predictionType == null || predictionType.trim().isEmpty()) {
            return List.of();
        }
        return list("predictionType", predictionType.trim());
    }

    /**
     * Finds predictions containing specific text (case-insensitive).
     * 
     * @param searchText the text to search for
     * @return list of predictions containing the search text
     */
    public static List<PredictionEntity> findContainingText(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return List.of();
        }
        return list("LOWER(predictionText) LIKE LOWER(?1)", "%" + searchText.trim() + "%");
    }

    /**
     * Gets all distinct prediction types.
     * 
     * @return list of distinct prediction types
     */
    public static List<String> findDistinctTypes() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT p.predictionType FROM PredictionEntity p WHERE p.predictionType IS NOT NULL ORDER BY p.predictionType", String.class)
            .getResultList();
    }

    /**
     * Counts predictions by type.
     * 
     * @param predictionType the prediction type
     * @return count of predictions of the specified type
     */
    public static long countByType(String predictionType) {
        if (predictionType == null || predictionType.trim().isEmpty()) {
            return 0;
        }
        return count("predictionType", predictionType.trim());
    }

    /**
     * Adds an article to this prediction's many-to-many relationship.
     * 
     * @param article the article to associate with this prediction
     */
    public void addArticle(ArticleEntity article) {
        if (article != null && !articles.contains(article)) {
            articles.add(article);
            if (!article.predictions.contains(this)) {
                article.predictions.add(this);
            }
        }
    }

    /**
     * Removes an article from this prediction's many-to-many relationship.
     * 
     * @param article the article to disassociate from this prediction
     */
    public void removeArticle(ArticleEntity article) {
        if (article != null) {
            articles.remove(article);
            article.predictions.remove(this);
        }
    }

    /**
     * Sanitizes the prediction text to prevent security issues.
     * 
     * @param predictionText the raw prediction text
     * @return sanitized prediction text
     */
    private static String sanitizePredictionText(String predictionText) {
        if (predictionText == null) {
            return null;
        }
        
        String sanitized = predictionText.trim();
        
        // Basic HTML escaping to prevent XSS
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * Sanitizes the prediction type.
     * 
     * @param predictionType the raw prediction type
     * @return sanitized prediction type
     */
    private static String sanitizePredictionType(String predictionType) {
        if (predictionType == null) {
            return null;
        }
        
        String sanitized = predictionType.trim().toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    @Override
    public String toString() {
        return "PredictionEntity{" +
                "id=" + id +
                ", predictionText='" + (predictionText != null && predictionText.length() > 50 ? 
                    predictionText.substring(0, 50) + "..." : predictionText) + '\'' +
                ", predictionType='" + predictionType + '\'' +
                ", createdAt=" + createdAt +
                ", articlesCount=" + (articles != null ? articles.size() : 0) +
                ", instancesCount=" + (instances != null ? instances.size() : 0) +
                '}';
    }
}