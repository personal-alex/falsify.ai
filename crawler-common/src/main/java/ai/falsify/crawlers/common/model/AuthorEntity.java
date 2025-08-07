package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * AuthorEntity represents an author of articles in the system.
 * Provides author information including name and avatar for better article organization.
 */
@Entity
@Table(name = "authors", indexes = {
    @Index(name = "idx_author_name", columnList = "name")
})
public class AuthorEntity extends PanacheEntity {

    @NotBlank(message = "Author name cannot be empty")
    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    public String name;

    @Size(max = 1000, message = "Avatar URL cannot exceed 1000 characters")
    @Column(name = "avatar_url", length = 1000)
    public String avatarUrl;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant updatedAt;

    // One-to-many relationship with articles
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    public List<ArticleEntity> articles;

    /**
     * Default constructor for JPA.
     */
    public AuthorEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Constructor with name and optional avatar URL.
     * 
     * @param name the author's name (required)
     * @param avatarUrl the author's avatar URL (optional)
     */
    public AuthorEntity(String name, String avatarUrl) {
        this();
        this.name = sanitizeName(name);
        this.avatarUrl = sanitizeAvatarUrl(avatarUrl);
    }

    /**
     * Constructor with name only.
     * 
     * @param name the author's name (required)
     */
    public AuthorEntity(String name) {
        this(name, null);
    }

    /**
     * Finds an author by name (case-insensitive).
     * 
     * @param name the author's name
     * @return the AuthorEntity or null if not found
     */
    public static AuthorEntity findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return find("LOWER(name) = LOWER(?1)", name.trim()).firstResult();
    }

    /**
     * Finds or creates an author with the given name and avatar URL.
     * This method implements the find-or-create pattern for author entities.
     * Note: This method should be called within an existing transaction context.
     * 
     * @param name the author's name (required)
     * @param avatarUrl the author's avatar URL (optional)
     * @return existing or newly created AuthorEntity
     */
    public static AuthorEntity findOrCreate(String name, String avatarUrl) {
        if (name == null || name.trim().isEmpty()) {
            return getUnknownAuthor();
        }

        AuthorEntity existing = findByName(name);
        if (existing != null) {
            // Update avatar URL if provided and different
            if (avatarUrl != null && !avatarUrl.equals(existing.avatarUrl)) {
                existing.avatarUrl = sanitizeAvatarUrl(avatarUrl);
                existing.updatedAt = Instant.now();
                existing.persist();
            }
            return existing;
        }

        // Create new author
        AuthorEntity newAuthor = new AuthorEntity(name, avatarUrl);
        newAuthor.persist();
        return newAuthor;
    }

    /**
     * Gets or creates the default "Unknown Author" entity.
     * Note: This method should be called within an existing transaction context.
     * 
     * @return the default unknown author entity
     */
    public static AuthorEntity getUnknownAuthor() {
        AuthorEntity unknown = findByName("Unknown Author");
        if (unknown == null) {
            unknown = new AuthorEntity("Unknown Author", null);
            unknown.persist();
        }
        return unknown;
    }

    /**
     * Gets all authors ordered by name.
     * 
     * @return list of all authors
     */
    public static List<AuthorEntity> findAllOrderedByName() {
        return list("ORDER BY name ASC");
    }

    /**
     * Counts the number of articles for this author.
     * 
     * @return the number of articles by this author
     */
    public long getArticleCount() {
        return ArticleEntity.count("author", this);
    }

    /**
     * Sanitizes the author name to prevent security issues.
     * 
     * @param name the raw author name
     * @return sanitized author name
     */
    private static String sanitizeName(String name) {
        if (name == null) {
            return null;
        }
        
        // Trim whitespace and limit length
        String sanitized = name.trim();
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
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
     * Sanitizes the avatar URL to prevent security issues.
     * 
     * @param avatarUrl the raw avatar URL
     * @return sanitized avatar URL
     */
    private static String sanitizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        
        String sanitized = avatarUrl.trim();
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        // Basic URL validation - must start with http:// or https://
        if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
            return null;
        }
        
        return sanitized;
    }

    /**
     * Updates the updated_at timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "AuthorEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}