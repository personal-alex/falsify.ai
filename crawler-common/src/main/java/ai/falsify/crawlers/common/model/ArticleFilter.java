package ai.falsify.crawlers.common.model;

import java.time.Instant;

/**
 * Filter criteria for article queries.
 * Used to filter articles for analysis based on various criteria.
 */
public record ArticleFilter(
    Long authorId,
    String titleSearch,
    Instant fromDate,
    Instant toDate,
    Integer page,
    Integer size
) {
    
    /**
     * Creates a filter with default pagination.
     */
    public static ArticleFilter withDefaults() {
        return new ArticleFilter(null, null, null, null, 0, 20);
    }
    
    /**
     * Creates a filter with custom pagination.
     */
    public static ArticleFilter withPagination(int page, int size) {
        return new ArticleFilter(null, null, null, null, page, size);
    }
    
    /**
     * Creates a builder for constructing filters.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for ArticleFilter.
     */
    public static class Builder {
        private Long authorId;
        private String titleSearch;
        private Instant fromDate;
        private Instant toDate;
        private Integer page = 0;
        private Integer size = 20;
        
        public Builder authorId(Long authorId) {
            this.authorId = authorId;
            return this;
        }
        
        public Builder titleSearch(String titleSearch) {
            this.titleSearch = titleSearch;
            return this;
        }
        
        public Builder fromDate(Instant fromDate) {
            this.fromDate = fromDate;
            return this;
        }
        
        public Builder toDate(Instant toDate) {
            this.toDate = toDate;
            return this;
        }
        
        public Builder page(Integer page) {
            this.page = page;
            return this;
        }
        
        public Builder size(Integer size) {
            this.size = size;
            return this;
        }
        
        public ArticleFilter build() {
            return new ArticleFilter(authorId, titleSearch, fromDate, toDate, page, size);
        }
    }
    
    /**
     * Gets the page number, defaulting to 0 if null.
     */
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }
    
    /**
     * Gets the page size, defaulting to 20 if null.
     */
    public int getSizeOrDefault() {
        return size != null ? size : 20;
    }
    
    /**
     * Checks if any filter criteria are specified.
     */
    public boolean hasFilters() {
        return authorId != null || 
               (titleSearch != null && !titleSearch.trim().isEmpty()) ||
               fromDate != null || 
               toDate != null;
    }
    
    /**
     * Gets the sanitized title search term.
     */
    public String getSanitizedTitleSearch() {
        if (titleSearch == null || titleSearch.trim().isEmpty()) {
            return null;
        }
        return titleSearch.trim();
    }
}