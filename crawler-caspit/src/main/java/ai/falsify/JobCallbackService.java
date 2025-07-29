package ai.falsify;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Service for making callback requests to the crawler manager
 * to report job progress and completion status.
 */
@ApplicationScoped
public class JobCallbackService {
    
    private static final Logger LOG = Logger.getLogger(JobCallbackService.class);
    
    private final Client httpClient;
    
    public JobCallbackService() {
        this.httpClient = ClientBuilder.newClient();
    }
    
    /**
     * Report job progress to the manager
     */
    public void reportProgress(String callbackUrl, String jobId, int articlesProcessed, 
                              int articlesSkipped, int articlesFailed, String currentActivity) {
        if (callbackUrl == null || jobId == null) {
            LOG.debug("Skipping progress report - no callback URL or job ID provided");
            return;
        }
        
        try {
            Map<String, Object> progressData = Map.of(
                "articlesProcessed", articlesProcessed,
                "articlesSkipped", articlesSkipped,
                "articlesFailed", articlesFailed,
                "currentActivity", currentActivity != null ? currentActivity : "Processing"
            );
            
            String url = callbackUrl + "/" + jobId + "/progress";
            
            Response response = httpClient.target(url)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(progressData));
            
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                LOG.debugf("Successfully reported progress for job %s: processed=%d, skipped=%d, failed=%d", 
                          jobId, articlesProcessed, articlesSkipped, articlesFailed);
            } else {
                LOG.warnf("Failed to report progress for job %s: HTTP %d", jobId, response.getStatus());
            }
            
            response.close();
            
        } catch (Exception e) {
            LOG.errorf("Error reporting progress for job %s: %s", jobId, e.getMessage(), e);
        }
    }
    
    /**
     * Report job completion to the manager
     */
    public void reportCompletion(String callbackUrl, String jobId, int articlesProcessed, 
                                int articlesSkipped, int articlesFailed) {
        if (callbackUrl == null || jobId == null) {
            LOG.debug("Skipping completion report - no callback URL or job ID provided");
            return;
        }
        
        try {
            Map<String, Object> completionData = Map.of(
                "articlesProcessed", articlesProcessed,
                "articlesSkipped", articlesSkipped,
                "articlesFailed", articlesFailed
            );
            
            String url = callbackUrl + "/" + jobId + "/complete";
            
            Response response = httpClient.target(url)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(completionData));
            
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                LOG.infof("Successfully reported completion for job %s: processed=%d, skipped=%d, failed=%d", 
                         jobId, articlesProcessed, articlesSkipped, articlesFailed);
            } else {
                LOG.warnf("Failed to report completion for job %s: HTTP %d", jobId, response.getStatus());
            }
            
            response.close();
            
        } catch (Exception e) {
            LOG.errorf("Error reporting completion for job %s: %s", jobId, e.getMessage(), e);
        }
    }
    
    /**
     * Report job failure to the manager
     */
    public void reportFailure(String callbackUrl, String jobId, String errorMessage) {
        if (callbackUrl == null || jobId == null) {
            LOG.debug("Skipping failure report - no callback URL or job ID provided");
            return;
        }
        
        try {
            Map<String, Object> failureData = Map.of(
                "errorMessage", errorMessage != null ? errorMessage : "Unknown error"
            );
            
            String url = callbackUrl + "/" + jobId + "/fail";
            
            Response response = httpClient.target(url)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(failureData));
            
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                LOG.infof("Successfully reported failure for job %s: %s", jobId, errorMessage);
            } else {
                LOG.warnf("Failed to report failure for job %s: HTTP %d", jobId, response.getStatus());
            }
            
            response.close();
            
        } catch (Exception e) {
            LOG.errorf("Error reporting failure for job %s: %s", jobId, e.getMessage(), e);
        }
    }
}