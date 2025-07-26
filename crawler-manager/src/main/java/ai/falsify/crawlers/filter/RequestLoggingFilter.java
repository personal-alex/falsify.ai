package ai.falsify.crawlers.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.Instant;

/**
 * Request/Response logging filter for debugging HTTP requests.
 * Logs incoming requests and outgoing responses with timing information.
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    
    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_START_TIME = "request.start.time";
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Only log for our API endpoints to avoid noise
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("api/")) {
            requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());
            
            LOG.debugf("Incoming request: %s %s from %s", 
                      requestContext.getMethod(), 
                      requestContext.getUriInfo().getRequestUri(),
                      getClientInfo(requestContext));
            
            // Log headers for debugging
            requestContext.getHeaders().forEach((key, values) -> 
                LOG.debugf("Request header: %s = %s", key, values)
            );
        }
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("api/")) {
            Long startTime = (Long) requestContext.getProperty(REQUEST_START_TIME);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
            
            LOG.debugf("Outgoing response: %s %s -> %d (%dms)", 
                      requestContext.getMethod(),
                      requestContext.getUriInfo().getRequestUri(),
                      responseContext.getStatus(),
                      duration);
            
            // Log response headers for debugging
            responseContext.getHeaders().forEach((key, values) -> 
                LOG.debugf("Response header: %s = %s", key, values)
            );
            
            // Log response entity for errors
            if (responseContext.getStatus() >= 400) {
                LOG.debugf("Error response entity: %s", responseContext.getEntity());
            }
        }
    }
    
    private String getClientInfo(ContainerRequestContext requestContext) {
        String userAgent = requestContext.getHeaderString("User-Agent");
        String remoteAddr = requestContext.getHeaderString("X-Forwarded-For");
        if (remoteAddr == null) {
            remoteAddr = requestContext.getHeaderString("X-Real-IP");
        }
        if (remoteAddr == null) {
            remoteAddr = "unknown";
        }
        
        return String.format("%s [%s]", remoteAddr, userAgent != null ? userAgent : "unknown");
    }
}