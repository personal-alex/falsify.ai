package ai.falsify.crawlers;

import ai.falsify.crawlers.model.CrawlRequest;
import ai.falsify.crawlers.model.CrawlResponse;
import ai.falsify.crawlers.service.CrawlerProxyService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST resource for proxying crawler operations.
 * Provides endpoints for triggering crawls and checking crawler status through the proxy service.
 */
@Path("/api/crawlers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CrawlerProxyResource {
    
    private static final Logger LOG = Logger.getLogger(CrawlerProxyResource.class);
    
    @Inject
    CrawlerProxyService proxyService;
    
    /**
     * Triggers a crawl operation on the specified crawler.
     * 
     * @param crawlerId The ID of the crawler to trigger
     * @param request Optional request body with crawl parameters
     * @return CrawlResponse with operation result
     */
    @POST
    @Path("/{crawlerId}/crawl")
    public Response triggerCrawl(@PathParam("crawlerId") String crawlerId, 
                                @Valid CrawlRequest request) {
        String requestId = "trigger-" + System.currentTimeMillis();
        LOG.infof("Received crawl trigger request [%s] for crawler: %s", requestId, crawlerId);
        
        try {
            // Validate crawler ID first - empty path params should return 404
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                LOG.warnf("Invalid crawler ID in request [%s]", requestId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(CrawlResponse.error("unknown", requestId, 
                        "Crawler ID is required", "validation"))
                    .build();
            }
            
            // If no request body provided, create one with the crawler ID
            if (request == null) {
                request = new CrawlRequest(crawlerId);
            } else {
                // Ensure crawler ID matches path parameter
                request.crawlerId = crawlerId;
            }
            
            // Execute crawl through proxy service
            CrawlResponse response = proxyService.triggerCrawl(request);
            
            // Map response status to HTTP status
            Response.Status httpStatus = mapCrawlResponseStatus(response.status);
            
            LOG.infof("Crawl trigger request [%s] completed with status: %s", 
                requestId, response.status);
            
            return Response.status(httpStatus)
                .entity(response)
                .build();
                
        } catch (jakarta.ws.rs.BadRequestException e) {
            LOG.warnf("Bad request in crawl trigger [%s]: %s", requestId, e.getMessage());
            
            CrawlResponse errorResponse = CrawlResponse.error(
                crawlerId, requestId, 
                "Invalid request format", 
                "validation"
            );
            
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
        } catch (Exception e) {
            LOG.errorf("Unexpected error in crawl trigger [%s]: %s", requestId, e.getMessage(), e);
            
            CrawlResponse errorResponse = CrawlResponse.error(
                crawlerId, requestId, 
                "Internal server error: " + e.getMessage(), 
                "internal"
            );
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }
    
    /**
     * Gets the current status of a crawler.
     * 
     * @param crawlerId The ID of the crawler
     * @return Response containing status information
     */
    @GET
    @Path("/{crawlerId}/status")
    public Response getCrawlerStatus(@PathParam("crawlerId") String crawlerId) {
        String requestId = "status-" + System.currentTimeMillis();
        LOG.debugf("Received status request [%s] for crawler: %s", requestId, crawlerId);
        
        try {
            // Validate crawler ID - empty path params should return 404
            if (crawlerId == null || crawlerId.trim().isEmpty()) {
                LOG.warnf("Invalid crawler ID in status request [%s]", requestId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(java.util.Map.of(
                        "error", "Crawler ID is required",
                        "requestId", requestId
                    ))
                    .build();
            }
            
            // Get status through proxy service
            Response response = proxyService.getCrawlerStatus(crawlerId);
            
            LOG.debugf("Status request [%s] completed with HTTP status: %s", 
                requestId, response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            LOG.errorf("Unexpected error in status request [%s]: %s", requestId, e.getMessage(), e);
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(java.util.Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage(),
                    "crawlerId", crawlerId,
                    "requestId", requestId
                ))
                .build();
        }
    }
    
    /**
     * Triggers a crawl operation using a simple POST without request body.
     * This is a convenience endpoint for simple crawl triggers.
     * 
     * @param crawlerId The ID of the crawler to trigger
     * @return CrawlResponse with operation result
     */
    @POST
    @Path("/{crawlerId}/crawl/start")
    public Response startCrawl(@PathParam("crawlerId") String crawlerId) {
        LOG.infof("Received simple crawl start request for crawler: %s", crawlerId);
        
        // Create a simple request and delegate to the main trigger method
        CrawlRequest request = new CrawlRequest(crawlerId);
        return triggerCrawl(crawlerId, request);
    }
    
    /**
     * Maps CrawlResponse.Status to appropriate HTTP status codes.
     */
    private Response.Status mapCrawlResponseStatus(CrawlResponse.Status status) {
        switch (status) {
            case ACCEPTED:
                return Response.Status.ACCEPTED;
            case CONFLICT:
                return Response.Status.CONFLICT;
            case SERVICE_UNAVAILABLE:
                return Response.Status.SERVICE_UNAVAILABLE;
            case ERROR:
            default:
                return Response.Status.BAD_REQUEST;
        }
    }
}