package ai.falsify.crawlers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.test.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test resource to serve HTML fixtures for integration testing.
 * This replaces the need for a separate Jetty server.
 */
@Mock
@Path("/html-fixtures")
public class TestHtmlResource {

    @GET
    @Path("/{filename}")
    @Produces(MediaType.TEXT_HTML)
    public Response getHtmlFixture(@PathParam("filename") String filename) {
        try {
            // Load HTML fixture from test resources
            String resourcePath = "/html-fixtures/" + filename;
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("<html><body><h1>404 - File not found</h1></body></html>")
                    .build();
            }
            
            String htmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();
            
            return Response.ok(htmlContent)
                .header("Content-Type", "text/html; charset=utf-8")
                .build();
                
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("<html><body><h1>500 - Internal Server Error</h1></body></html>")
                .build();
        }
    }
    
    @GET
    @Path("/empty-page.html")
    @Produces(MediaType.TEXT_HTML)
    public Response getEmptyPage() {
        String emptyPageContent = """
            <!DOCTYPE html>
            <html>
            <head><title>Empty Page</title></head>
            <body>
                <div class="main-content">
                    <p>No articles here</p>
                </div>
            </body>
            </html>
            """;
        
        return Response.ok(emptyPageContent)
            .header("Content-Type", "text/html; charset=utf-8")
            .build();
    }
}