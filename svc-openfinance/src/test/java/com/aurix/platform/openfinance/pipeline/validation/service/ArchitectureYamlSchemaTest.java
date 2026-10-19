package com.aurix.platform.openfinance.pipeline.validation.service;

import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de governança arquitetural (Issue #12) que valida os arquivos YAML
 * autoritativos em {@code architecture/*.yaml} contra seus respectivos JSON Schemas
 * em {@code architecture/schemas/*.schema.json}.
 */
class ArchitectureYamlSchemaTest {

    private static Path archDir;
    private static Path schemasDir;
    private final SchemaValidationService schemaValidationService = new SchemaValidationService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Yaml yaml = new Yaml();

    @BeforeAll
    static void locateArchitectureDirs() {
        archDir = Files.exists(Path.of("architecture/architecture.yaml"))
                ? Path.of("architecture")
                : Path.of("../architecture");
        schemasDir = archDir.resolve("schemas");
    }

    @Test
    @DisplayName("architecture/architecture.yaml deve ser válido contra architecture.schema.json")
    void deveValidarArchitectureYamlContraSchema() throws IOException {
        ValidationResult res = validarYamlContraSchema("architecture.yaml", "architecture.schema.json");
        assertTrue(res.isValid(), "architecture.yaml inválido: " + res.getErrors());
    }

    @Test
    @DisplayName("architecture/services.yaml deve ser válido contra services.schema.json")
    void deveValidarServicesYamlContraSchema() throws IOException {
        ValidationResult res = validarYamlContraSchema("services.yaml", "services.schema.json");
        assertTrue(res.isValid(), "services.yaml inválido: " + res.getErrors());
    }

    @Test
    @DisplayName("architecture/flows.yaml deve ser válido contra flows.schema.json")
    void deveValidarFlowsYamlContraSchema() throws IOException {
        ValidationResult res = validarYamlContraSchema("flows.yaml", "flows.schema.json");
        assertTrue(res.isValid(), "flows.yaml inválido: " + res.getErrors());
    }

    @Test
    @DisplayName("architecture/policies.yaml deve ser válido contra policies.schema.json")
    void deveValidarPoliciesYamlContraSchema() throws IOException {
        ValidationResult res = validarYamlContraSchema("policies.yaml", "policies.schema.json");
        assertTrue(res.isValid(), "policies.yaml inválido: " + res.getErrors());
    }

    @Test
    @DisplayName("architecture/events.yaml deve ser válido contra events.schema.json")
    void deveValidarEventsYamlContraSchema() throws IOException {
        ValidationResult res = validarYamlContraSchema("events.yaml", "events.schema.json");
        assertTrue(res.isValid(), "events.yaml inválido: " + res.getErrors());
    }

    @Test
    @DisplayName("Deve rejeitar especificação YAML com kind ou invariant ID fora do padrão")
    void deveRejeitarYamlDeArquiteturaInvalido() throws IOException {
        String schemaJson = Files.readString(schemasDir.resolve("architecture.schema.json"));
        String invalidYamlAsJson = """
                {
                  "specVersion": "invalid-version",
                  "kind": "WrongKind",
                  "metadata": {
                    "name": "broken"
                  },
                  "invariants": [
                    {
                      "id": "BAD_ID",
                      "rule": "NONE",
                      "severity": "unknown"
                    }
                  ]
                }
                """;
        ValidationResult res = schemaValidationService.validateAgainstOfficialJsonSchema(schemaJson, invalidYamlAsJson);
        assertFalse(res.isValid());
        assertTrue(res.getErrors().size() >= 4);
    }

    private ValidationResult validarYamlContraSchema(String yamlFileName, String schemaFileName) throws IOException {
        String yamlContent = Files.readString(archDir.resolve(yamlFileName));
        Map<String, Object> parsedYaml = yaml.load(yamlContent);
        String jsonInstance = objectMapper.writeValueAsString(parsedYaml);
        String schemaJson = Files.readString(schemasDir.resolve(schemaFileName));
        return schemaValidationService.validateAgainstOfficialJsonSchema(schemaJson, jsonInstance);
    }
}
