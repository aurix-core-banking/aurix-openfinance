package com.aurix.platform.openfinance.architecture;

import com.aurix.platform.openfinance.event.EventEnvelope;
import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
import com.aurix.platform.openfinance.pipeline.validation.service.SchemaValidationService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema compatibility tests (INV-007).
 * Verifies backward compatibility of canonical models and schema validation.
 */
public class SchemaCompatibilityTest {

    @Test
    void testBackwardCompatible() {
        SchemaValidationService validationService = new SchemaValidationService();

        // 1. Evolução compatível: mantém todos os campos obrigatórios de data-models.yaml + adiciona campo novo
        String contaSchemaV2Compativel = """
                {"fields":["accountId","documentNumber","accountType","agency","number","status","createdAt","overdraftLimit"]}
                """;
        SchemaValidationService.SchemaCompatibility compativel =
                validationService.checkCompatibility(ResourceType.CONTA, contaSchemaV2Compativel);
        assertTrue(compativel.isCompativel(),
                "Schema v2 que preserva campos obrigatórios deve ser retrocompatível (INV-007)");

        // 2. Breaking change: remove campo obrigatório do modelo canônico (ex: accountId / status)
        String contaSchemaBreaking = """
                {"fields":["documentNumber","agency","number"]}
                """;
        SchemaValidationService.SchemaCompatibility incompativel =
                validationService.checkCompatibility(ResourceType.CONTA, contaSchemaBreaking);
        assertFalse(incompativel.isCompativel(),
                "Remoção de campo obrigatório (accountId/accountType/status/createdAt) deve falhar no INV-007");

        // 3. Verifica que todos os 6 tipos de recurso possuem schemas de domínio reais registrados
        for (ResourceType type : ResourceType.values()) {
            SchemaValidationService.Schema schema = validationService.getSchema(type, "1.0");
            assertNotNull(schema, "Schema 1.0 deve existir para " + type);
            assertFalse(schema.getRequiredFields().contains("canonicalId"),
                    "Schema deve validar campos do JSON canônico de data-models.yaml, não metadados da entidade");
        }
    }

    @Test
    void testSchemaValidationValidaCamposDoJsonCanonico() {
        SchemaValidationService validationService = new SchemaValidationService();

        CanonicalRecord invalidoSemCampos = CanonicalRecord.criar(
                "can-1", "raw-1", ResourceType.CONTA, "{\"foo\":\"bar\"}", "1.0", "a".repeat(64));
        ValidationResult resInvalido = validationService.validate(invalidoSemCampos, "1.0");
        assertFalse(resInvalido.isValid(),
                "Registro canônico sem os campos de CanonicalAccount deve ser reprovado na validação de schema");

        CanonicalRecord valido = CanonicalRecord.criar(
                "can-2", "raw-2", ResourceType.CONTA,
                "{\"accountId\":\"acc-1\",\"documentNumber\":\"123\",\"accountType\":\"CONTA_CORRENTE\","
                        + "\"agency\":\"0001\",\"number\":\"12345\",\"status\":\"ATIVA\",\"createdAt\":\"2026-01-01T00:00:00\"}",
                "1.0", "b".repeat(64));
        ValidationResult resValido = validationService.validate(valido, "1.0");
        assertTrue(resValido.isValid(),
                "Registro canônico completo deveria ser válido: " + resValido.getErrors());
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
