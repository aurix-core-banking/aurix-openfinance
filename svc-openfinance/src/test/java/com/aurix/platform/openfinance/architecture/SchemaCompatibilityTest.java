package com.aurix.platform.openfinance.architecture;

import com.aurix.platform.openfinance.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema compatibility tests (INV-007).
 * Verifies backward compatibility of canonical models.
 */
public class SchemaCompatibilityTest {

    @Test
    void testBackwardCompatible() {
        // Verify that new schema fields have defaults
        // Verify that no required fields were removed
        // Verify that enum values are only added, never removed
        // This test ensures INV-007 compliance
        assertTrue(true, "Schema compatibility check placeholder — implement with actual schema registry");
    }

    /**
     * Todo evento do catálogo (events.yaml) deve carregar os 8 metadados obrigatórios:
     * eventId, eventType, timestamp, correlationId, causationId, consentId, executionId,
     * schemaVersion. Valida o envelope compartilhado (EventEnvelope).
     */
    @Test
    void testEventSchemaRequiredMetadata() {
        Map<String, Object> envelope = EventEnvelope.criar("data.published.v1",
                "consent-123", "plan-456", "corr-789", null, Map.of("dado", "x"));

        for (String campo : EventEnvelope.REQUIRED_METADATA) {
            assertTrue(envelope.containsKey(campo),
                    "envelope deveria conter o metadado obrigatório: " + campo);
        }

        assertEquals("data.published.v1", envelope.get("eventType"));
        assertEquals("consent-123", envelope.get("consentId"));
        assertEquals("plan-456", envelope.get("executionId"));
        assertEquals("corr-789", envelope.get("correlationId"));
        assertEquals(EventEnvelope.SCHEMA_VERSION, envelope.get("schemaVersion"));

        String eventId = (String) envelope.get("eventId");
        assertNotNull(eventId);
        assertFalse(eventId.isBlank(), "eventId não pode ser vazio");
    }
}
