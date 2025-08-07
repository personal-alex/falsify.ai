package ai.falsify.crawlers.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for communicating with the prediction-analysis service.
 */
@RegisterRestClient(configKey = "prediction-analysis")
@Path("/api/prediction-analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PredictionAnalysisClient {
    
    @GET
    @Path("/articles")
    Response getArticles(@QueryParam("authorId") Long authorId,
                        @QueryParam("titleSearch") String titleSearch,
                        @QueryParam("fromDate") String fromDate,
                        @QueryParam("toDate") String toDate,
                        @QueryParam("page") Integer page,
                        @QueryParam("size") Integer size);
    
    @GET
    @Path("/authors")
    Response getAuthors();
    
    @POST
    @Path("/jobs")
    Response startAnalysis(String body);
    
    @GET
    @Path("/jobs")
    Response getJobHistory(@QueryParam("page") Integer page,
                          @QueryParam("size") Integer size);
    
    @GET
    @Path("/jobs/{jobId}")
    Response getJob(@PathParam("jobId") String jobId);
    
    @GET
    @Path("/jobs/{jobId}/results")
    Response getJobResults(@PathParam("jobId") String jobId,
                          @QueryParam("page") Integer page,
                          @QueryParam("size") Integer size,
                          @QueryParam("minRating") Integer minRating);
    
    @DELETE
    @Path("/jobs/{jobId}")
    Response cancelJob(@PathParam("jobId") String jobId);
    
    @GET
    @Path("/status")
    Response getSystemStatus();
    
    @GET
    @Path("/extractors")
    Response getExtractorConfigurations();
    
    @GET
    @Path("/statistics")
    Response getJobStatistics();
    
    @GET
    @Path("/jobs/{jobId}/export")
    Response exportResults(@PathParam("jobId") String jobId,
                          @QueryParam("format") String format);
    
    @POST
    @Path("/test")
    Response testPredictionExtraction(String body);
    
    @GET
    @Path("/config")
    Response getConfiguration();
    
    @GET
    @Path("/extractors/status")
    Response getExtractorStatus();
}