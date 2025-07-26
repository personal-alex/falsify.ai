package ai.falsify.crawlers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test JSON serialization and deserialization of HealthStatus objects.
 * This test ensures that the Instant field serialization issue is resolved.
 */
class HealthStatusSerializationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    void testHealthStatusSerialization() throws Exception {
        // Given
        HealthStatus healthStatus = HealthStatus.healthy("test-crawler", 150L);
        
        // When
        String json = objectMapper.writeValueAsString(healthStatus);
        
        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"HEALTHY\""));
        assertTrue(json.contains("\"crawlerId\":\"test-crawler\""));
        assertTrue(json.contains("\"responseTimeMs\":150"));
        assertTrue(json.contains("\"lastCheck\":"));
        
        System.out.println("Serialized JSON: " + json);
    }
    
    @Test
    void testHealthStatusDeserialization() throws Exception {
        // Given
        HealthStatus original = HealthStatus.unhealthy("test-crawler", "Connection failed");
        String json = objectMapper.writeValueAsString(original);
        
        // When
        HealthStatus deserialized = objectMapper.readValue(json, HealthStatus.class);
        
        // Then
        assertNotNull(deserialized);
        assertEquals(original.status, deserialized.status);
        assertEquals(original.crawlerId, deserialized.crawlerId);
        assertEquals(original.message, deserialized.message);
        assertEquals(original.responseTimeMs, deserialized.responseTimeMs);
        assertNotNull(deserialized.lastCheck);
    }
    
    @Test
    void testHealthStatusWithNullValues() throws Exception {
        // Given
        HealthStatus healthStatus = new HealthStatus();
        healthStatus.crawlerId = "test";
        healthStatus.status = HealthStatus.Status.UNKNOWN;
        healthStatus.message = null;
        healthStatus.lastCheck = null;
        healthStatus.responseTimeMs = null;
        
        // When
        String json = objectMapper.writeValueAsString(healthStatus);
        HealthStatus deserialized = objectMapper.readValue(json, HealthStatus.class);
        
        // Then
        assertNotNull(json);
        assertNotNull(deserialized);
        assertEquals(healthStatus.status, deserialized.status);
        assertEquals(healthStatus.crawlerId, deserialized.crawlerId);
        assertNull(deserialized.message);
        assertNull(deserialized.lastCheck);
        assertNull(deserialized.responseTimeMs);
    }
    
    @Test
    void testInstantSerialization() throws Exception {
        // Given
        Instant now = Instant.now();
        HealthStatus healthStatus = new HealthStatus("test", HealthStatus.Status.HEALTHY, "OK", now, 100L);
        
        // When
        String json = objectMapper.writeValueAsString(healthStatus);
        HealthStatus deserialized = objectMapper.readValue(json, HealthStatus.class);
        
        // Then
        assertNotNull(deserialized.lastCheck);
        assertEquals(now.getEpochSecond(), deserialized.lastCheck.getEpochSecond());
        
        System.out.println("Instant serialized as: " + json);
    }
}