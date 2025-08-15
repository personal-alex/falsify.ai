package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchResponseTest {

    @Test
    void testValidSuccessfulConstruction() {
        BatchResponse response = new BatchResponse("req-123", "Analysis result", true, null);
        
        assertEquals("req-123", response.requestId());
        assertEquals("Analysis result", response.response());
        assertTrue(response.success());
        assertNull(response.errorMessage());
    }

    @Test
    void testValidFailedConstruction() {
        BatchResponse response = new BatchResponse("req-123", null, false, "API Error");
        
        assertEquals("req-123", response.requestId());
        assertNull(response.response());
        assertFalse(response.success());
        assertEquals("API Error", response.errorMessage());
    }

    @Test
    void testValidationRequestIdRequired() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse(null, "response", true, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("", "response", true, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("   ", "response", true, null)
        );
    }

    @Test
    void testValidationSuccessfulResponseMustHaveContent() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", null, true, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", "", true, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", "   ", true, null)
        );
    }

    @Test
    void testValidationFailedResponseMustHaveError() {
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", null, false, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", null, false, "")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BatchResponse("req-123", null, false, "   ")
        );
    }

    @Test
    void testSuccessFactory() {
        BatchResponse response = BatchResponse.success("req-123", "Analysis result");
        
        assertEquals("req-123", response.requestId());
        assertEquals("Analysis result", response.response());
        assertTrue(response.success());
        assertNull(response.errorMessage());
    }

    @Test
    void testFailureFactory() {
        BatchResponse response = BatchResponse.failure("req-123", "API Error");
        
        assertEquals("req-123", response.requestId());
        assertNull(response.response());
        assertFalse(response.success());
        assertEquals("API Error", response.errorMessage());
    }

    @Test
    void testPartialFailureFactory() {
        BatchResponse response = BatchResponse.partialFailure("req-123", "Partial result", "Timeout error");
        
        assertEquals("req-123", response.requestId());
        assertEquals("Partial result", response.response());
        assertFalse(response.success());
        assertEquals("Timeout error", response.errorMessage());
    }

    @Test
    void testGetResponseOrEmpty() {
        BatchResponse withResponse = new BatchResponse("req-123", "content", true, null);
        BatchResponse withoutResponse = new BatchResponse("req-123", null, false, "error");
        
        assertEquals("content", withResponse.getResponseOrEmpty());
        assertEquals("", withoutResponse.getResponseOrEmpty());
    }

    @Test
    void testGetErrorMessageOrEmpty() {
        BatchResponse withError = new BatchResponse("req-123", null, false, "error message");
        BatchResponse withoutError = new BatchResponse("req-123", "content", true, null);
        
        assertEquals("error message", withError.getErrorMessageOrEmpty());
        assertEquals("", withoutError.getErrorMessageOrEmpty());
    }

    @Test
    void testGetResponseLength() {
        BatchResponse withResponse = new BatchResponse("req-123", "Hello world", true, null);
        BatchResponse withoutResponse = new BatchResponse("req-123", null, false, "error");
        
        assertEquals(11, withResponse.getResponseLength());
        assertEquals(0, withoutResponse.getResponseLength());
    }

    @Test
    void testHasContent() {
        BatchResponse withContent = new BatchResponse("req-123", "content", true, null);
        BatchResponse withoutContent = new BatchResponse("req-123", null, false, "error");
        
        assertTrue(withContent.hasContent());
        assertFalse(withoutContent.hasContent());
    }

    @Test
    void testIsPartialResponse() {
        BatchResponse successful = new BatchResponse("req-123", "content", true, null);
        BatchResponse failed = new BatchResponse("req-123", null, false, "error");
        BatchResponse partial = new BatchResponse("req-123", "partial content", false, "error");
        
        assertFalse(successful.isPartialResponse());
        assertFalse(failed.isPartialResponse());
        assertTrue(partial.isPartialResponse());
    }

    @Test
    void testIsEmpty() {
        BatchResponse withContent = new BatchResponse("req-123", "content", true, null);
        BatchResponse withError = new BatchResponse("req-123", null, false, "error");
        
        assertFalse(withContent.isEmpty());
        assertFalse(withError.isEmpty());
    }

    @Test
    void testGetSummary() {
        BatchResponse successful = new BatchResponse("req-123", "Hello world", true, null);
        BatchResponse failed = new BatchResponse("req-123", null, false, "Network timeout");
        BatchResponse partial = new BatchResponse("req-123", "Partial", false, "Very long error message that should be truncated for summary display");
        
        assertEquals("Success: 11 chars", successful.getSummary());
        assertEquals("Failed: Network timeout", failed.getSummary());
        assertTrue(partial.getSummary().startsWith("Partial: 7 chars, Error:"));
        assertTrue(partial.getSummary().contains("..."));
    }

    @Test
    void testResponseSanitization() {
        String dirtyResponse = "Hello\u0000world\u0001test\u001F";
        BatchResponse response = new BatchResponse("req-123", dirtyResponse, true, null);
        
        assertEquals("Helloworldtest", response.response());
    }

    @Test
    void testResponseTruncation() {
        String veryLongResponse = "x".repeat(150000);
        BatchResponse response = new BatchResponse("req-123", veryLongResponse, true, null);
        
        assertTrue(response.response().length() <= 100000);
        assertTrue(response.response().endsWith("..."));
    }

    @Test
    void testErrorMessageSanitization() {
        BatchResponse response = new BatchResponse("req-123", null, false, "<script>alert('xss')</script>");
        
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;", response.errorMessage());
    }

    @Test
    void testErrorMessageTruncation() {
        String longError = "x".repeat(2500);
        BatchResponse response = new BatchResponse("req-123", null, false, longError);
        
        assertTrue(response.errorMessage().length() <= 2000);
        assertTrue(response.errorMessage().endsWith("..."));
    }

    @Test
    void testFieldTrimming() {
        BatchResponse response = new BatchResponse("  req-123  ", "  content  ", true, null);
        
        assertEquals("req-123", response.requestId());
        assertEquals("content", response.response());
    }
}