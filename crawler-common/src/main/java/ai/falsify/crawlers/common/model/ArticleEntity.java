package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Common ArticleEntity for database persistence across all crawler implementations.
 * Uses Panache for simplified ORM operations.
 * Enhanced with author relationship support.
 */
@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_article_url", columnList = "url"),
    @Index(name = "idx_article_crawler_source", columnList = "crawler_source"),
    @Index(name = "idx_article_created_at", columnList = "created_at"),
    @Index(name = "idx_article_author", columnList = "author_id")
})
public class ArticleEntity extends PanacheEntity {

    @NotBlank(message = "Article URL cannot be empty")
    @Size(max = 2048, message = "Article URL cannot exceed 2048 characters")
    @Column(nullable = false, unique = true, length = 2048)
    public String url;

    @NotBlank(message = "Article title cannot be empty")
    @Size(max = 1000, message = "Article title cannot exceed 1000 characters")
    @Column(nullable = false, length = 1000)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String text;

    @Size(max = 50, message = "Crawler source cannot exceed 50 characters")
    @Column(name = "crawler_source", length = 50)
    public String crawlerSource;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant createdAt;

    // Many-to-one relationship with author
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    public AuthorEntity author;

    // Many-to-many relationship with predictions
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "article_predictions",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "prediction_id")
    )
    public List<PredictionEntity> predictions = new ArrayList<>();

    // One-to-many relationship with prediction instances
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    public List<PredictionInstanceEntity> predictionInstances = new ArrayList<>();

    /**
     * Default constructor for JPA.
     */
    public ArticleEntity() {
        this.createdAt = Instant.now();
    }

    /**
     * Constructor from Article record with author.
     * 
     * @param article the Article record to convert
     * @param crawlerSource the source crawler name
     * @param author the author entity (required)
     */
    public ArticleEntity(Article article, String crawlerSource, AuthorEntity author) {
        this();
        this.url = sanitizeUrl(article.url());
        this.title = sanitizeTitle(article.title());
        this.text = sanitizeText(article.text());
        this.crawlerSource = sanitizeCrawlerSource(crawlerSource);
        this.author = author != null ? author : AuthorEntity.getUnknownAuthor(); // Author must not be null when persisting
    }

    /**
     * Constructor from Article record (backward compatibility).
     * Uses null author - caller must set author before persisting.
     * 
     * @param article the Article record to convert
     * @param crawlerSource the source crawler name
     * @deprecated Use constructor with AuthorEntity parameter instead
     */
    @Deprecated
    public ArticleEntity(Article article, String crawlerSource) {
        this();
        this.url = sanitizeUrl(article.url());
        this.title = sanitizeTitle(article.title());
        this.text = sanitizeText(article.text());
        this.crawlerSource = sanitizeCrawlerSource(crawlerSource);
        this.author = AuthorEntity.getUnknownAuthor(); // Caller must set author
    }

    /**
     * Converts this entity to an Article record.
     * 
     * @return Article record representation
     */
    public Article toArticle() {
        return new Article(title, url, text);
    }

    /**
     * Finds an article by URL.
     * 
     * @param url the article URL
     * @return the ArticleEntity or null if not found
     */
    public static ArticleEntity findByUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return find("url", url.trim()).firstResult();
    }

    /**
     * Checks if an article with the given URL exists.
     * 
     * @param url the article URL
     * @return true if exists, false otherwise
     */
    public static boolean existsByUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return count("url", url.trim()) > 0;
    }

    /**
     * Finds articles by crawler source.
     * 
     * @param crawlerSource the crawler source name
     * @return list of articles from the specified crawler
     */
    public static List<ArticleEntity> findByCrawlerSource(String crawlerSource) {
        if (crawlerSource == null || crawlerSource.trim().isEmpty()) {
            return List.of();
        }
        return list("crawlerSource", crawlerSource.trim());
    }

    /**
     * Finds articles by author.
     * 
     * @param author the author entity
     * @return list of articles by the specified author
     */
    public static List<ArticleEntity> findByAuthor(AuthorEntity author) {
        if (author == null) {
            return List.of();
        }
        return list("author", author);
    }

    /**
     * Finds articles by author name.
     * 
     * @param authorName the author's name
     * @return list of articles by the specified author name
     */
    public static List<ArticleEntity> findByAuthorName(String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            return List.of();
        }
        return list("author.name", authorName.trim());
    }

    /**
     * Finds articles for analysis with optional filtering.
     * 
     * @param authorId optional author ID filter
     * @param titleSearch optional title search filter
     * @param fromDate optional from date filter
     * @param toDate optional to date filter
     * @return list of filtered articles
     */
    public static List<ArticleEntity> findForAnalysis(Long authorId, String titleSearch, 
                                                     Instant fromDate, Instant toDate) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        
        if (authorId != null) {
            query.append(" AND author.id = ?").append(params.size() + 1);
            params.add(authorId);
        }
        if (titleSearch != null && !titleSearch.trim().isEmpty()) {
            query.append(" AND LOWER(title) LIKE LOWER(?").append(params.size() + 1).append(")");
            params.add("%" + titleSearch.trim() + "%");
        }
        if (fromDate != null) {
            query.append(" AND createdAt >= ?").append(params.size() + 1);
            params.add(fromDate);
        }
        if (toDate != null) {
            query.append(" AND createdAt <= ?").append(params.size() + 1);
            params.add(toDate);
        }
        
        query.append(" ORDER BY createdAt DESC");
        
        return list(query.toString(), params.toArray());
    }

    /**
     * Adds a prediction to this article's many-to-many relationship.
     * 
     * @param prediction the prediction to associate with this article
     */
    public void addPrediction(PredictionEntity prediction) {
        if (prediction != null && !predictions.contains(prediction)) {
            predictions.add(prediction);
            if (!prediction.articles.contains(this)) {
                prediction.articles.add(this);
            }
        }
    }

    /**
     * Removes a prediction from this article's many-to-many relationship.
     * 
     * @param prediction the prediction to disassociate from this article
     */
    public void removePrediction(PredictionEntity prediction) {
        if (prediction != null) {
            predictions.remove(prediction);
            prediction.articles.remove(this);
        }
    }

    /**
     * Gets the count of predictions associated with this article.
     * 
     * @return number of predictions
     */
    public int getPredictionCount() {
        return predictions != null ? predictions.size() : 0;
    }

    /**
     * Gets the count of prediction instances for this article.
     * 
     * @return number of prediction instances
     */
    public int getPredictionInstanceCount() {
        return predictionInstances != null ? predictionInstances.size() : 0;
    }

    /**
     * Checks if this article has been analyzed for predictions.
     * 
     * @return true if the article has prediction instances
     */
    public boolean hasBeenAnalyzed() {
        return predictionInstances != null && !predictionInstances.isEmpty();
    }

    /**
     * Gets the most recent analysis date for this article.
     * 
     * @return the most recent analysis date, or null if never analyzed
     */
    public Instant getLastAnalysisDate() {
        if (predictionInstances == null || predictionInstances.isEmpty()) {
            return null;
        }
        return predictionInstances.stream()
            .map(instance -> instance.extractedAt)
            .filter(date -> date != null)
            .max(Instant::compareTo)
            .orElse(null);
    }

    /**
     * Sanitizes the URL to prevent security issues.
     * 
     * @param url the raw URL
     * @return sanitized URL
     */
    private static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        String sanitized = url.trim();
        if (sanitized.length() > 2048) {
            sanitized = sanitized.substring(0, 2048);
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * Sanitizes the title to prevent security issues.
     * 
     * @param title the raw title
     * @return sanitized title
     */
    private static String sanitizeTitle(String title) {
        if (title == null) {
            return null;
        }
        
        String sanitized = title.trim();
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        // Basic HTML escaping to prevent XSS
        // Note: & must be escaped first to avoid double-escaping
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * Sanitizes the text content.
     * 
     * @param text the raw text
     * @return sanitized text
     */
    private static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        
        // Just trim for text content, no length limit as it's TEXT column
        return text.trim();
    }

    /**
     * Sanitizes the crawler source.
     * 
     * @param crawlerSource the raw crawler source
     * @return sanitized crawler source
     */
    private static String sanitizeCrawlerSource(String crawlerSource) {
        if (crawlerSource == null) {
            return null;
        }
        
        String sanitized = crawlerSource.trim();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }

    @Override
    public String toString() {
        return "ArticleEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", crawlerSource='" + crawlerSource + '\'' +
                ", author=" + (author != null ? author.name : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}