package ai.falsify.prediction.resource;

import ai.falsify.crawlers.common.model.AnalysisJobEntity;
import ai.falsify.crawlers.common.model.ArticleEntity;
import ai.falsify.crawlers.common.model.ArticleFilter;
import ai.falsify.crawlers.common.model.PredictionInstanceEntity;
import ai.falsify.crawlers.common.service.ArticleService;
import ai.falsify.prediction.service.PredictionAnalysisService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * REST API for prediction analysis operations.
 * Provides endpoints for starting analysis jobs, monitoring progress, and retrieving results.
 */
@Path("/api/prediction-analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PredictionAnalysisResource {
    
    private static final Logger LOG = Logger.getLogger(PredictionAnalysisResource.class);
    
    @Inject
    PredictionAnalysisService analysisService;
    
    @Inject
    ArticleService articleService;
    
    /**
     * Start a new prediction analysis job.
     * 
     * @param request Analysis request containing article IDs and analysis type
     * @return Created analysis job
     */
    @POST
    @Path("/jobs")
    public Response startAnalysis(AnalysisRequest request) {
        LOG.infof("Starting analysis for %d articles with type: %s", 
                 request.articleIds.size(), request.analysisType);
        
        try {
            // Validate request
            if (request.articleIds == null || request.articleIds.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Article IDs are required"))
                        .build();
            }
            
            if (request.analysisType == null || request.analysisType.trim().isEmpty()) {
                request.analysisType = "mock"; // Default to mock
            }
            
            // Start analysis
            AnalysisJobEntity job = analysisService.startAnalysis(request.articleIds, request.analysisType);
            
            // Return a clear response structure that matches frontend expectations
            Map<String, Object> response = Map.of(
                "id", job.id, // Database ID
                "jobId", job.jobId, // UUID job identifier
                "status", job.status.toString(),
                "totalArticles", job.totalArticles,
                "processedArticles", job.processedArticles,
                "predictionsFound", job.predictionsFound,
                "startedAt", job.startedAt.toString(),
                "message", "Analysis job started successfully"
            );
            
            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
                    
        } catch (IllegalStateException e) {
            LOG.warnf("Analysis request rejected: %s", e.getMessage());
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Invalid analysis request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error starting analysis");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to start analysis"))
                    .build();
        }
    }
    
    /**
     * Get analysis job status and details.
     * 
     * @param jobId Job ID
     * @return Analysis job details
     */
    @GET
    @Path("/jobs/{jobId}")
    public Response getJob(@PathParam("jobId") String jobId) {
        LOG.debugf("Getting job details for: %s", jobId);
        
        try {
            AnalysisJobEntity job = analysisService.getJob(jobId);
            if (job == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Job not found"))
                        .build();
            }
            
            return Response.ok(job).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting job: %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job details"))
                    .build();
        }
    }
    
    /**
     * Get analysis job results.
     * 
     * @param jobId Job ID
     * @return List of prediction results
     */
    @GET
    @Path("/jobs/{jobId}/results")
    public Response getJobResults(@PathParam("jobId") String jobId) {
        LOG.debugf("Getting results for job: %s", jobId);
        
        try {
            List<PredictionInstanceEntity> results = analysisService.getJobResults(jobId);
            return Response.ok(results).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting job results: %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job results"))
                    .build();
        }
    }
    
    /**
     * Cancel a running analysis job.
     * 
     * @param jobId Job ID
     * @return Cancellation result
     */
    @DELETE
    @Path("/jobs/{jobId}")
    public Response cancelJob(@PathParam("jobId") String jobId) {
        LOG.infof("Cancelling job: %s", jobId);
        
        try {
            boolean cancelled = analysisService.cancelJob(jobId);
            if (cancelled) {
                return Response.ok(Map.of("message", "Job cancelled successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Job not found or not running"))
                        .build();
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error cancelling job: %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to cancel job"))
                    .build();
        }
    }
    
    /**
     * Get analysis job history with pagination.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return List of analysis jobs
     */
    @GET
    @Path("/jobs")
    public Response getJobHistory(@QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("20") int size) {
        LOG.debugf("Getting job history: page=%d, size=%d", page, size);
        
        try {
            // Validate pagination parameters
            if (page < 0) page = 0;
            if (size < 1 || size > 100) size = 20;
            
            List<AnalysisJobEntity> jobs = analysisService.getJobHistory(page, size);
            return Response.ok(jobs).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting job history");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job history"))
                    .build();
        }
    }
    
    /**
     * Get system status and metrics.
     * 
     * @return System status information
     */
    @GET
    @Path("/status")
    public Response getSystemStatus() {
        LOG.debug("Getting system status");
        
        try {
            Map<String, Object> status = analysisService.getSystemStatus();
            return Response.ok(status).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting system status");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get system status"))
                    .build();
        }
    }
    
    /**
     * Get health status for monitoring.
     * 
     * @return Health status information
     */
    @GET
    @Path("/health")
    public Response getHealthStatus() {
        LOG.debug("Getting health status");
        
        try {
            Map<String, Object> status = analysisService.getSystemStatus();
            return Response.ok(status).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting health status");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get health status"))
                    .build();
        }
    }
    
    /**
     * Get extractor status for debugging.
     * 
     * @return Extractor status information
     */
    @GET
    @Path("/extractors/status")
    public Response getExtractorStatus() {
        LOG.debug("Getting extractor status");
        
        try {
            Map<String, Object> status = analysisService.getExtractorStatus();
            return Response.ok(status).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting extractor status");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get extractor status"))
                    .build();
        }
    }
    
    /**
     * Test prediction extraction with sample text.
     * 
     * @param request Test request with sample text
     * @return Prediction extraction results
     */
    @POST
    @Path("/test")
    public Response testPredictionExtraction(TestRequest request) {
        LOG.infof("Testing prediction extraction with sample text");
        
        try {
            Map<String, Object> result = analysisService.testPredictionExtraction(
                request.text != null ? request.text : "The economy will grow by 5% next year according to experts.",
                request.title != null ? request.title : "Economic Forecast Test"
            );
            return Response.ok(result).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error testing prediction extraction");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to test prediction extraction: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get configuration details for debugging.
     * 
     * @return Configuration details
     */
    @GET
    @Path("/config")
    public Response getConfiguration() {
        LOG.debug("Getting configuration details");
        
        try {
            Map<String, Object> config = analysisService.getConfigurationDetails();
            return Response.ok(config).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting configuration");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get configuration: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get articles available for analysis with filtering and pagination.
     * 
     * @param authorId Filter by author ID (optional)
     * @param titleSearch Filter by title search (optional)
     * @param fromDate Filter by date from (optional, ISO format)
     * @param toDate Filter by date to (optional, ISO format)
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of articles
     */
    @GET
    @Path("/articles")
    public Response getArticlesForAnalysis(
            @QueryParam("authorId") Long authorId,
            @QueryParam("titleSearch") String titleSearch,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        LOG.debugf("Getting articles for analysis: authorId=%s, titleSearch=%s, page=%d, size=%d", 
                  authorId, titleSearch, page, size);
        
        try {
            // Validate pagination parameters
            if (page < 0) page = 0;
            if (size < 1 || size > 100) size = 20;
            
            // Parse dates
            Instant fromInstant = null;
            Instant toInstant = null;
            
            try {
                if (fromDate != null && !fromDate.trim().isEmpty()) {
                    fromInstant = Instant.parse(fromDate);
                }
                if (toDate != null && !toDate.trim().isEmpty()) {
                    toInstant = Instant.parse(toDate);
                }
            } catch (DateTimeParseException e) {
                LOG.warnf("Invalid date format: %s", e.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid date format. Use ISO format (e.g., 2024-01-01T00:00:00Z)"))
                        .build();
            }
            
            // Create filter using builder
            ArticleFilter filter = ArticleFilter.builder()
                    .authorId(authorId)
                    .titleSearch(titleSearch)
                    .fromDate(fromInstant)
                    .toDate(toInstant)
                    .page(page)
                    .size(size)
                    .build();
            
            // Get articles
            List<ArticleEntity> articles = articleService.getArticlesForAnalysis(filter);
            long totalCount = articleService.getArticleCountForAnalysis(filter);
            
            // Calculate pagination info
            int totalPages = (int) Math.ceil((double) totalCount / size);
            boolean hasNext = page < totalPages - 1;
            boolean hasPrevious = page > 0;
            
            Map<String, Object> response = Map.of(
                "articles", articles,
                "pagination", Map.of(
                    "page", page,
                    "size", size,
                    "totalElements", totalCount,
                    "totalPages", totalPages,
                    "hasNext", hasNext,
                    "hasPrevious", hasPrevious
                )
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting articles for analysis");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get articles"))
                    .build();
        }
    }
    
    /**
     * Get all authors for filter dropdowns.
     * 
     * @return List of authors with article counts
     */
    @GET
    @Path("/authors")
    public Response getAllAuthors() {
        LOG.debug("Getting all authors for filter dropdown");
        
        try {
            var authorsWithCounts = articleService.getAuthorsWithCounts();
            
            // Transform to the format expected by frontend
            List<Map<String, Object>> authorData = authorsWithCounts.stream()
                .map(authorWithCount -> Map.of(
                    "author", Map.of(
                        "id", authorWithCount.author().id(),
                        "name", authorWithCount.author().name(),
                        "avatarUrl", authorWithCount.author().avatarUrl() != null ? authorWithCount.author().avatarUrl() : ""
                    ),
                    "articleCount", authorWithCount.articleCount()
                ))
                .toList();
            
            return Response.ok(authorData).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting authors");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get authors"))
                    .build();
        }
    }
    
    /**
     * Get extractor configurations and status.
     * 
     * @return Extractor configurations
     */
    @GET
    @Path("/extractors")
    public Response getExtractorConfigurations() {
        LOG.debug("Getting extractor configurations");
        
        try {
            Map<String, Object> extractors = analysisService.getExtractorStatus();
            return Response.ok(extractors).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting extractor configurations");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get extractor configurations"))
                    .build();
        }
    }
    
    /**
     * Get job statistics and performance metrics.
     * 
     * @return Job statistics
     */
    @GET
    @Path("/statistics")
    public Response getJobStatistics() {
        LOG.debug("Getting job statistics");
        
        try {
            // For now, return basic statistics - can be enhanced later
            Map<String, Object> stats = Map.of(
                "totalJobs", AnalysisJobEntity.count(),
                "runningJobs", AnalysisJobEntity.count("status = 'RUNNING'"),
                "completedJobs", AnalysisJobEntity.count("status = 'COMPLETED'"),
                "failedJobs", AnalysisJobEntity.count("status = 'FAILED'")
            );
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting job statistics");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get job statistics"))
                    .build();
        }
    }
    
    /**
     * Export analysis results in various formats.
     * 
     * @param jobId Job ID
     * @param format Export format (csv, json)
     * @return Exported data as file
     */
    @GET
    @Path("/jobs/{jobId}/export")
    public Response exportAnalysisResults(
            @PathParam("jobId") String jobId,
            @QueryParam("format") @DefaultValue("csv") String format) {
        
        LOG.debugf("Exporting results for job %s in format: %s", jobId, format);
        
        try {
            // For now, return a simple message - can be enhanced later with actual export
            String content = "Export functionality not yet implemented";
            
            return Response.ok(content)
                    .header("Content-Disposition", "attachment; filename=\"analysis-" + jobId + "." + format + "\"")
                    .build();
                    
        } catch (Exception e) {
            LOG.errorf(e, "Error exporting results for job: %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to export results"))
                    .build();
        }
    }
    
    /**
     * Test request DTO.
     */
    public static class TestRequest {
        public String text;
        public String title;
        
        public TestRequest() {}
        
        public TestRequest(String text, String title) {
            this.text = text;
            this.title = title;
        }
    }
    
    /**
     * Request DTO for starting analysis.
     */
    public static class AnalysisRequest {
        public List<Long> articleIds;
        public String analysisType;
        
        public AnalysisRequest() {}
        
        public AnalysisRequest(List<Long> articleIds, String analysisType) {
            this.articleIds = articleIds;
            this.analysisType = analysisType;
        }
    }
}