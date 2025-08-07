package ai.falsify.crawlers.common.service;

import ai.falsify.crawlers.common.model.*;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing articles with enhanced filtering and author support.
 * Provides methods for article retrieval, filtering, and analysis preparation.
 */
@ApplicationScoped
public class ArticleService {
    
    private static final Logger LOG = Logger.getLogger(ArticleService.class);
    
    /**
     * Gets articles for analysis with filtering support.
     * Supports filtering by author, title search, and date range with pagination.
     * 
     * @param filter the filter criteria
     * @return list of filtered articles
     */
    public List<ArticleEntity> getArticlesForAnalysis(@NotNull ArticleFilter filter) {
        LOG.debugf("Getting articles for analysis with filter: %s", filter);
        
        try {
            StringBuilder queryBuilder = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            
            // Build the WHERE clause
            buildWhereClause(filter, queryBuilder, parameters);
            
            // Create the query with sorting
            String query = queryBuilder.toString();
            Sort sort = Sort.by("createdAt").descending()
                          .and("id").descending(); // Secondary sort for consistency
            
            PanacheQuery<ArticleEntity> panacheQuery;
            if (parameters.isEmpty()) {
                panacheQuery = ArticleEntity.findAll(sort);
            } else {
                panacheQuery = ArticleEntity.find(query, sort, parameters.toArray());
            }
            
            // Apply pagination
            Page page = Page.of(filter.getPageOrDefault(), filter.getSizeOrDefault());
            List<ArticleEntity> results = panacheQuery.page(page).list();
            
            LOG.debugf("Found %d articles for analysis", results.size());
            return results;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting articles for analysis with filter: %s", filter);
            throw new RuntimeException("Failed to retrieve articles for analysis", e);
        }
    }
    
    /**
     * Gets the total count of articles matching the filter criteria.
     * 
     * @param filter the filter criteria
     * @return total count of matching articles
     */
    public long getArticleCountForAnalysis(@NotNull ArticleFilter filter) {
        LOG.debugf("Getting article count for analysis with filter: %s", filter);
        
        try {
            StringBuilder queryBuilder = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            
            // Build the WHERE clause
            buildWhereClause(filter, queryBuilder, parameters);
            
            long count;
            if (parameters.isEmpty()) {
                count = ArticleEntity.count();
            } else {
                count = ArticleEntity.count(queryBuilder.toString(), parameters.toArray());
            }
            
            LOG.debugf("Found %d total articles matching filter", count);
            return count;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting article count for analysis with filter: %s", filter);
            throw new RuntimeException("Failed to count articles for analysis", e);
        }
    }
    
    /**
     * Gets all authors ordered by name for filter dropdowns.
     * 
     * @return list of all authors
     */
    public List<AuthorEntity> getAllAuthors() {
        LOG.debug("Getting all authors for filter dropdown");
        
        try {
            List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();
            LOG.debugf("Found %d authors", authors.size());
            return authors;
            
        } catch (Exception e) {
            LOG.error("Error getting all authors", e);
            throw new RuntimeException("Failed to retrieve authors", e);
        }
    }
    
    /**
     * Gets authors with article counts for enhanced filtering.
     * 
     * @return list of authors with their article counts
     */
    public List<AuthorWithCount> getAuthorsWithCounts() {
        LOG.debug("Getting authors with article counts");
        
        try {
            List<AuthorEntity> authors = AuthorEntity.findAllOrderedByName();
            List<AuthorWithCount> authorsWithCounts = new ArrayList<>();
            
            for (AuthorEntity author : authors) {
                long articleCount = author.getArticleCount();
                AuthorInfo authorInfo = new AuthorInfo(author.id, author.name, author.avatarUrl);
                authorsWithCounts.add(new AuthorWithCount(authorInfo, articleCount));
            }
            
            LOG.debugf("Found %d authors with counts", authorsWithCounts.size());
            return authorsWithCounts;
            
        } catch (Exception e) {
            LOG.error("Error getting authors with counts", e);
            throw new RuntimeException("Failed to retrieve authors with counts", e);
        }
    }
    
