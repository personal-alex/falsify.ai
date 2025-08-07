package ai.falsify.crawlers;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuration class for DruckerCrawler with pagination and early termination support.
 */
@ApplicationScoped
public class DruckerCrawlerConfig {

    private static final Logger LOG = Logger.getLogger(DruckerCrawlerConfig.class);

    @ConfigProperty(name = "crawler.drucker.network.start-url")
    String baseUrl;

    @ConfigProperty(name = "crawler.drucker.pagination.max-pages", defaultValue = "10")
    int maxPages;

    @ConfigProperty(name = "crawler.drucker.pagination.page-delay", defaultValue = "PT2S")
    Duration pageDelay;

    @ConfigProperty(name = "crawler.drucker.pagination.enable-early-termination", defaultValue = "true")
    boolean enableEarlyTermination;

    @ConfigProperty(name = "crawler.drucker.pagination.empty-page-threshold", defaultValue = "1")
    int emptyPageThreshold;

    // Author metadata configuration
    @ConfigProperty(name = "crawler.drucker.author.name", defaultValue = "Unknown Author")
    String authorName;

    @ConfigProperty(name = "crawler.drucker.author.avatar-url")
    Optional<String> authorAvatarUrl;

    @ConfigProperty(name = "crawler.drucker.author.fallback-name", defaultValue = "Unknown Author")
    String authorFallbackName;

    public String baseUrl() {
        return baseUrl;
    }

    public int maxPages() {
        return maxPages;
    }

    public Duration pageDelay() {
        return pageDelay;
    }

    public boolean enableEarlyTermination() {
        return enableEarlyTermination;
    }

    public int emptyPageThreshold() {
        return emptyPageThreshold;
    }

    // Author configuration getters
    public AuthorConfig author() {
        return new AuthorConfig();
    }

    public class AuthorConfig {
        public String name() {
            return authorName != null && !authorName.trim().isEmpty() ? authorName : authorFallbackName;
        }

        public Optional<String> avatarUrl() {
            return authorAvatarUrl;
        }

        public String fallbackName() {
            return authorFallbackName;
        }
    }

    /**
     * Validate configuration after injection to ensure all values are within acceptable ranges
     */
    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        LOG.info("Validating Drucker crawler configuration");
        
        try {
            // Validate author metadata configurations
            validateAuthorConfigurations();
            
            LOG.info("Drucker crawler configuration validation completed successfully");
            
        } catch (Exception e) {
            LOG.errorf("Configuration validation failed: %s", e.getMessage());
            throw new RuntimeException("Invalid crawler configuration", e);
        }
    }
    
    /**
     * Validate author metadata configuration values
     */
    private void validateAuthorConfigurations() {
        // Validate author name
        if (authorName == null || authorName.trim().isEmpty()) {
            LOG.warnf("Author name is not configured, using fallback: %s", authorFallbackName);
        } else if (authorName.length() > 255) {
            throw new IllegalArgumentException("Author name is too long (max 255 characters). Current length: " + authorName.length());
        }
        
        // Validate fallback name
        if (authorFallbackName == null || authorFallbackName.trim().isEmpty()) {
            throw new IllegalArgumentException("Author fallback name cannot be empty");
        }
        if (authorFallbackName.length() > 255) {
            throw new IllegalArgumentException("Author fallback name is too long (max 255 characters). Current length: " + authorFallbackName.length());
        }
        
        // Validate avatar URL if provided
        if (authorAvatarUrl.isPresent()) {
            String avatarUrl = authorAvatarUrl.get();
            if (avatarUrl.length() > 1000) {
                throw new IllegalArgumentException("Author avatar URL is too long (max 1000 characters). Current length: " + avatarUrl.length());
            }
            if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                LOG.warnf("Author avatar URL does not start with http:// or https://: %s", avatarUrl);
            }
        }
        
        LOG.debugf("Author configuration validation passed - name: %s, avatarUrl: %s, fallback: %s", 
                  authorName, authorAvatarUrl.orElse("not configured"), authorFallbackName);
    }

    /**
     * Log configuration summary for debugging
     */
    public void logConfigurationSummary() {
        LOG.infof("DruckerCrawler Configuration Summary:");
        LOG.infof("  Base URL: %s", baseUrl);
        LOG.infof("  Max Pages: %d", maxPages);
        LOG.infof("  Page Delay: %s", pageDelay);
        LOG.infof("  Early Termination: %s", enableEarlyTermination);
        LOG.infof("  Empty Page Threshold: %d", emptyPageThreshold);
        LOG.infof("  Author: name=%s, avatarUrl=%s, fallback=%s", 
                 authorName, authorAvatarUrl.orElse("not configured"), authorFallbackName);
    }
}