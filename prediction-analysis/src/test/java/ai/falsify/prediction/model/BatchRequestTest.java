package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatchRequestTest {

    @Test
    void testValidConstruction() {
        Map<String, Object> params = Map.of("temperature", 0.7, "maxTokens", 1000);
        BatchRequest request = new BatchRequest("req-123", "article-456", "Analyze this text", params);
        
        assertEquals("req-123", request.requestId());
        assertEquals("article-456", request.articleId());
        assertEquals("Analyze this text", request.prompt());
        assertEquals(params, request.parameters());
    }

    @Test
    void testValidationRequestIdRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest(null, "article-456", "prompt", Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("", "article-456", "prompt", Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("   ", "article-456", "prompt", Map.of())
        );
    }

    @Test
    void testValidationArticleIdRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("req-123", null, "prompt", Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("req-123", "", "prompt", Map.of())
        );
    }

    @Test
    void testValidationPromptRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("req-123", "article-456", null, Map.of())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchRequest("req-123", "article-456", "", Map.of())
        );
    }

    @Test
    void testNullParametersHandled() {
        BatchRequest request = new BatchRequest("req-123", "article-456", "prompt", null);
        
        assertEquals(Map.of(), request.parameters());
        assertFalse(request.hasParameter("any"));
    }

    @Test
    void testSimpleFactory() {
        BatchRequest request = BatchRequest.simple("req-123", "article-456", "prompt");
        
        assertEquals("req-123", request.requestId());
        assertEquals("article-456", request.articleId());
        assertEquals("prompt", request.prompt());
        assertEquals(Map.of(), request.parameters());
    }

    @Test
    void testWithParametersFactory() {
        Map<String, Object> params = Map.of("temperature", 0.8);
        BatchRequest request = BatchRequest.withParameters("req-123", "article-456", "prompt", params);
        
        assertEquals(params, request.parameters());
    }

    @Test
    void testGetParameter() {
        Map<String, Object> params = Map.of("temperature", 0.7, "maxTokens", 1000);
        BatchRequest request = new BatchRequest("req-123", "article-456", "prompt", params);
        
        assertEquals(0.7, request.getParameter("temperature"));
        assertEquals(1000, request.getParameter("maxTokens"));
        assertNull(request.getParameter("nonexistent"));
    }

    @Test
    void testGetParameterAsString() {
        Map<String, Object> params = Map.of("model", "gpt-4", "temperature", 0.7);
        BatchRequest request = new BatchRequest("req-123", "article-456", "prompt", params);
        
        assertEquals("gpt-4", request.getParameterAsString("model", "default"));
        assertEquals("0.7", request.getParameterAsString("temperature", "default"));
        assertEquals("default", request.getParameterAsString("nonexistent", "default"));
    }

    @Test
    void testGetParameterAsInteger() {
        Map<String, Object> params = Map.of("maxTokens", 1000, "temperature", 0.7, "stringNumber", "500");
        BatchRequest request = new BatchRequest("req-123", "article-456", "prompt", params);
        
        assertEquals(1000, request.getParameterAsInteger("maxTokens", 100));
        assertEquals(0, request.getParameterAsInteger("temperature", 100)); // Double converted to int
        assertEquals(500, request.getParameterAsInteger("stringNumber", 100));
        assertEquals(100, request.getParameterAsInteger("nonexistent", 100));
        
        // Test invalid string number
        Map<String, Object> invalidParams = Map.of("invalid", "not-a-number");
        BatchRequest invalidRequest = new BatchRequest("req-123", "article-456", "prompt", invalidParams);
        assertEquals(100, invalidRequest.getParameterAsInteger("invalid", 100));
    }

    @Test
    void testHasParameter() {
        Map<String, Object> params = Map.of("temperature", 0.7);
        BatchRequest request = new BatchRequest("req-123", "article-456", "prompt", params);
        
        assertTrue(request.hasParameter("temperature"));
        assertFalse(request.hasParameter("nonexistent"));
    }

    @Test
    void testGetPromptLength() {
        BatchRequest request = new BatchRequest("req-123", "article-456", "Hello world", Map.of());
        
        assertEquals(11, request.getPromptLength());
    }

    @Test
    void testIsLargePrompt() {
        String smallPrompt = "Small prompt";
        String largePrompt = "x".repeat(15000);
        
        BatchRequest smallRequest = new BatchRequest("req-123", "article-456", smallPrompt, Map.of());
        BatchRequest largeRequest = new BatchRequest("req-123", "article-456", largePrompt, Map.of());
        
        assertFalse(smallRequest.isLargePrompt());
        assertTrue(largeRequest.isLargePrompt());
    }

    @Test
    void testPromptSanitization() {
        String dirtyPrompt = "Hello\u0000world\u0001test\u001F";
        BatchRequest request = new BatchRequest("req-123", "article-456", dirtyPrompt, Map.of());
        
        assertEquals("Helloworldtest", request.prompt());
    }

    @Test
    void testPromptTruncation() {
        String veryLongPrompt = "x".repeat(60000);
        BatchRequest request = new BatchRequest("req-123", "article-456", veryLongPrompt, Map.of());
        
        assertTrue(request.prompt().length() <= 50000);
        assertTrue(request.prompt().endsWith("..."));
    }

    @Test
    void testFieldTrimming() {
        BatchRequest request = new BatchRequest("  req-123  ", "  article-456  ", "  prompt  ", Map.of());
        
        assertEquals("req-123", request.requestId());
        assertEquals("article-456", request.articleId());
        assertEquals("prompt", request.prompt());
    }
}