    /**
     * Creates an article with author relationship.
     * Implements find-or-create pattern for authors.
     * 
     * @param article the article data
     * @param crawlerSource the crawler source name
     * @param authorName the author's name
     * @param avatarUrl the author's avatar URL (optional)
     * @return the created article entity
     */
    @Transactional
    public ArticleEntity createArticleWithAuthor(@NotNull Article article, 
                                               @NotNull String crawlerSource,
                                               String authorName, 
                                               String avatarUrl) {
        LOG.debugf("Creating article with author: %s, crawler: %s, author: %s", 
                  article.title(), crawlerSource, authorName);
        
        try {
            // Find or create the author
            AuthorEntity author = AuthorEntity.findOrCreate(authorName, avatarUrl);
            
            // Create the article with author relationship
            ArticleEntity articleEntity = new ArticleEntity(article, crawlerSource, author);
            articleEntity.persist();
            
            LOG.debugf("Created article with ID: %d, author: %s", articleEntity.id, author.name);
            return articleEntity;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error creating article with author: %s", authorName);
            throw new RuntimeException("Failed to create article with author", e);
        }
    }
    
    /**
     * Gets articles by specific IDs for batch operations.
     * 
     * @param articleIds list of article IDs
     * @return list of articles matching the IDs
     */
    public List<ArticleEntity> getArticlesByIds(@NotNull List<Long> articleIds) {
        LOG.debugf("Getting articles by IDs: %s", articleIds);
        
        if (articleIds.isEmpty()) {
            return List.of();
        }
        
        try {
            List<ArticleEntity> articles = ArticleEntity.list("id in ?1", articleIds);
            LOG.debugf("Found %d articles for %d requested IDs", articles.size(), articleIds.size());
            return articles;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting articles by IDs: %s", articleIds);
            throw new RuntimeException("Failed to retrieve articles by IDs", e);
        }
    }
    
    /**
     * Gets articles that haven't been analyzed yet.
     * 
     * @param filter the filter criteria
     * @return list of unanalyzed articles
     */
    public List<ArticleEntity> getUnanalyzedArticles(@NotNull ArticleFilter filter) {
        LOG.debugf("Getting unanalyzed articles with filter: %s", filter);
        
        try {
            StringBuilder queryBuilder = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            
            // Build the WHERE clause
            buildWhereClause(filter, queryBuilder, parameters);
            
            // Add condition for unanalyzed articles
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" AND ");
            }
            queryBuilder.append("size(predictionInstances) = 0");
            
            // Create the query with sorting
            String query = queryBuilder.toString();
            Sort sort = Sort.by("createdAt").descending();
            
            PanacheQuery<ArticleEntity> panacheQuery;
            if (parameters.isEmpty() && query.equals("size(predictionInstances) = 0")) {
                panacheQuery = ArticleEntity.find("size(predictionInstances) = 0", sort);
            } else {
                panacheQuery = ArticleEntity.find(query, sort, parameters.toArray());
            }
            
            // Apply pagination
            Page page = Page.of(filter.getPageOrDefault(), filter.getSizeOrDefault());
            List<ArticleEntity> results = panacheQuery.page(page).list();
            
            LOG.debugf("Found %d unanalyzed articles", results.size());
            return results;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting unanalyzed articles with filter: %s", filter);
            throw new RuntimeException("Failed to retrieve unanalyzed articles", e);
        }
    }
    
    /**
     * Builds the WHERE clause for article filtering.
     * 
     * @param filter the filter criteria
     * @param queryBuilder the query builder to append to
     * @param parameters the parameter list to add to
     */
    private void buildWhereClause(ArticleFilter filter, StringBuilder queryBuilder, List<Object> parameters) {
        boolean hasConditions = false;
        
        // Author filter
        if (filter.authorId() != null) {
            queryBuilder.append("author.id = ?").append(parameters.size() + 1);
            parameters.add(filter.authorId());
            hasConditions = true;
        }
        
        // Title search filter
        String titleSearch = filter.getSanitizedTitleSearch();
        if (titleSearch != null) {
            if (hasConditions) {
                queryBuilder.append(" AND ");
            }
            queryBuilder.append("LOWER(title) LIKE LOWER(?").append(parameters.size() + 1).append(")");
            parameters.add("%" + titleSearch + "%");
            hasConditions = true;
        }
        
        // From date filter
        if (filter.fromDate() != null) {
            if (hasConditions) {
                queryBuilder.append(" AND ");
            }
            queryBuilder.append("createdAt >= ?").append(parameters.size() + 1);
            parameters.add(filter.fromDate());
            hasConditions = true;
        }
        
        // To date filter
        if (filter.toDate() != null) {
            if (hasConditions) {
                queryBuilder.append(" AND ");
            }
            queryBuilder.append("createdAt <= ?").append(parameters.size() + 1);
            parameters.add(filter.toDate());
        }
    }
    
    /**
     * Record for author with article count information.
     */
    public record AuthorWithCount(
        AuthorInfo author,
        long articleCount
    ) {}
    
    /**
     * Record for author information.
     */
    public record AuthorInfo(
        Long id,
        String name,
        String avatarUrl
    ) {}
}