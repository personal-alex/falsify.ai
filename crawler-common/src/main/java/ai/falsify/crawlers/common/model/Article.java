package ai.falsify.crawlers.common.model;

/**
 * Common Article data model used across all crawler implementations.
 * This record represents a crawled article with its basic properties.
 */
public record Article(
    String title,
    String url,
    String text
) {
    
    /**
     * Validates that the article has all required fields.
     * 
     * @return true if the article is valid, false otherwise
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
               url != null && !url.trim().isEmpty() &&
               text != null && !text.trim().isEmpty();
    }
    
    /**
     * Gets the content length of the article text.
     * 
     * @return the length of the article text
     */
    public int getContentLength() {
        return text != null ? text.length() : 0;
    }
    
    /**
     * Gets the title length.
     * 
     * @return the length of the article title
     */
    public int getTitleLength() {
        return title != null ? title.length() : 0;
    }
    
    /**
     * Creates a sanitized version of the article with trimmed fields.
     * 
     * @return a new Article with trimmed title, url, and text
     */
    public Article sanitized() {
        return new Article(
            title != null ? title.trim() : null,
            url != null ? url.trim() : null,
            text != null ? text.trim() : null
        );
    }
}