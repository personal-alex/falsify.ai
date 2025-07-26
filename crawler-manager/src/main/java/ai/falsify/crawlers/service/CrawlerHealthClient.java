package ai.falsify.crawlers.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.concurrent.CompletionStage;

/**
 * REST client interface for communicating with crawler health endpoints.
 * Uses Quarkus REST Client to make HTTP requests to crawler instances.
 * The health endpoint path is configurable per crawler.
 */
@RegisterRestClient(configKey = "crawler-health")
public interface CrawlerHealthClient {
    
    // Caspit crawler health endpoint
    @GET
    @Path("/caspit/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkCaspitHealth();
    
    @GET
    @Path("/caspit/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkCaspitHealthSync();
    
    // Drucker crawler health endpoint
    @GET
    @Path("/drucker/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkDruckerHealth();
    
    @GET
    @Path("/drucker/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkDruckerHealthSync();
    
    // Test crawler health endpoint
    @GET
    @Path("/test/health")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Response> checkTestHealth();
    
    @GET
    @Path("/test/health")
    @Produces(MediaType.APPLICATION_JSON)
    Response checkTestHealthSync();
}