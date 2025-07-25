package ai.falsify.crawlers;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class CaspitPageNavigator {

    private static final Logger LOG = Logger.getLogger(CaspitPageNavigator.class);

    private final CaspitCrawlerConfig config;
    private WebDriver driver;
    private WebDriverWait wait;

    // Metrics tracking for empty pages
    private int emptyPagesCount = 0;

    @Inject
    public CaspitPageNavigator(CaspitCrawlerConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initializing CaspitPageNavigator with Chrome WebDriver");
        setupWebDriver();
    }

    @PreDestroy
    public void cleanup() {
        LOG.info("Cleaning up WebDriver resources");
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                LOG.warn("Error during WebDriver cleanup", e);
            }
        }
    }

    /**
     * Get all article links from the Ben Caspit author page, handling dynamic
     * pagination
     * 
     * @param baseUrl The base URL for Ben Caspit's author page
     * @return List of article URLs found across all pages
     */
    public List<String> getAllArticleLinks(String baseUrl) {
        LOG.infof("Starting to collect article links from: %s", baseUrl);
        Set<String> allLinks = new HashSet<>();
        int maxRetries = 3;
        int navigationRetryCount = 0;

        // Reset metrics for this crawl session
        emptyPagesCount = 0;

        // Validate input
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            LOG.error("Base URL is null or empty, cannot proceed with link collection");
            return new ArrayList<>();
        }

        // Ensure WebDriver is available
        if (driver == null) {
            LOG.error("WebDriver is not initialized, attempting to reinitialize");
            try {
                setupWebDriver();
            } catch (Exception e) {
                LOG.errorf("Failed to initialize WebDriver for link collection: %s", e.getMessage());
                return new ArrayList<>();
            }
        }

        while (navigationRetryCount < maxRetries) {
            try {
                LOG.infof("Navigating to base URL (attempt %d/%d): %s", navigationRetryCount + 1, maxRetries, baseUrl);

                // Navigate to the base URL with timeout handling
                driver.get(baseUrl);

                // Wait for page to load with explicit timeout
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    LOG.debug("Page loaded successfully, body element found");
                } catch (TimeoutException e) {
                    LOG.warnf("Timeout waiting for page body to load on attempt %d: %s", navigationRetryCount + 1,
                            e.getMessage());
                    if (navigationRetryCount < maxRetries - 1) {
                        navigationRetryCount++;
                        Thread.sleep(2000); // Wait before retry
                        continue;
                    } else {
                        throw e;
                    }
                }

                // Successfully navigated, break out of retry loop
                break;

            } catch (Exception e) {
                navigationRetryCount++;
                LOG.warnf("Navigation failed on attempt %d/%d: %s", navigationRetryCount, maxRetries, e.getMessage());

                if (navigationRetryCount >= maxRetries) {
                    LOG.errorf("Failed to navigate to base URL after %d attempts: %s", maxRetries, e.getMessage());

                    // Try to reinitialize WebDriver as last resort
                    try {
                        LOG.info("Attempting WebDriver reinitialization as recovery measure");
                        reinitializeWebDriver();
                        driver.get(baseUrl);
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                        LOG.info("WebDriver reinitialization successful, navigation recovered");
                        break;
                    } catch (Exception recoveryException) {
                        LOG.errorf("WebDriver recovery failed: %s", recoveryException.getMessage());
                        return new ArrayList<>();
                    }
                }

                // Wait before retry
                try {
                    Thread.sleep(1000 * navigationRetryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Navigation retry interrupted");
                    return new ArrayList<>();
                }
            }
        }

        // Main pagination loop with comprehensive error handling
        try {
            int pageCount = 0;
            boolean hasMorePages = true;
            int consecutiveFailures = 0;
            final int maxConsecutiveFailures = 3;
            boolean earlyTerminated = false;

            while (hasMorePages && pageCount < config.maxPages() && consecutiveFailures < maxConsecutiveFailures) {
                pageCount++;
                LOG.infof("Processing page %d (max: %d, consecutive failures: %d)", pageCount, config.maxPages(),
                        consecutiveFailures);

                try {
                    // Extract links from current page
                    List<String> currentPageLinks = extractCurrentPageLinks();
                    int newLinksCount = 0;

                    for (String link : currentPageLinks) {
                        if (link != null && !link.trim().isEmpty() && allLinks.add(link.trim())) {
                            newLinksCount++;
                        }
                    }

                    LOG.infof("Found %d new article links on page %d (total: %d)",
                            newLinksCount, pageCount, allLinks.size());

                    // Track empty pages for metrics
                    if (newLinksCount == 0) {
                        emptyPagesCount++;
                        LOG.debugf("Empty page detected: page %d had no new articles (total empty pages: %d)",
                                pageCount, emptyPagesCount);
                    }

                    // Check for early termination if no new articles found
                    if (config.crawling().earlyTerminationEnabled() && newLinksCount == 0) {
                        LOG.infof(
                                "Early termination triggered: No new articles found on page %d (early termination enabled)",
                                pageCount);
                        LOG.infof("Stopping pagination to prevent unnecessary crawling. Total articles collected: %d",
                                allLinks.size());
                        earlyTerminated = true;
                        break;
                    }

                    // Reset consecutive failures on successful page processing
                    if (newLinksCount > 0 || currentPageLinks.size() > 0) {
                        consecutiveFailures = 0;
                    }

                    // Try to load more articles
                    hasMorePages = loadMoreArticles();

                    if (hasMorePages) {
                        // Wait for content to load with configurable delay
                        try {
                            Thread.sleep(config.crawling().pageDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            LOG.warn("Page delay interrupted, stopping pagination");
                            break;
                        }
                    }

                } catch (Exception pageException) {
                    consecutiveFailures++;
                    LOG.warnf("Error processing page %d (consecutive failures: %d/%d): %s",
                            pageCount, consecutiveFailures, maxConsecutiveFailures, pageException.getMessage());

                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        LOG.errorf("Too many consecutive page processing failures (%d), stopping pagination",
                                consecutiveFailures);
                        break;
                    }

                    // Try to recover by waiting and continuing
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.warn("Recovery wait interrupted");
                        break;
                    }
                }
            }

            // Log completion status with detailed reason
            if (earlyTerminated) {
                LOG.infof(
                        "Pagination completed - early termination due to no new articles found (optimization enabled)");
            } else if (pageCount >= config.maxPages()) {
                LOG.infof("Pagination completed - reached maximum pages limit (%d)", config.maxPages());
            } else if (consecutiveFailures >= maxConsecutiveFailures) {
                LOG.warnf("Pagination stopped due to consecutive failures (%d)", consecutiveFailures);
            } else {
                LOG.infof("Pagination completed naturally - no more pages available");
            }

            LOG.infof("Link collection completed. Total unique article links found: %d", allLinks.size());
            LOG.infof("Pagination metrics: %d pages processed, %d empty pages encountered", pageCount, emptyPagesCount);

        } catch (Exception e) {
            LOG.errorf("Critical error during pagination process: %s", e.getMessage(), e);

            // Log additional context for debugging
            LOG.debugf("Error context - Current page count: %d, Total links collected: %d, WebDriver status: %s",
                    0, allLinks.size(), driver != null ? "Available" : "Null");
        }

        // Validate results before returning
        List<String> result = new ArrayList<>(allLinks);
        if (result.isEmpty()) {
            LOG.warn(
                    "No article links were collected - this may indicate a problem with the site structure or selectors");
        } else {
            LOG.infof("Successfully collected %d unique article links", result.size());
        }

        return result;
    }

    /**
     * Extract article links from the current page with comprehensive error handling
     * 
     * @return List of article URLs found on the current page
     */
    private List<String> extractCurrentPageLinks() {
        List<String> links = new ArrayList<>();
        int maxRetries = 2;
        int retryCount = 0;

        // Validate WebDriver state
        if (driver == null) {
            LOG.error("WebDriver is null, cannot extract links from current page");
            return links;
        }

        while (retryCount < maxRetries) {
            try {
                LOG.debugf("Extracting links from current page (attempt %d/%d)", retryCount + 1, maxRetries);

                // Wait for page to be ready
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                } catch (TimeoutException e) {
                    LOG.warnf("Timeout waiting for page body during link extraction (attempt %d)", retryCount + 1);
                    if (retryCount < maxRetries - 1) {
                        retryCount++;
                        Thread.sleep(1000);
                        continue;
                    }
                    throw e;
                }

                // Look for articles in the four-articles-in-row sections
                List<WebElement> articleSections = null;
                try {
                    articleSections = driver.findElements(By.cssSelector("section.four-articles-in-row"));
                    LOG.debugf("Found %d four-articles-in-row sections", articleSections.size());
                } catch (Exception e) {
                    LOG.warnf("Error finding four-articles-in-row sections: %s", e.getMessage());
                    articleSections = new ArrayList<>();
                }

                // Extract links from sections
                int sectionCount = 0;
                for (WebElement section : articleSections) {
                    sectionCount++;
                    try {
                        // Find all <a> elements within each section
                        List<WebElement> articleLinks = section.findElements(By.tagName("a"));
                        LOG.debugf("Section %d contains %d link elements", sectionCount, articleLinks.size());

                        for (WebElement linkElement : articleLinks) {
                            try {
                                String href = linkElement.getAttribute("href");
                                String title = linkElement.getAttribute("title");

                                if (href != null && !href.trim().isEmpty() && isValidArticleUrl(href.trim())) {
                                    String cleanHref = href.trim();
                                    if (!links.contains(cleanHref)) {
                                        links.add(cleanHref);
                                        LOG.debugf("Found valid article: %s - %s",
                                                title != null && !title.trim().isEmpty() ? title.trim() : "No title",
                                                cleanHref);
                                    }
                                } else if (href != null) {
                                    LOG.debugf("Skipped invalid URL: %s", href);
                                }
                            } catch (Exception linkException) {
                                LOG.debugf("Error extracting href from link element in section %d: %s",
                                        sectionCount, linkException.getMessage());
                                // Continue with next link
                            }
                        }
                    } catch (Exception sectionException) {
                        LOG.warnf("Error processing section %d: %s", sectionCount, sectionException.getMessage());
                        // Continue with next section
                    }
                }

                // Fallback: if no four-articles-in-row sections found or no links extracted,
                // try other selectors
                if (links.isEmpty()) {
                    LOG.debug("No articles found in four-articles-in-row sections, trying fallback selectors");

                    String[] fallbackSelectors = {
                            "article a",
                            ".article-item a",
                            ".story-item a",
                            ".item-title a",
                            ".article-link",
                            ".news-item a",
                            ".content-item a"
                    };

                    for (String selector : fallbackSelectors) {
                        try {
                            List<WebElement> fallbackElements = driver.findElements(By.cssSelector(selector));
                            LOG.debugf("Fallback selector '%s' found %d elements", selector, fallbackElements.size());

                            for (WebElement element : fallbackElements) {
                                try {
                                    String href = element.getAttribute("href");
                                    if (href != null && !href.trim().isEmpty() && isValidArticleUrl(href.trim())) {
                                        String cleanHref = href.trim();
                                        if (!links.contains(cleanHref)) {
                                            links.add(cleanHref);
                                            LOG.debugf("Found fallback article: %s", cleanHref);
                                        }
                                    }
                                } catch (Exception elementException) {
                                    LOG.debugf("Error extracting href from fallback element: %s",
                                            elementException.getMessage());
                                }
                            }

                            // If we found links with this selector, no need to try others
                            if (!links.isEmpty()) {
                                LOG.debugf("Successfully extracted %d links using fallback selector: %s", links.size(),
                                        selector);
                                break;
                            }
                        } catch (Exception selectorException) {
                            LOG.debugf("Error with fallback selector '%s': %s", selector,
                                    selectorException.getMessage());
                        }
                    }
                }

                // Successfully extracted links, break retry loop
                break;

            } catch (Exception e) {
                retryCount++;
                LOG.warnf("Error extracting links from current page (attempt %d/%d): %s", retryCount, maxRetries,
                        e.getMessage());

                if (retryCount >= maxRetries) {
                    LOG.errorf("Failed to extract links after %d attempts: %s", maxRetries, e.getMessage());
                    break;
                }

                // Wait before retry
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Link extraction retry interrupted");
                    break;
                }
            }
        }

        // Log final results
        if (links.isEmpty()) {
            LOG.warn("No article links extracted from current page - this may indicate page structure changes");
        } else {
            LOG.debugf("Successfully extracted %d unique article links from current page", links.size());
        }

        return links;
    }

    /**
     * Attempt to load more articles by clicking pagination buttons or "load more"
     * buttons with comprehensive error handling
     * 
     * @return true if more content was loaded, false if no more pages available
     */
    private boolean loadMoreArticles() {
        if (driver == null) {
            LOG.error("WebDriver is null, cannot attempt to load more articles");
            return false;
        }

        try {
            LOG.debug("Attempting to load more articles using various strategies");

            // Look for various types of "load more" or pagination buttons
            List<String> loadMoreSelectors = List.of(
                    "button[class*='load-more']",
                    "a[class*='load-more']",
                    ".load-more",
                    "button[class*='show-more']",
                    ".show-more",
                    "a[class*='next']",
                    ".next",
                    ".pagination a[class*='next']",
                    "button[onclick*='load']",
                    ".more-articles",
                    ".load-next",
                    "[data-action*='load']");

            // Try each selector with individual error handling
            for (String selector : loadMoreSelectors) {
                try {
                    LOG.debugf("Trying load more selector: %s", selector);
                    WebElement loadMoreButton = driver.findElement(By.cssSelector(selector));

                    if (loadMoreButton != null && loadMoreButton.isDisplayed() && loadMoreButton.isEnabled()) {
                        LOG.infof("Found clickable load more button with selector: %s", selector);

                        try {
                            // Scroll to button to ensure it's in view
                            ((org.openqa.selenium.JavascriptExecutor) driver)
                                    .executeScript("arguments[0].scrollIntoView(true);", loadMoreButton);

                            // Wait a moment for any animations
                            Thread.sleep(500);

                            // Click the button
                            loadMoreButton.click();
                            LOG.infof("Successfully clicked load more button: %s", selector);

                            // Wait for new content to load with timeout
                            int waitTime = Math.max(config.crawling().pageDelay(), 1000);
                            Thread.sleep(waitTime);

                            // Verify that content actually loaded by checking if button is still there
                            // If button disappeared, content likely loaded
                            try {
                                WebElement buttonAfterClick = driver.findElement(By.cssSelector(selector));
                                if (buttonAfterClick == null || !buttonAfterClick.isDisplayed()) {
                                    LOG.debug("Load more button disappeared after click - content likely loaded");
                                    return true;
                                }
                            } catch (NoSuchElementException e) {
                                LOG.debug("Load more button no longer found after click - content loaded");
                                return true;
                            }

                            LOG.debug("Load more button still present after click - assuming content loaded");
                            return true;

                        } catch (Exception clickException) {
                            LOG.warnf("Error clicking load more button '%s': %s", selector,
                                    clickException.getMessage());
                            // Continue to try other selectors
                        }
                    } else {
                        LOG.debugf(
                                "Load more button found but not clickable with selector: %s (displayed: %s, enabled: %s)",
                                selector,
                                loadMoreButton != null ? loadMoreButton.isDisplayed() : "null",
                                loadMoreButton != null ? loadMoreButton.isEnabled() : "null");
                    }
                } catch (NoSuchElementException e) {
                    LOG.debugf("Load more button not found with selector: %s", selector);
                } catch (Exception selectorException) {
                    LOG.debugf("Error with load more selector '%s': %s", selector, selectorException.getMessage());
                }
            }

            LOG.debug("No clickable load more buttons found, trying infinite scroll");
            // If no load more button found, try scrolling to trigger infinite scroll
            return tryInfiniteScroll();

        } catch (Exception e) {
            LOG.warnf("Critical error attempting to load more articles: %s", e.getMessage());

            // Log additional context for debugging
            try {
                String currentUrl = driver.getCurrentUrl();
                String pageTitle = driver.getTitle();
                LOG.debugf("Error context - Current URL: %s, Page title: %s", currentUrl, pageTitle);
            } catch (Exception contextException) {
                LOG.debugf("Could not retrieve error context: %s", contextException.getMessage());
            }

            return false;
        }
    }

    /**
     * Try to trigger infinite scroll by scrolling to the bottom of the page with
     * comprehensive error handling
     * 
     * @return true if new content appears to have loaded
     */
    private boolean tryInfiniteScroll() {
        if (driver == null) {
            LOG.error("WebDriver is null, cannot attempt infinite scroll");
            return false;
        }

        try {
            LOG.debug("Attempting infinite scroll to load more content");

            // Validate that we can execute JavaScript
            if (!(driver instanceof org.openqa.selenium.JavascriptExecutor)) {
                LOG.warn("WebDriver does not support JavaScript execution, cannot perform infinite scroll");
                return false;
            }

            org.openqa.selenium.JavascriptExecutor jsExecutor = (org.openqa.selenium.JavascriptExecutor) driver;

            // Get current page height with error handling
            Long initialHeight = null;
            try {
                initialHeight = (Long) jsExecutor.executeScript("return document.body.scrollHeight");
                LOG.debugf("Initial page height: %d pixels", initialHeight);

                if (initialHeight == null || initialHeight <= 0) {
                    LOG.warn("Invalid initial page height, cannot determine scroll effectiveness");
                    return false;
                }
            } catch (Exception heightException) {
                LOG.warnf("Error getting initial page height: %s", heightException.getMessage());
                return false;
            }

            // Perform scroll with multiple strategies
            try {
                // Strategy 1: Scroll to bottom
                jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                LOG.debug("Executed scroll to bottom");

                // Wait for potential new content with configurable delay
                int scrollDelay = Math.max(config.crawling().scrollDelay(), 1000);
                Thread.sleep(scrollDelay);

                // Strategy 2: Additional scroll attempts if needed
                for (int i = 0; i < 3; i++) {
                    try {
                        // Scroll down by viewport height
                        jsExecutor.executeScript("window.scrollBy(0, window.innerHeight);");
                        Thread.sleep(500);
                    } catch (Exception scrollException) {
                        LOG.debugf("Error in additional scroll attempt %d: %s", i + 1, scrollException.getMessage());
                    }
                }

            } catch (Exception scrollException) {
                LOG.warnf("Error during scroll execution: %s", scrollException.getMessage());
                return false;
            }

            // Check if page height increased (indicating new content loaded)
            Long newHeight = null;
            try {
                newHeight = (Long) jsExecutor.executeScript("return document.body.scrollHeight");
                LOG.debugf("New page height after scroll: %d pixels", newHeight);

                if (newHeight == null) {
                    LOG.warn("Could not determine new page height after scroll");
                    return false;
                }
            } catch (Exception newHeightException) {
                LOG.warnf("Error getting new page height: %s", newHeightException.getMessage());
                return false;
            }

            // Determine if content was loaded
            boolean contentLoaded = newHeight > initialHeight;
            long heightDifference = newHeight - initialHeight;

            if (contentLoaded) {
                LOG.infof("Infinite scroll successful - page height increased by %d pixels (from %d to %d)",
                        heightDifference, initialHeight, newHeight);
            } else {
                LOG.debugf("Infinite scroll completed but no height change detected (height: %d)", newHeight);
            }

            return contentLoaded;

        } catch (Exception e) {
            LOG.warnf("Critical error during infinite scroll attempt: %s", e.getMessage());

            // Log additional context for debugging
            try {
                String currentUrl = driver.getCurrentUrl();
                LOG.debugf("Infinite scroll error context - Current URL: %s", currentUrl);
            } catch (Exception contextException) {
                LOG.debugf("Could not retrieve infinite scroll error context: %s", contextException.getMessage());
            }

            return false;
        }
    }

    /**
     * Check if a URL appears to be a valid article URL
     * 
     * @param url The URL to validate
     * @return true if the URL appears to be an article
     */
    private boolean isValidArticleUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Filter out non-article URLs
        String lowerUrl = url.toLowerCase();

        // Skip common non-article pages
        if (lowerUrl.contains("/category/") ||
                lowerUrl.contains("/tag/") ||
                lowerUrl.contains("/author/") ||
                lowerUrl.contains("/search/") ||
                lowerUrl.contains("javascript:") ||
                lowerUrl.startsWith("mailto:") ||
                lowerUrl.startsWith("#")) {
            return false;
        }

        // Must be from maariv.co.il domain
        if (!lowerUrl.contains("maariv.co.il")) {
            return false;
        }

        return true;
    }

    /**
     * Setup Chrome WebDriver with appropriate options
     */
    private void setupWebDriver() {
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                LOG.infof("Initializing WebDriver (attempt %d/%d)", retryCount + 1, maxRetries);

                // Setup WebDriverManager to handle Chrome driver
                WebDriverManager.chromedriver().setup();
                LOG.debug("WebDriverManager setup completed");

                // Configure Chrome options
                ChromeOptions options = new ChromeOptions();

                // Configure headless mode based on configuration
                if (config.webdriver().headless()) {
                    options.addArguments("--headless");
                    LOG.debug("WebDriver configured for headless mode");
                }

                // Add stability and security options
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-extensions");
                options.addArguments("--disable-plugins");
                options.addArguments("--disable-images");
                options.addArguments("--disable-javascript");
                options.addArguments("--disable-web-security");
                options.addArguments("--ignore-certificate-errors");
                options.addArguments("--ignore-ssl-errors");
                options.addArguments("--allow-running-insecure-content");
                options.addArguments(String.format("--window-size=%d,%d",
                        config.webdriver().windowWidth(), config.webdriver().windowHeight()));
                options.addArguments("--user-agent=" + config.webdriver().userAgent());

                LOG.debugf("Chrome options configured: headless=%s, window-size=%dx%d",
                        config.webdriver().headless(),
                        config.webdriver().windowWidth(),
                        config.webdriver().windowHeight());

                // Create WebDriver instance
                driver = new ChromeDriver(options);
                LOG.debug("ChromeDriver instance created successfully");

                // Configure timeouts with validation
                Duration pageLoadTimeout = Duration.ofMillis(Math.max(config.pageLoadTimeout(), 5000));
                Duration implicitWaitTimeout = Duration.ofSeconds(Math.max(config.webdriver().implicitWait(), 1));
                Duration elementWaitTimeout = Duration.ofSeconds(Math.max(config.webdriver().elementWait(), 3));

                driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
                driver.manage().timeouts().implicitlyWait(implicitWaitTimeout);

                LOG.debugf("WebDriver timeouts configured: pageLoad=%dms, implicitWait=%ds",
                        pageLoadTimeout.toMillis(), implicitWaitTimeout.getSeconds());

                // Create WebDriverWait instance
                wait = new WebDriverWait(driver, elementWaitTimeout);

                LOG.infof("Chrome WebDriver initialized successfully on attempt %d", retryCount + 1);
                return; // Success, exit retry loop

            } catch (Exception e) {
                lastException = e;
                retryCount++;

                LOG.warnf("WebDriver initialization failed on attempt %d/%d: %s",
                        retryCount, maxRetries, e.getMessage());

                // Clean up any partially created driver
                if (driver != null) {
                    try {
                        driver.quit();
                        LOG.debug("Cleaned up partially initialized WebDriver");
                    } catch (Exception cleanupException) {
                        LOG.debugf("Error during WebDriver cleanup: %s", cleanupException.getMessage());
                    }
                    driver = null;
                }

                // Wait before retry (exponential backoff)
                if (retryCount < maxRetries) {
                    try {
                        long waitTime = 1000L * retryCount; // 1s, 2s, 3s
                        LOG.debugf("Waiting %dms before retry", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.warn("WebDriver initialization retry interrupted");
                        break;
                    }
                }
            }
        }

        // All retries failed
        String errorMessage = String.format("Failed to initialize WebDriver after %d attempts", maxRetries);
        LOG.errorf("%s. Last error: %s", errorMessage,
                lastException != null ? lastException.getMessage() : "Unknown error");

        if (lastException != null) {
            throw new RuntimeException(errorMessage, lastException);
        } else {
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Reinitialize WebDriver if needed (for error recovery)
     */
    public void reinitializeWebDriver() {
        LOG.info("Reinitializing WebDriver");
        cleanup();
        setupWebDriver();
    }
}