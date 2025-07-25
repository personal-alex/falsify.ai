package ai.falsify.crawlers;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

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
    }
}