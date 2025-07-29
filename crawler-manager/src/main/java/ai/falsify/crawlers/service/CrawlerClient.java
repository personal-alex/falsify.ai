package ai.falsify.crawlers.service;

import ai.falsify.crawlers.model.CrawlRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.concurrent.CompletionStage;

/**
 * REST client interface for communicating with crawler instances.
 * Uses Quarkus REST Client to make HTTP requests to crawler endpoints.
 * Supports both health checks and crawl operations.
 */
@RegisterRestClient(configKey = "crawler-proxy")
public interface CrawlerClient {
    
    // Caspit crawler endpoints
    @GET
    @Path("/caspit/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkCaspitHealth();
    
    @GET
    @Path("/caspit/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkCaspitHealthSync();
    
    @POST
    @Path("/caspit/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> triggerCaspitCrawl(CrawlRequest request);
    
    @POST
    @Path("/caspit/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response triggerCaspitCrawlSync(CrawlRequest request);
    
    @GET
    @Path("/caspit/status")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> getCaspitStatus();
    
    @GET
    @Path("/caspit/status")
    @Produces(MediaType.APPLICATION_JSON)
    Response getCaspitStatusSync();
    
    // Drucker crawler endpoints
    @GET
    @Path("/drucker/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkDruckerHealth();
    
    @GET
    @Path("/drucker/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkDruckerHealthSync();
    
    @POST
    @Path("/drucker/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> triggerDruckerCrawl(CrawlRequest request);
    
    @POST
    @Path("/drucker/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response triggerDruckerCrawlSync(CrawlRequest request);
    
    @GET
    @Path("/drucker/status")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> getDruckerStatus();
    
    @GET
    @Path("/drucker/status")
    @Produces(MediaType.APPLICATION_JSON)
    Response getDruckerStatusSync();
    
    // Generic endpoints for testing
    @GET
    @Path("/test/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkTestHealth();
    
    @GET
    @Path("/test/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkTestHealthSync();
    
    @POST
    @Path("/test/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> triggerTestCrawl(CrawlRequest request);
    
    @POST
    @Path("/test/crawl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response triggerTestCrawlSync(CrawlRequest request);
    
    @GET
    @Path("/test/status")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> getTestStatus();
    
    @GET
    @Path("/test/status")
    @Produces(MediaType.APPLICATION_JSON)
    Response getTestStatusSync();
}