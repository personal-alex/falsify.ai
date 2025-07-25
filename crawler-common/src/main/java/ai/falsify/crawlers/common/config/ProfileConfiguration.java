package ai.falsify.crawlers.common.config;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Service for managing profile-specific configuration.
 * Provides utilities for working with different deployment profiles (dev, test, prod).
 */
@ApplicationScoped
public class ProfileConfiguration {

    private static final Logger LOG = Logger.getLogger(ProfileConfiguration.class);

    @Inject
    CrawlerConfiguration config;

    /**
     * Gets the current active profile.
     * 
     * @return the active profile name
     */
    public String getActiveProfile() {
        // Use system property to get the active profile
        return System.getProperty("quarkus.profile", "prod");
    }

    /**
     * Checks if the application is running in development mode.
     * 
     * @return true if in development profile
     */
    public boolean isDevelopmentMode() {
        return "dev".equals(getActiveProfile());
    }

    /**
     * Checks if the application is running in test mode.
     * 
     * @return true if in test profile
     */
    public boolean isTestMode() {
        return "test".equals(getActiveProfile());
    }

    /**
     * Checks if the application is running in production mode.
     * 
     * @return true if in production profile
     */
    public boolean isProductionMode() {
        return "prod".equals(getActiveProfile());
    }

    /**
     * Gets profile-specific configuration adjustments.
     * 
     * @return ProfileSettings for the current profile
     */
    public ProfileSettings getProfileSettings() {
        String profile = getActiveProfile();
        
        switch (profile) {
            case "dev":
                return createDevelopmentSettings();
            case "test":
                return createTestSettings();
            case "prod":
                return createProductionSettings();
            default:
                LOG.warnf("Unknown profile '%s', using default settings", profile);
                return createDefaultSettings();
        }
    }

    private ProfileSettings createDevelopmentSettings() {
        return ProfileSettings.builder()
                .profile("dev")
                .enableDebugLogging(true)
                .enableMetrics(true)
                .enableHealthChecks(true)
                .enableCircuitBreaker(false)
                .maxConcurrentRequests(2)
                .requestDelay(java.time.Duration.ofMillis(500))
                .enableContentValidation(true)
                .enableRedis(true)
                .logLevel("DEBUG")
                .build();
    }

    private ProfileSettings createTestSettings() {
        return ProfileSettings.builder()
                .profile("test")
                .enableDebugLogging(false)
                .enableMetrics(false)
                .enableHealthChecks(false)
                .enableCircuitBreaker(false)
                .maxConcurrentRequests(1)
                .requestDelay(java.time.Duration.ofMillis(100))
                .enableContentValidation(true)
                .enableRedis(false)
                .logLevel("WARN")
                .build();
    }

    private ProfileSettings createProductionSettings() {
        return ProfileSettings.builder()
                .profile("prod")
                .enableDebugLogging(false)
                .enableMetrics(true)
                .enableHealthChecks(true)
                .enableCircuitBreaker(true)
                .maxConcurrentRequests(config.performance().maxConcurrentRequests())
                .requestDelay(config.performance().requestDelay())
                .enableContentValidation(config.content().enableContentValidation())
                .enableRedis(config.redis().enableRedis())
                .logLevel("INFO")
                .build();
    }

    private ProfileSettings createDefaultSettings() {
        return ProfileSettings.builder()
                .profile("default")
                .enableDebugLogging(false)
                .enableMetrics(true)
                .enableHealthChecks(true)
                .enableCircuitBreaker(true)
                .maxConcurrentRequests(config.performance().maxConcurrentRequests())
                .requestDelay(config.performance().requestDelay())
                .enableContentValidation(config.content().enableContentValidation())
                .enableRedis(config.redis().enableRedis())
                .logLevel("INFO")
                .build();
    }

    /**
     * Profile-specific settings that can override base configuration.
     */
    public static class ProfileSettings {
        private final String profile;
        private final boolean enableDebugLogging;
        private final boolean enableMetrics;
        private final boolean enableHealthChecks;
        private final boolean enableCircuitBreaker;
        private final int maxConcurrentRequests;
        private final java.time.Duration requestDelay;
        private final boolean enableContentValidation;
        private final boolean enableRedis;
        private final String logLevel;

        private ProfileSettings(Builder builder) {
            this.profile = builder.profile;
            this.enableDebugLogging = builder.enableDebugLogging;
            this.enableMetrics = builder.enableMetrics;
            this.enableHealthChecks = builder.enableHealthChecks;
            this.enableCircuitBreaker = builder.enableCircuitBreaker;
            this.maxConcurrentRequests = builder.maxConcurrentRequests;
            this.requestDelay = builder.requestDelay;
            this.enableContentValidation = builder.enableContentValidation;
            this.enableRedis = builder.enableRedis;
            this.logLevel = builder.logLevel;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getProfile() { return profile; }
        public boolean isEnableDebugLogging() { return enableDebugLogging; }
        public boolean isEnableMetrics() { return enableMetrics; }
        public boolean isEnableHealthChecks() { return enableHealthChecks; }
        public boolean isEnableCircuitBreaker() { return enableCircuitBreaker; }
        public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
        public java.time.Duration getRequestDelay() { return requestDelay; }
        public boolean isEnableContentValidation() { return enableContentValidation; }
        public boolean isEnableRedis() { return enableRedis; }
        public String getLogLevel() { return logLevel; }

        public static class Builder {
            private String profile;
            private boolean enableDebugLogging;
            private boolean enableMetrics;
            private boolean enableHealthChecks;
            private boolean enableCircuitBreaker;
            private int maxConcurrentRequests;
            private java.time.Duration requestDelay;
            private boolean enableContentValidation;
            private boolean enableRedis;
            private String logLevel;

            public Builder profile(String profile) { this.profile = profile; return this; }
            public Builder enableDebugLogging(boolean enable) { this.enableDebugLogging = enable; return this; }
            public Builder enableMetrics(boolean enable) { this.enableMetrics = enable; return this; }
            public Builder enableHealthChecks(boolean enable) { this.enableHealthChecks = enable; return this; }
            public Builder enableCircuitBreaker(boolean enable) { this.enableCircuitBreaker = enable; return this; }
            public Builder maxConcurrentRequests(int max) { this.maxConcurrentRequests = max; return this; }
            public Builder requestDelay(java.time.Duration delay) { this.requestDelay = delay; return this; }
            public Builder enableContentValidation(boolean enable) { this.enableContentValidation = enable; return this; }
            public Builder enableRedis(boolean enable) { this.enableRedis = enable; return this; }
            public Builder logLevel(String level) { this.logLevel = level; return this; }

            public ProfileSettings build() {
                return new ProfileSettings(this);
            }
        }

        @Override
        public String toString() {
            return String.format("ProfileSettings[profile=%s, debugLogging=%s, metrics=%s, healthChecks=%s, " +
                    "circuitBreaker=%s, maxConcurrent=%d, requestDelay=%s, contentValidation=%s, redis=%s, logLevel=%s]",
                    profile, enableDebugLogging, enableMetrics, enableHealthChecks, enableCircuitBreaker,
                    maxConcurrentRequests, requestDelay, enableContentValidation, enableRedis, logLevel);
        }
    }
}