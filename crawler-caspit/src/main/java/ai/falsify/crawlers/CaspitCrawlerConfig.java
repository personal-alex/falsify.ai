package ai.falsify.crawlers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration properties for the Ben Caspit crawler using Quarkus declarative approach.
 * Uses @ConfigProperty for individual property injection with validation and error handling.
 */
@ApplicationScoped
public class CaspitCrawlerConfig {

    private static final Logger LOG = Logger.getLogger(CaspitCrawlerConfig.class);

    // Base crawler configuration
    @ConfigProperty(name = "caspit.crawler.base-url")
    String baseUrl;

    @ConfigProperty(name = "caspit.crawler.source", defaultValue = "caspit")
    String crawlerSource;

    @ConfigProperty(name = "caspit.crawler.max-pages", defaultValue = "50")
    int maxPages;

    @ConfigProperty(name = "caspit.crawler.page-load-timeout", defaultValue = "10000")
    int pageLoadTimeout;

    // WebDriver configuration
    @ConfigProperty(name = "caspit.crawler.webdriver.headless", defaultValue = "true")
    boolean webdriverHeadless;

    @ConfigProperty(name = "caspit.crawler.webdriver.window-width", defaultValue = "1920")
    int webdriverWindowWidth;

    @ConfigProperty(name = "caspit.crawler.webdriver.window-height", defaultValue = "1080")
    int webdriverWindowHeight;

    @ConfigProperty(name = "caspit.crawler.webdriver.user-agent", 
                   defaultValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
    String webdriverUserAgent;

    @ConfigProperty(name = "caspit.crawler.webdriver.implicit-wait", defaultValue = "2")
    int webdriverImplicitWait;

    @ConfigProperty(name = "caspit.crawler.webdriver.element-wait", defaultValue = "5")
    int webdriverElementWait;

    // Crawling behavior configuration
    @ConfigProperty(name = "caspit.crawler.crawling.page-delay", defaultValue = "2000")
    int crawlingPageDelay;

    @ConfigProperty(name = "caspit.crawler.crawling.scroll-delay", defaultValue = "3000")
    int crawlingScrollDelay;

    @ConfigProperty(name = "caspit.crawler.crawling.connection-timeout", defaultValue = "10000")
    int crawlingConnectionTimeout;

    @ConfigProperty(name = "caspit.crawler.crawling.min-content-length", defaultValue = "100")
    int crawlingMinContentLength;

    @ConfigProperty(name = "caspit.crawler.crawling.early-termination-enabled", defaultValue = "true")
    boolean crawlingEarlyTerminationEnabled;

    // Author metadata configuration
    @ConfigProperty(name = "caspit.crawler.author.name", defaultValue = "Unknown Author")
    String authorName;

    @ConfigProperty(name = "caspit.crawler.author.avatar-url")
    Optional<String> authorAvatarUrl;

    @ConfigProperty(name = "caspit.crawler.author.fallback-name", defaultValue = "Unknown Author")
    String authorFallbackName;

    // Getter methods for base configuration
    public String baseUrl() {
        return baseUrl;
    }

    public String crawlerSource() {
        return crawlerSource;
    }

    public int maxPages() {
        return maxPages;
    }

    public int pageLoadTimeout() {
        return pageLoadTimeout;
    }

    // WebDriver configuration getters
    public WebDriverConfig webdriver() {
        return new WebDriverConfig();
    }

    public class WebDriverConfig {
        public boolean headless() {
            return webdriverHeadless;
        }

        public int windowWidth() {
            return webdriverWindowWidth;
        }

        public int windowHeight() {
            return webdriverWindowHeight;
        }

        public String userAgent() {
            return webdriverUserAgent;
        }

        public int implicitWait() {
            return webdriverImplicitWait;
        }

        public int elementWait() {
            return webdriverElementWait;
        }
    }

    // Crawling configuration getters
    public CrawlingConfig crawling() {
        return new CrawlingConfig();
    }

    // Author configuration getters
    public AuthorConfig author() {
        return new AuthorConfig();
    }

    public class CrawlingConfig {
        public int pageDelay() {
            return crawlingPageDelay;
        }

        public int scrollDelay() {
            return crawlingScrollDelay;
        }

        public int connectionTimeout() {
            return crawlingConnectionTimeout;
        }

        public int minContentLength() {
            return crawlingMinContentLength;
        }

        public boolean earlyTerminationEnabled() {
            return crawlingEarlyTerminationEnabled;
        }
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
     * and provide detailed error messages for troubleshooting.
     */
    @PostConstruct
    public void validateConfiguration() {
        LOG.info("Validating Ben Caspit crawler configuration");
        
        try {
            // Validate base URL
            validateBaseUrl();
            
            // Validate numeric configurations
            validateNumericConfigurations();
            
            // Validate WebDriver configurations
            validateWebDriverConfigurations();
            
            // Validate crawling behavior configurations
            validateCrawlingConfigurations();
            
            // Validate author metadata configurations
            validateAuthorConfigurations();
            
            LOG.info("Ben Caspit crawler configuration validation completed successfully");
            logConfigurationSummary();
            
        } catch (Exception e) {
            LOG.errorf("Configuration validation failed: %s", e.getMessage());
            throw new RuntimeException("Invalid crawler configuration", e);
        }
    }
    
    /**
     * Validate the base URL configuration
     */
    private void validateBaseUrl() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL (caspit.crawler.base-url) is required and cannot be empty");
        }
        
        String cleanUrl = baseUrl.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Base URL must start with http:// or https://. Current value: " + cleanUrl);
        }
        
