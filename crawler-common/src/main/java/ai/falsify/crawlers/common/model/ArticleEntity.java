package ai.falsify.crawlers.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Common ArticleEntity for database persistence across all crawler implementations.
 * Uses Panache for simplified ORM operations.
 */
@Entity
@Table(name = "articles")
public class ArticleEntity extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 2048)
    public String url;

    @Column(nullable = false, length = 1000)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String text;

    @Column(name = "crawler_source", length = 50)
    public String crawlerSource;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public java.time.Instant createdAt;

    /**
     * Default constructor for JPA.
     */
    public ArticleEntity() {
        this.createdAt = java.time.Instant.now();
    }

    /**
     * Constructor from Article record.
     * 
     * @param article the Article record to convert
     * @param crawlerSource the source crawler name
     */
    public ArticleEntity(Article article, String crawlerSource) {
        this.url = article.url();
        this.title = article.title();
        this.text = article.text();
        this.crawlerSource = crawlerSource;
        this.createdAt = java.time.Instant.now();
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
        return find("url", url).firstResult();
    }

    /**
     * Checks if an article with the given URL exists.
     * 
     * @param url the article URL
     * @return true if exists, false otherwise
     */
    public static boolean existsByUrl(String url) {
        return count("url", url) > 0;
    }

    /**
     * Finds articles by crawler source.
     * 
     * @param crawlerSource the crawler source name
     * @return list of articles from the specified crawler
     */
    public static java.util.List<ArticleEntity> findByCrawlerSource(String crawlerSource) {
        return list("crawlerSource", crawlerSource);
    }
}