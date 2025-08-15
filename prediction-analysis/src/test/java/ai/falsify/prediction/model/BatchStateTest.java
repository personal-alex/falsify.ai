package ai.falsify.prediction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchStateTest {

    @Test
    void testIsTerminal() {
        assertTrue(BatchState.COMPLETED.isTerminal());
        assertTrue(BatchState.FAILED.isTerminal());
        assertTrue(BatchState.CANCELLED.isTerminal());
        assertTrue(BatchState.TIMEOUT.isTerminal());
        
        assertFalse(BatchState.SUBMITTED.isTerminal());
        assertFalse(BatchState.PROCESSING.isTerminal());
    }

    @Test
    void testIsSuccessful() {
        assertTrue(BatchState.COMPLETED.isSuccessful());
        
        assertFalse(BatchState.SUBMITTED.isSuccessful());
        assertFalse(BatchState.PROCESSING.isSuccessful());
        assertFalse(BatchState.FAILED.isSuccessful());
        assertFalse(BatchState.CANCELLED.isSuccessful());
        assertFalse(BatchState.TIMEOUT.isSuccessful());
    }

    @Test
    void testIsActive() {
        assertTrue(BatchState.SUBMITTED.isActive());
        assertTrue(BatchState.PROCESSING.isActive());
        
        assertFalse(BatchState.COMPLETED.isActive());
        assertFalse(BatchState.FAILED.isActive());
        assertFalse(BatchState.CANCELLED.isActive());
        assertFalse(BatchState.TIMEOUT.isActive());
    }

    @Test
    void testIsFailure() {
        assertTrue(BatchState.FAILED.isFailure());
        assertTrue(BatchState.CANCELLED.isFailure());
        assertTrue(BatchState.TIMEOUT.isFailure());
        
        assertFalse(BatchState.SUBMITTED.isFailure());
        assertFalse(BatchState.PROCESSING.isFailure());
        assertFalse(BatchState.COMPLETED.isFailure());
    }
}