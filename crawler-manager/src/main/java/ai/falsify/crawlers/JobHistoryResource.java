package ai.falsify.crawlers;

import ai.falsify.crawlers.model.JobStatus;
import ai.falsify.crawlers.service.JobTrackerService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource for job history and status operations.
 * Provides endpoints to retrieve job information and history.
 */
@Path("/api/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobHistoryResource {
    
    @Inject
    JobTrackerService jobTrackerService;
    
    /**
     * Gets recent jobs for a specific crawler (last 5 jobs).
     */
    @GET
    @Path("/crawler/{crawlerId}/recent")
    public Response getRecentJobs(@PathParam("crawlerId") String crawlerId) {
        try {
            List<JobStatus> recentJobs = jobTrackerService.getRecentJobs(crawlerId);
            Log.debugf("Retrieved %d recent jobs for crawler %s", recentJobs.size(), crawlerId);
            return Response.ok(recentJobs).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve recent jobs for crawler %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve recent jobs", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets job history for a specific crawler with pagination.
     */
    @GET
    @Path("/crawler/{crawlerId}")
    public Response getJobHistory(@PathParam("crawlerId") String crawlerId,
                                 @QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("10") int size) {
        try {
            // Validate pagination parameters
            if (page < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Page number must be non-negative"))
                        .build();
            }
            
            if (size <= 0 || size > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Page size must be between 1 and 100"))
                        .build();
            }
            
            List<JobStatus> jobHistory = jobTrackerService.getJobHistory(crawlerId, page, size);
            Log.debugf("Retrieved %d jobs for crawler %s (page %d, size %d)", 
                      jobHistory.size(), crawlerId, page, size);
            
            return Response.ok(jobHistory).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve job history for crawler %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve job history", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets a specific job by ID.
     */
    @GET
    @Path("/{jobId}")
    public Response getJob(@PathParam("jobId") String jobId) {
        try {
            Optional<JobStatus> job = jobTrackerService.getJob(jobId);
            
            if (job.isPresent()) {
                Log.debugf("Retrieved job %s", jobId);
                return Response.ok(job.get()).build();
            } else {
                Log.warnf("Job not found: %s", jobId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Job not found", "jobId", jobId))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve job %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve job", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets all currently running jobs.
     */
    @GET
    @Path("/running")
    public Response getRunningJobs() {
        try {
            List<JobStatus> runningJobs = jobTrackerService.getRunningJobs();
            Log.debugf("Retrieved %d running jobs", runningJobs.size());
            return Response.ok(runningJobs).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve running jobs");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve running jobs", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Gets currently running jobs for a specific crawler.
     */
    @GET
    @Path("/crawler/{crawlerId}/running")
    public Response getRunningJobs(@PathParam("crawlerId") String crawlerId) {
        try {
            List<JobStatus> runningJobs = jobTrackerService.getRunningJobs(crawlerId);
            Log.debugf("Retrieved %d running jobs for crawler %s", runningJobs.size(), crawlerId);
            return Response.ok(runningJobs).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to retrieve running jobs for crawler %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve running jobs", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Cancels a running job.
     */
    @POST
    @Path("/{jobId}/cancel")
    public Response cancelJob(@PathParam("jobId") String jobId) {
        try {
            Optional<JobStatus> job = jobTrackerService.getJob(jobId);
            
            if (job.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Job not found", "jobId", jobId))
                        .build();
            }
            
            if (!job.get().isRunning()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Job is not running", 
                                      "jobId", jobId, 
                                      "status", job.get().status.toString()))
                        .build();
            }
            
            jobTrackerService.cancelJob(jobId);
            Log.infof("Cancelled job %s", jobId);
            
            return Response.ok(Map.of("message", "Job cancelled successfully", "jobId", jobId))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to cancel job %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to cancel job", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Checks if a crawler has any running jobs.
     */
    @GET
    @Path("/crawler/{crawlerId}/has-running")
    public Response hasRunningJobs(@PathParam("crawlerId") String crawlerId) {
        try {
            boolean hasRunning = jobTrackerService.hasRunningJobs(crawlerId);
            return Response.ok(Map.of("crawlerId", crawlerId, "hasRunningJobs", hasRunning))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to check running jobs for crawler %s", crawlerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to check running jobs", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Updates job progress. Called by crawlers to report progress.
     */
    @PUT
    @Path("/{jobId}/progress")
    public Response updateJobProgress(@PathParam("jobId") String jobId, 
                                     Map<String, Object> progressData) {
        try {
            // Validate required fields
            if (!progressData.containsKey("articlesProcessed") || 
                !progressData.containsKey("articlesSkipped") || 
                !progressData.containsKey("articlesFailed")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Missing required progress fields"))
                        .build();
            }
            
            int articlesProcessed = ((Number) progressData.get("articlesProcessed")).intValue();
            int articlesSkipped = ((Number) progressData.get("articlesSkipped")).intValue();
            int articlesFailed = ((Number) progressData.get("articlesFailed")).intValue();
            String currentActivity = (String) progressData.getOrDefault("currentActivity", "Processing");
            
            jobTrackerService.trackJobProgress(jobId, articlesProcessed, articlesSkipped, 
                                             articlesFailed, currentActivity);
            
            Log.debugf("Updated progress for job %s: processed=%d, skipped=%d, failed=%d", 
                      jobId, articlesProcessed, articlesSkipped, articlesFailed);
            
            return Response.ok(Map.of("message", "Progress updated successfully", "jobId", jobId))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to update progress for job %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update job progress", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Marks a job as completed. Called by crawlers when they finish successfully.
     */
    @PUT
    @Path("/{jobId}/complete")
    public Response completeJob(@PathParam("jobId") String jobId, 
                               Map<String, Object> completionData) {
        try {
            // Validate required fields
            if (!completionData.containsKey("articlesProcessed") || 
                !completionData.containsKey("articlesSkipped") || 
                !completionData.containsKey("articlesFailed")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Missing required completion fields"))
                        .build();
            }
            
            int articlesProcessed = ((Number) completionData.get("articlesProcessed")).intValue();
            int articlesSkipped = ((Number) completionData.get("articlesSkipped")).intValue();
            int articlesFailed = ((Number) completionData.get("articlesFailed")).intValue();
            
            jobTrackerService.trackJobCompletion(jobId, articlesProcessed, articlesSkipped, articlesFailed);
            
            Log.infof("Completed job %s: processed=%d, skipped=%d, failed=%d", 
                     jobId, articlesProcessed, articlesSkipped, articlesFailed);
            
            return Response.ok(Map.of("message", "Job completed successfully", "jobId", jobId))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to complete job %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to complete job", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Marks a job as failed. Called by crawlers when they encounter an error.
     */
    @PUT
    @Path("/{jobId}/fail")
    public Response failJob(@PathParam("jobId") String jobId, 
                           Map<String, Object> failureData) {
        try {
            String errorMessage = (String) failureData.getOrDefault("errorMessage", "Unknown error");
            
            jobTrackerService.trackJobFailure(jobId, errorMessage);
            
            Log.warnf("Failed job %s: %s", jobId, errorMessage);
            
            return Response.ok(Map.of("message", "Job failure recorded", "jobId", jobId))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to record job failure for %s", jobId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to record job failure", 
                                  "message", e.getMessage()))
                    .build();
        }
    }
}