        if (!cleanUrl.toLowerCase().contains("maariv.co.il")) {
            LOG.warnf("Base URL does not contain 'maariv.co.il' - this may not be the expected target site: %s", cleanUrl);
        }
        
        LOG.debugf("Base URL validation passed: %s", cleanUrl);
    }
    
    /**
     * Validate numeric configuration values
     */
    private void validateNumericConfigurations() {
        // Validate max pages
        if (maxPages <= 0) {
            throw new IllegalArgumentException("Max pages must be positive. Current value: " + maxPages);
        }
        if (maxPages > 1000) {
            LOG.warnf("Max pages is very high (%d) - this may result in long crawl times", maxPages);
        }
        
        // Validate page load timeout
        if (pageLoadTimeout < 1000) {
            throw new IllegalArgumentException("Page load timeout must be at least 1000ms. Current value: " + pageLoadTimeout);
        }
        if (pageLoadTimeout > 60000) {
            LOG.warnf("Page load timeout is very high (%dms) - this may cause slow crawling", pageLoadTimeout);
        }
        
        LOG.debugf("Numeric configuration validation passed - maxPages: %d, pageLoadTimeout: %dms", maxPages, pageLoadTimeout);
    }
    
    /**
     * Validate WebDriver configuration values
     */
    private void validateWebDriverConfigurations() {
        // Validate window dimensions
        if (webdriverWindowWidth < 800 || webdriverWindowWidth > 4000) {
            throw new IllegalArgumentException("WebDriver window width must be between 800 and 4000 pixels. Current value: " + webdriverWindowWidth);
        }
        if (webdriverWindowHeight < 600 || webdriverWindowHeight > 3000) {
            throw new IllegalArgumentException("WebDriver window height must be between 600 and 3000 pixels. Current value: " + webdriverWindowHeight);
        }
        
        // Validate user agent
        if (webdriverUserAgent == null || webdriverUserAgent.trim().isEmpty()) {
            throw new IllegalArgumentException("WebDriver user agent cannot be empty");
        }
        if (webdriverUserAgent.length() > 500) {
            throw new IllegalArgumentException("WebDriver user agent is too long (max 500 characters). Current length: " + webdriverUserAgent.length());
        }
        
        // Validate wait times
        if (webdriverImplicitWait < 0 || webdriverImplicitWait > 30) {
            throw new IllegalArgumentException("WebDriver implicit wait must be between 0 and 30 seconds. Current value: " + webdriverImplicitWait);
        }
        if (webdriverElementWait < 1 || webdriverElementWait > 60) {
            throw new IllegalArgumentException("WebDriver element wait must be between 1 and 60 seconds. Current value: " + webdriverElementWait);
        }
        
        LOG.debugf("WebDriver configuration validation passed - dimensions: %dx%d, headless: %s, waits: implicit=%ds, element=%ds", 
                  webdriverWindowWidth, webdriverWindowHeight, webdriverHeadless, webdriverImplicitWait, webdriverElementWait);
    }
    
    /**
     * Validate crawling behavior configuration values
     */
    private void validateCrawlingConfigurations() {
        // Validate delays
        if (crawlingPageDelay < 0 || crawlingPageDelay > 30000) {
            throw new IllegalArgumentException("Page delay must be between 0 and 30000ms. Current value: " + crawlingPageDelay);
        }
        if (crawlingScrollDelay < 0 || crawlingScrollDelay > 30000) {
            throw new IllegalArgumentException("Scroll delay must be between 0 and 30000ms. Current value: " + crawlingScrollDelay);
        }
        
        // Validate connection timeout
        if (crawlingConnectionTimeout < 1000 || crawlingConnectionTimeout > 60000) {
            throw new IllegalArgumentException("Connection timeout must be between 1000 and 60000ms. Current value: " + crawlingConnectionTimeout);
        }
        
        // Validate minimum content length
        if (crawlingMinContentLength < 10 || crawlingMinContentLength > 10000) {
            throw new IllegalArgumentException("Minimum content length must be between 10 and 10000 characters. Current value: " + crawlingMinContentLength);
        }
        
        // Warn about potentially problematic configurations
        if (crawlingPageDelay < 1000) {
            LOG.warnf("Page delay is very short (%dms) - this may overwhelm the target site", crawlingPageDelay);
        }
        if (crawlingScrollDelay < 1000) {
            LOG.warnf("Scroll delay is very short (%dms) - this may not allow sufficient time for content loading", crawlingScrollDelay);
        }
        
        LOG.debugf("Crawling configuration validation passed - pageDelay: %dms, scrollDelay: %dms, connectionTimeout: %dms, minContentLength: %d", 
                  crawlingPageDelay, crawlingScrollDelay, crawlingConnectionTimeout, crawlingMinContentLength);
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
     * Log a summary of the current configuration for monitoring and debugging
     */
    private void logConfigurationSummary() {
        LOG.infof("Crawler Configuration Summary:");
        LOG.infof("  Base URL: %s", baseUrl);
        LOG.infof("  Max Pages: %d", maxPages);
        LOG.infof("  Page Load Timeout: %dms", pageLoadTimeout);
        LOG.infof("  WebDriver: headless=%s, window=%dx%d, waits=implicit:%ds/element:%ds", 
                 webdriverHeadless, webdriverWindowWidth, webdriverWindowHeight, 
                 webdriverImplicitWait, webdriverElementWait);
        LOG.infof("  Crawling: pageDelay=%dms, scrollDelay=%dms, connectionTimeout=%dms, minContentLength=%d, earlyTermination=%s", 
                 crawlingPageDelay, crawlingScrollDelay, crawlingConnectionTimeout, crawlingMinContentLength, crawlingEarlyTerminationEnabled);
        LOG.infof("  Author: name=%s, avatarUrl=%s, fallback=%s", 
                 authorName, authorAvatarUrl.orElse("not configured"), authorFallbackName);
    }
    
    /**
     * Get a validation report for external monitoring or health checks
     * @return Map containing configuration validation status and details
     */
    public Map<String, Object> getValidationReport() {
        Map<String, Object> report = new HashMap<>();
        
        try {
            validateConfiguration();
            report.put("status", "valid");
            report.put("message", "All configuration values are within acceptable ranges");
        } catch (Exception e) {
            report.put("status", "invalid");
            report.put("message", e.getMessage());
            report.put("error", e.getClass().getSimpleName());
        }
        
        // Add current configuration values for reference
        Map<String, Object> currentConfig = new HashMap<>();
        currentConfig.put("baseUrl", baseUrl);
        currentConfig.put("maxPages", maxPages);
        currentConfig.put("pageLoadTimeout", pageLoadTimeout);
        currentConfig.put("webdriverHeadless", webdriverHeadless);
        currentConfig.put("webdriverWindowSize", webdriverWindowWidth + "x" + webdriverWindowHeight);
        currentConfig.put("crawlingDelays", "page:" + crawlingPageDelay + "ms, scroll:" + crawlingScrollDelay + "ms");
        currentConfig.put("connectionTimeout", crawlingConnectionTimeout);
        currentConfig.put("minContentLength", crawlingMinContentLength);
        currentConfig.put("earlyTerminationEnabled", crawlingEarlyTerminationEnabled);
        currentConfig.put("authorName", authorName);
        currentConfig.put("authorAvatarUrl", authorAvatarUrl.orElse("not configured"));
        currentConfig.put("authorFallbackName", authorFallbackName);
        
        report.put("configuration", currentConfig);
        report.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return report;
    }
}