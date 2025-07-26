package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for discovering and managing crawler configurations from application properties.
 * Reads crawler configurations from properties with the pattern:
 * crawler.instances.{id}.{property}
 */
@ApplicationScoped
public class CrawlerConfigurationService {
    
    private static final Logger LOG = Logger.getLogger(CrawlerConfigurationService.class);
    private static final String CRAWLER_INSTANCES_PREFIX = "crawler.instances.";
    
    @Inject
    Config config;
    
    @Inject
    Validator validator;
    
    private Map<String, CrawlerConfiguration> configurationCache;
    
    /**
     * Discovers all configured crawler instances from application properties.
     * @return List of all discovered crawler configurations
     */
    public List<CrawlerConfiguration> discoverCrawlers() {
        if (configurationCache == null) {
            configurationCache = loadCrawlerConfigurations();
        }
        return new ArrayList<>(configurationCache.values());
    }
    
    /**
     * Gets a specific crawler configuration by ID.
     * @param crawlerId The crawler ID to look up
     * @return Optional containing the configuration if found
     */
    public Optional<CrawlerConfiguration> getCrawlerConfiguration(String crawlerId) {
        if (configurationCache == null) {
            configurationCache = loadCrawlerConfigurations();
        }
        return Optional.ofNullable(configurationCache.get(crawlerId));
    }
    
    /**
     * Gets all enabled crawler configurations.
     * @return List of enabled crawler configurations
     */
    public List<CrawlerConfiguration> getEnabledCrawlers() {
        return discoverCrawlers().stream()
                .filter(config -> config.enabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Validates a crawler configuration.
     * @param configuration The configuration to validate
     * @return Set of validation errors, empty if valid
     */
    public Set<ConstraintViolation<CrawlerConfiguration>> validateConfiguration(CrawlerConfiguration configuration) {
        return validator.validate(configuration);
    }
    
    /**
     * Gets all crawler configurations (alias for discoverCrawlers for consistency).
     * @return List of all crawler configurations
     */
    public List<CrawlerConfiguration> getAllCrawlers() {
        return discoverCrawlers();
    }
    
    /**
     * Gets a specific crawler configuration by ID.
     * @param crawlerId The crawler ID to look up
     * @return The configuration if found, null otherwise
     */
    public CrawlerConfiguration getCrawlerById(String crawlerId) {
        return getCrawlerConfiguration(crawlerId).orElse(null);
    }
    
    /**
     * Refreshes the configuration cache by reloading from properties.
     */
    public void refreshConfigurations() {
        LOG.info("Refreshing crawler configurations");
        configurationCache = loadCrawlerConfigurations();
    }
    
    private Map<String, CrawlerConfiguration> loadCrawlerConfigurations() {
        Map<String, CrawlerConfiguration> configurations = new HashMap<>();
        Set<String> crawlerIds = extractCrawlerIds();
        
        LOG.info("Discovered crawler IDs: " + crawlerIds);
        
        for (String crawlerId : crawlerIds) {
            try {
                CrawlerConfiguration configuration = buildConfiguration(crawlerId);
                
                // Validate configuration
                Set<ConstraintViolation<CrawlerConfiguration>> violations = validateConfiguration(configuration);
                if (!violations.isEmpty()) {
                    LOG.warn("Invalid configuration for crawler '" + crawlerId + "': " + 
                            violations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining(", ")));
                    continue;
                }
                
                configurations.put(crawlerId, configuration);
                LOG.info("Loaded configuration for crawler: " + crawlerId);
                
            } catch (Exception e) {
                LOG.error("Failed to load configuration for crawler '" + crawlerId + "'", e);
            }
        }
        
        LOG.info("Successfully loaded " + configurations.size() + " crawler configurations");
        return configurations;
    }
    
    private Set<String> extractCrawlerIds() {
        Set<String> crawlerIds = new HashSet<>();
        
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(CRAWLER_INSTANCES_PREFIX)) {
                String remainder = propertyName.substring(CRAWLER_INSTANCES_PREFIX.length());
                int dotIndex = remainder.indexOf('.');
                if (dotIndex > 0) {
                    String crawlerId = remainder.substring(0, dotIndex);
                    crawlerIds.add(crawlerId);
                }
            }
        }
        
        return crawlerIds;
    }
    
    private CrawlerConfiguration buildConfiguration(String crawlerId) {
        String prefix = CRAWLER_INSTANCES_PREFIX + crawlerId + ".";
        
        String name = getRequiredProperty(prefix + "name");
        String baseUrl = getRequiredProperty(prefix + "base-url");
        String healthEndpoint = getRequiredProperty(prefix + "health-endpoint");
        String crawlEndpoint = getRequiredProperty(prefix + "crawl-endpoint");
        String statusEndpoint = getRequiredProperty(prefix + "status-endpoint");
        Boolean enabled = config.getOptionalValue(prefix + "enabled", Boolean.class).orElse(true);
        
        // Extract port from base URL if not explicitly configured
        Integer port = extractPortFromUrl(baseUrl);
        
        return new CrawlerConfiguration(
                crawlerId,
                name,
                baseUrl,
                port,
                healthEndpoint,
                crawlEndpoint,
                statusEndpoint,
                enabled
        );
    }
    
    private String getRequiredProperty(String propertyName) {
        return config.getOptionalValue(propertyName, String.class)
                .orElseThrow(() -> new IllegalStateException("Required property not found: " + propertyName));
    }
    
    private Integer extractPortFromUrl(String url) {
        try {
            // Extract port from URL like http://localhost:8080
            if (url.contains(":")) {
                String[] parts = url.split(":");
                if (parts.length >= 3) {
                    String portPart = parts[2];
                    // Remove any path part
                    if (portPart.contains("/")) {
                        portPart = portPart.substring(0, portPart.indexOf("/"));
                    }
                    return Integer.parseInt(portPart);
                }
            }
            // Default ports
            return url.startsWith("https://") ? 443 : 80;
        } catch (NumberFormatException e) {
            LOG.warn("Could not extract port from URL: " + url + ", using default");
            return url.startsWith("https://") ? 443 : 80;
        }
    }
}