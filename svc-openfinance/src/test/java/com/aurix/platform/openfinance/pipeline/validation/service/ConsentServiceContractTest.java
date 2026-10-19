package com.aurix.platform.openfinance.pipeline.validation.service;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import com.aurix.platform.openfinance.event.EventEnvelope;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de contrato Consumer/Provider (Pact-style) e validação contra os JSON Schemas
 * oficiais em {@code architecture/contracts/consent.schema.json} e
 * {@code architecture/contracts/execution-plan.schema.json}, cobrindo os relacionamentos
 * declarados em {@code architecture/services.yaml} (Issue #9).
 */
class ConsentServiceContractTest {

    private static String consentSchemaJson;
    private static String executionPlanSchemaJson;
    private final SchemaValidationService schemaValidationService = new SchemaValidationService();

    @BeforeAll
    static void loadContractSchemas() throws IOException {
        Path root = Files.exists(Path.of("architecture/contracts/consent.schema.json"))
                ? Path.of("architecture/contracts")
                : Path.of("../architecture/contracts");
        consentSchemaJson = Files.readString(root.resolve("consent.schema.json"));
        executionPlanSchemaJson = Files.readString(root.resolve("execution-plan.schema.json"));
    }

    @Test
    @DisplayName("Contrato JSON Schema: consent.schema.json deve aprovar consentimento válido e rejeitar inválido")
    void deveValidarContratoOficialConsentSchemaJson() {
        String validConsentJson = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "subject": {
                    "type": "PF",
                    "identifier": "12345678909",
                    "name": "Maria Silva"
                  },
                  "permissions": [
                    {
                      "resource": "accounts",
                      "actions": ["READ"]
                    }
                  ],
                  "resources": [
                    {
                      "type": "ACCOUNT",
                      "path": "/accounts/v1"
                    }
                  ],
                  "purpose": {
                    "code": "CREDIT_ANALYSIS",
                    "description": "Compartilhamento para análise de crédito"
                  },
                  "status": "AUTHORIZED",
                  "version": 1,
                  "createdAt": "2026-10-18T10:00:00Z",
                  "expiresAt": "2027-10-18T10:00:00Z"
                }
                """;

        ValidationResult validResult = schemaValidationService
                .validateAgainstOfficialJsonSchema(consentSchemaJson, validConsentJson);
        assertTrue(validResult.isValid(), "Consentimento válido deveria passar no consent.schema.json: "
                + validResult.getErrors());

        String invalidConsentJson = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "subject": {
                    "type": "INVALID_TYPE"
                  },
                  "permissions": [],
                  "status": "UNKNOWN_STATUS"
                }
                """;

        ValidationResult invalidResult = schemaValidationService
                .validateAgainstOfficialJsonSchema(consentSchemaJson, invalidConsentJson);
        assertFalse(invalidResult.isValid(), "Consentimento inválido deve ser rejeitado pelo consent.schema.json");
        assertTrue(invalidResult.getErrors().size() >= 3);
    }

    @Test
    @DisplayName("Contrato JSON Schema: execution-plan.schema.json deve aprovar DAG válido e rejeitar propriedade adicional")
    void deveValidarContratoOficialExecutionPlanSchemaJson() {
        String validPlanJson = """
                {
                  "planId": "550e8400-e29b-41d4-a716-446655440001",
                  "consentId": "550e8400-e29b-41d4-a716-446655440000",
                  "consentVersion": 1,
                  "createdAt": "2026-10-18T10:00:00Z",
                  "validUntil": "2026-10-18T11:00:00Z",
                  "nodes": [
                    {
                      "nodeId": "node-contas",
                      "capability": "extract_contas",
                      "resource": {
                        "type": "ACCOUNT",
                        "path": "/accounts"
                      },
                      "extractor": "CoreAccountExtractor",
                      "authorization": {
                        "permissions": ["ACCOUNTS_READ"],
                        "purpose": "CREDIT_ANALYSIS",
                        "validUntil": "2026-10-18T11:00:00Z"
                      },
                      "dependencies": [],
                      "retryPolicy": {
                        "maxAttempts": 3,
                        "backoffCoefficient": 2.0,
                        "initialInterval": "PT1S",
                        "maximumInterval": "PT30S"
                      },
                      "rateLimit": {
                        "requestsPerSecond": 10
                      },
                      "timeout": "PT30S",
                      "idempotencyKey": "idemp-contas-1",
                      "schemaVersion": "1.0.0"
                    }
                  ],
                  "edges": []
                }
                """;

        ValidationResult validResult = schemaValidationService
                .validateAgainstOfficialJsonSchema(executionPlanSchemaJson, validPlanJson);
        assertTrue(validResult.isValid(), "ExecutionPlan válido deveria passar no execution-plan.schema.json: "
                + validResult.getErrors());

        String invalidPlanWithExtraProp = """
                {
                  "planId": "550e8400-e29b-41d4-a716-446655440001",
                  "consentId": "550e8400-e29b-41d4-a716-446655440000",
                  "consentVersion": 0,
                  "createdAt": "2026-10-18T10:00:00Z",
                  "validUntil": "2026-10-18T11:00:00Z",
                  "nodes": [],
                  "unexpectedField": "not-allowed"
                }
                """;

        ValidationResult invalidResult = schemaValidationService
                .validateAgainstOfficialJsonSchema(executionPlanSchemaJson, invalidPlanWithExtraProp);
        assertFalse(invalidResult.isValid());
        assertTrue(invalidResult.getErrors().stream().anyMatch(e -> e.contains("unexpectedField")),
                "Deve acusar propriedade adicional não permitida");
    }

    @Test
    @DisplayName("Contratos Consumer/Provider: relacionamentos entre ConsentPlane, DataPlane e DistributionPlane")
    void deveVerificarContratosDeRelacionamentoDeServicesYaml() {
        // 1. consent-service -> resource-discovery (event: consent.granted.v1)
        Map<String, Object> grantedEnvelope = EventEnvelope.criar(
                "consent.granted.v1", "consent-c1", null, "corr-1", null,
                Map.of("clientId", "client-1", "permissions", List.of("accounts")));
        assertEquals("consent.granted.v1", grantedEnvelope.get("eventType"));
        assertEquals("consent-c1", grantedEnvelope.get("consentId"));

        // 2. consent-service -> policy-engine (contract: ConsentRecord / PolicyDecision)
        // 3. resource-discovery -> policy-engine (contract: ResourceGraph)
        ResourceGraph graph = new ResourceGraph("graph-c1", "consent-c1", 1);
        ResourceNode node = new ResourceNode("graph-c1:accounts", "graph-c1", "ACCOUNT",
                "/accounts", "[\"READ\"]", "[]", "{}");
        graph.setNodes(List.of(node));
        assertEquals("consent-c1", graph.getConsentId());
        assertEquals(1, graph.getNodes().size());

        PolicyDecisionResponse policyDecision = new PolicyDecisionResponse(
                PolicyDecisionType.ALLOWED, "Regras satisfeitas", List.of("CONSENT-001"),
                4, "consent-c1", "graph-c1:accounts", "accounts");
        assertEquals(PolicyDecisionType.ALLOWED, policyDecision.getDecision());

        // 4. policy-engine -> extraction-planner (contract: AuthorizedContext)
        AuthorizedContext authCtx = new AuthorizedContext(
                "ctx-c1", "user-1", "consent-c1", 1, "accounts",
                "accounts", "{\"resources\":[\"contas\"]}",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-1", "sig-1");
        assertTrue(authCtx.isActive());

        // 7. data-pipeline -> extractors (contract: DataExtractor: ResourceDescriptor -> RawData)
        ResourceDescriptor descriptor = new ResourceDescriptor("contas", ResourceType.values()[0], "/accounts");
        RawData rawData = new RawData(ResourceType.values()[0], "aurix-core", Map.of("accountId", "acc-1"), 1, LocalDateTime.now());
        assertEquals(descriptor.getResourceType(), rawData.getResourceType());
        assertEquals(1, rawData.getRecordCount());

        // 9. data-pipeline -> data-products (contract: PublishedData / data.published.v1)
        Map<String, Object> publishedEnvelope = EventEnvelope.criar(
                "data.published.v1", "consent-c1", "plan-c1", "corr-1", null,
                Map.of("publicationId", "pub-1", "resource", "contas", "canonicalData", "{\"accountId\":\"acc-1\"}"));
        assertEquals("data.published.v1", publishedEnvelope.get("eventType"));
        assertEquals("contas", publishedEnvelope.get("resource"));
    }
}
