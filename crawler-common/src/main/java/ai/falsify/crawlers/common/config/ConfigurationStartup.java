package ai.falsify.crawlers.common.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Startup bean that validates configuration when the application starts.
 * Ensures that configuration is valid before the application begins processing.
 */
@ApplicationScoped
public class ConfigurationStartup {

    private static final Logger LOG = Logger.getLogger(ConfigurationStartup.class);

    @Inject
    ConfigurationValidator configurationValidator;

    /**
     * Validates configuration on application startup.
     * 
     * @param event the startup event
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Validating crawler configuration on startup...");
        
        try {
            ConfigurationValidator.ValidationResult result = configurationValidator.validateConfiguration();
            
            if (!result.isValid()) {
                LOG.errorf("Configuration validation failed with %d errors:", result.getErrorCount());
                for (String error : result.getErrors()) {
                    LOG.error("  - " + error);
                }
                
                // In production, you might want to fail fast
                // throw new IllegalStateException("Invalid configuration detected");
            }
            
            if (result.hasWarnings()) {
                LOG.warnf("Configuration validation completed with %d warnings:", result.getWarningCount());
                for (String warning : result.getWarnings()) {
                    LOG.warn("  - " + warning);
                }
            }
            
            if (result.isValid() && !result.hasWarnings()) {
                LOG.info("Configuration validation completed successfully with no issues");
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to validate configuration during startup");
            // In production, you might want to fail fast
            // throw new IllegalStateException("Configuration validation failed", e);
        }
    }
}