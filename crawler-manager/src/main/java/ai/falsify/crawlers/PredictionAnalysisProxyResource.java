package ai.falsify.crawlers;

import ai.falsify.crawlers.client.PredictionAnalysisClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Proxy resource that forwards prediction-analysis requests to the
 * prediction-analysis module.
 * This allows the frontend to make all API calls through the crawler-manager on
 * port 8082,
 * while the actual prediction-analysis service runs on port 8083.
 */
@Path("/api/prediction-analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PredictionAnalysisProxyResource {

    private static final Logger LOG = Logger.getLogger(PredictionAnalysisProxyResource.class);

    @Inject
    @RestClient
    PredictionAnalysisClient predictionAnalysisClient;

    /**
     * Get articles for analysis.
     */
    @GET
    @Path("/articles")
    public Response getArticles(@QueryParam("authorId") Long authorId,
            @QueryParam("titleSearch") String titleSearch,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {

        LOG.debugf("Proxying articles request: authorId=%s, titleSearch=%s, page=%s, size=%s",
                authorId, titleSearch, page, size);

        try {
            return predictionAnalysisClient.getArticles(authorId, titleSearch, fromDate, toDate, page, size);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying articles request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get articles from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get authors for filtering.
     */
    @GET
    @Path("/authors")
    public Response getAuthors() {
        LOG.debug("Proxying authors request");

        try {
            return predictionAnalysisClient.getAuthors();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying authors request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get authors from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get job history.
     */
    @GET
    @Path("/jobs")
    public Response getJobHistory(@QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        LOG.debugf("Proxying job history request: page=%s, size=%s", page, size);

        try {
            return predictionAnalysisClient.getJobHistory(page, size);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying job history request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job history from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get job details.
     */
    @GET
    @Path("/jobs/{jobId}")
    public Response getJob(@PathParam("jobId") String jobId) {
        LOG.debugf("Proxying job details request: jobId=%s", jobId);

        try {
            return predictionAnalysisClient.getJob(jobId);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying job details request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job details from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get job results.
     */
    @GET
    @Path("/jobs/{jobId}/results")
    public Response getJobResults(@PathParam("jobId") String jobId,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("minRating") Double minRating) {
        LOG.debugf("Proxying job results request: jobId=%s, page=%s, size=%s", jobId, page, size);

        try {
            return predictionAnalysisClient.getJobResults(jobId, page, size, minRating);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying job results request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job results from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get system status.
     */
    @GET
    @Path("/status")
    public Response getSystemStatus() {
        LOG.debug("Proxying system status request");

        try {
            return predictionAnalysisClient.getSystemStatus();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying system status request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get system status from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get health status.
     */
    @GET
    @Path("/health")
    public Response getHealthStatus() {
        LOG.debug("Proxying health status request");

        try {
            return predictionAnalysisClient.getSystemStatus();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying health status request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get health status from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get extractor configurations.
     */
    @GET
    @Path("/extractors")
    public Response getExtractorConfigurations() {
        LOG.debug("Proxying extractor configurations request");

        try {
            return predictionAnalysisClient.getExtractorConfigurations();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying extractor configurations request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get extractor configurations from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get job statistics.
     */
    @GET
    @Path("/statistics")
    public Response getJobStatistics() {
        LOG.debug("Proxying job statistics request");

        try {
            return predictionAnalysisClient.getJobStatistics();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying job statistics request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job statistics from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Export job results.
     */
    @GET
    @Path("/jobs/{jobId}/export")
    public Response exportResults(@PathParam("jobId") String jobId,
            @QueryParam("format") String format) {
        LOG.debugf("Proxying export request: jobId=%s, format=%s", jobId, format);

        try {
            return predictionAnalysisClient.exportResults(jobId, format);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying export request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to export results from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get configuration.
     */
    @GET
    @Path("/config")
    public Response getConfiguration() {
        LOG.debug("Proxying configuration request");

        try {
            return predictionAnalysisClient.getConfiguration();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying configuration request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get configuration from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Get extractor status.
     */
    @GET
    @Path("/extractors/status")
    public Response getExtractorStatus() {
        LOG.debug("Proxying extractor status request");

        try {
            return predictionAnalysisClient.getExtractorStatus();
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying extractor status request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get extractor status from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Start analysis job.
     */
    @POST
    @Path("/jobs")
    public Response startAnalysis(String body) {
        LOG.debugf("Proxying start analysis request with body: %s", body != null ? "present" : "null");

        try {
            return predictionAnalysisClient.startAnalysis(body);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying start analysis request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to start analysis from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Test prediction extraction.
     */
    @POST
    @Path("/test")
    public Response testPredictionExtraction(String body) {
        LOG.debugf("Proxying test prediction extraction request");

        try {
            return predictionAnalysisClient.testPredictionExtraction(body);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying test prediction extraction request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to test prediction extraction from prediction-analysis service"))
                    .build();
        }
    }

    /**
     * Cancel analysis job.
     */
    @DELETE
    @Path("/jobs/{jobId}")
    public Response cancelJob(@PathParam("jobId") String jobId) {
        LOG.debugf("Proxying cancel job request: jobId=%s", jobId);

        try {
            return predictionAnalysisClient.cancelJob(jobId);
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying cancel job request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to cancel job from prediction-analysis service"))
                    .build();
        }
    }
}