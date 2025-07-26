package ai.falsify.crawlers.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for REST endpoints.
 * Catches unhandled exceptions and provides structured error responses.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);
    
    @Override
    public Response toResponse(Exception exception) {
        LOG.errorf(exception, "Unhandled exception in REST endpoint: %s", exception.getMessage());
        
        // Log full stack trace for debugging
        LOG.debug("Full exception stack trace:", exception);
        
        // Determine response status based on exception type
        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        String errorType = "INTERNAL_SERVER_ERROR";
        
        if (exception instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
            errorType = "BAD_REQUEST";
        } else if (exception instanceof SecurityException) {
            status = Response.Status.FORBIDDEN;
            errorType = "FORBIDDEN";
        } else if (exception instanceof UnsupportedOperationException) {
            status = Response.Status.NOT_IMPLEMENTED;
            errorType = "NOT_IMPLEMENTED";
        }
        
        // Create structured error response
        Map<String, Object> errorResponse = Map.of(
            "error", "An error occurred while processing the request",
            "message", exception.getMessage() != null ? exception.getMessage() : "Unknown error",
            "type", errorType,
            "exceptionClass", exception.getClass().getSimpleName(),
            "timestamp", Instant.now().toString(),
            "path", "Unknown" // Could be enhanced to include request path
        );
        
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}