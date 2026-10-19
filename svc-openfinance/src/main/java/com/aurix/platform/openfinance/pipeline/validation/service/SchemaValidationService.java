package com.aurix.platform.openfinance.pipeline.validation.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de validação de schema do pipeline Open Finance.
 * Valida registros canônicos contra schemas registrados no Schema Registry
 * e verifica compatibilidade entre versões.
 */
@Service
public class SchemaValidationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SchemaValidationService.class);
    private static final String DEFAULT_SCHEMA_VERSION = "1.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Registros de schemas registrados (simulação do Schema Registry).
     * Em produção, seria conectado ao Apicurio ou Confluent Schema Registry.
     */
    private final Map<String, Map<String, Schema>> schemaRegistry = new HashMap<>();

    /**
     * Construtor que inicializa schemas padrão para cada tipo de recurso.
     */
    public SchemaValidationService() {
        inicializarSchemasPadrao();
    }

    /**
     * Valida um registro canônico contra o schema registrado.
     *
     * @param record        registro canônico a validar.
     * @param schemaVersion versão do schema alvo.
     * @return resultado da validação.
     */
    public ValidationResult validate(final CanonicalRecord record, final String schemaVersion) {
        log.info("Validando registro {} contra schema versão {}", record.getCanonicalId(), schemaVersion);

        Schema schema = getSchema(record.getResourceType(), schemaVersion);
        if (schema == null) {
            return ValidationResult.invalido(schemaVersion,
                    List.of("Schema não encontrado para tipo " + record.getResourceType()
                            + " versão " + schemaVersion));
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (record.getCanonicalData() == null || record.getCanonicalData().isBlank()) {
            errors.add("Dados canônicos não podem ser nulos ou vazios");
        } else {
            validarJsonCanonico(record.getCanonicalData(), schema, errors);
        }

        if (record.getCanonicalId() == null || record.getCanonicalId().isBlank()) {
            errors.add("ID canônico é obrigatório");
        }

        if (record.getChecksum() == null || record.getChecksum().isBlank()) {
            warnings.add("Checksum ausente — integridade não verificada");
        }

        if (record.getVersion() == null || record.getVersion().isBlank()) {
            errors.add("Versão do modelo canônico é obrigatória");
        }

        if (record.getRawRecordId() == null || record.getRawRecordId().isBlank()) {
            errors.add("Referência ao registro bruto é obrigatória");
        }

        if (!schema.getVersion().equals(schemaVersion)) {
            warnings.add("Versão do schema na entidade não corresponde à versão solicitada");
        }

        ValidationResult resultado = errors.isEmpty()
                ? ValidationResult.valido(schemaVersion)
                : ValidationResult.invalido(schemaVersion, errors);

        log.info("Validação concluída: válido={}, erros={}, avisos={}",
                resultado.isValid(), errors.size(), warnings.size());
        return resultado;
    }

    private void validarJsonCanonico(final String canonicalData, final Schema schema, final List<String> errors) {
        try {
            JsonNode root = objectMapper.readTree(canonicalData);
            if (!root.isObject()) {
                errors.add("Payload canônico deve ser um objeto JSON");
                return;
            }
            List<String> requiredFields = schema.getRequiredFields();
            for (String campo : requiredFields) {
                if (!root.has(campo)) {
                    errors.add("Campo canônico obrigatório ausente no schema: " + campo);
                }
            }
            if (!requiredFields.isEmpty()) {
                String campoIdentificadorPrimario = requiredFields.get(0);
                JsonNode idNode = root.get(campoIdentificadorPrimario);
                if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
                    errors.add("Identificador primário canônico '" + campoIdentificadorPrimario
                            + "' não pode ser nulo ou vazio");
                }
            }
        } catch (Exception e) {
            errors.add("Payload canônico não é um JSON válido: " + e.getMessage());
        }
    }

    /**
     * Valida um payload JSON contra um JSON Schema oficial (Draft-07) localizado em
     * {@code architecture/contracts/*.schema.json}.
     *
     * @param schemaJson  conteúdo do JSON Schema.
     * @param payloadJson payload JSON a validar.
     * @return resultado da validação com lista de erros estruturais.
     */
    public ValidationResult validateAgainstOfficialJsonSchema(final String schemaJson, final String payloadJson) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            validarNoContraJsonSchema("$", schemaNode, payloadNode, errors);
        } catch (Exception e) {
            errors.add("Erro ao processar JSON/Schema: " + e.getMessage());
        }
        return errors.isEmpty()
                ? ValidationResult.valido(DEFAULT_SCHEMA_VERSION)
                : ValidationResult.invalido(DEFAULT_SCHEMA_VERSION, errors);
    }

    private void validarNoContraJsonSchema(final String path, final JsonNode schema,
                                            final JsonNode instance, final List<String> errors) {
        if (schema == null || instance == null || instance.isNull()) {
            errors.add(path + ": valor nulo ou ausente");
            return;
        }
        if (schema.has("type")) {
            String expectedType = schema.get("type").asText();
            boolean typeOk = switch (expectedType) {
                case "object" -> instance.isObject();
                case "array" -> instance.isArray();
                case "string" -> instance.isTextual();
                case "integer" -> instance.isIntegralNumber();
                case "number" -> instance.isNumber();
                case "boolean" -> instance.isBoolean();
                default -> true;
            };
            if (!typeOk) {
                errors.add(path + ": tipo esperado '" + expectedType + "'");
                return;
            }
        }
        if (schema.has("enum") && schema.get("enum").isArray()) {
            boolean match = false;
            for (JsonNode val : schema.get("enum")) {
                if (val.asText().equals(instance.asText())) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                errors.add(path + ": valor '" + instance.asText() + "' fora do enum permitido");
            }
        }
        if (instance.isNumber() && schema.has("minimum")) {
            double min = schema.get("minimum").asDouble();
            if (instance.asDouble() < min) {
                errors.add(path + ": valor menor que o mínimo " + min);
            }
        }
        if (instance.isArray()) {
            if (schema.has("minItems") && instance.size() < schema.get("minItems").asInt()) {
                errors.add(path + ": array possui menos itens que minItems=" + schema.get("minItems").asInt());
            }
            if (schema.has("items")) {
                JsonNode itemSchema = schema.get("items");
                for (int i = 0; i < instance.size(); i++) {
                    validarNoContraJsonSchema(path + "[" + i + "]", itemSchema, instance.get(i), errors);
                }
            }
        }
        if (instance.isObject()) {
            if (schema.has("required") && schema.get("required").isArray()) {
                for (JsonNode req : schema.get("required")) {
                    String field = req.asText();
                    if (!instance.has(field) || instance.get(field).isNull()) {
                        errors.add(path + ": campo obrigatório ausente '" + field + "'");
                    }
                }
            }
            JsonNode props = schema.get("properties");
            if (schema.has("additionalProperties")
                    && schema.get("additionalProperties").isBoolean()
                    && !schema.get("additionalProperties").asBoolean()
                    && props != null && props.isObject()) {
                instance.fieldNames().forEachRemaining(fieldName -> {
                    if (!props.has(fieldName)) {
                        errors.add(path + ": propriedade adicional não permitida '" + fieldName + "'");
                    }
                });
            }
            if (props != null && props.isObject()) {
                props.fieldNames().forEachRemaining(propName -> {
                    if (instance.has(propName) && !instance.get(propName).isNull()) {
                        validarNoContraJsonSchema(path + "." + propName,
                                props.get(propName), instance.get(propName), errors);
                    }
                });
            }
        }
    }

    /**
     * Recupera um schema do registry.
     *
     * @param type    tipo de recurso.
     * @param version versão do schema.
     * @return schema encontrado ou null.
     */
    public Schema getSchema(final ResourceType type, final String version) {
        Map<String, Schema> versionMap = schemaRegistry.get(type.name());
        if (versionMap == null) {
            return null;
        }
        return versionMap.get(version);
    }

    /**
     * Verifica compatibilidade entre um novo schema e os existentes.
     *
     * @param type      tipo de recurso.
     * @param newSchema novo schema em formato JSON string.
     * @return resultado da verificação de compatibilidade.
     */
    public SchemaCompatibility checkCompatibility(final ResourceType type, final String newSchema) {
        log.info("Verificando compatibilidade de schema para tipo {}", type);

        Map<String, Schema> versionMap = schemaRegistry.get(type.name());
        if (versionMap == null || versionMap.isEmpty()) {
            return new SchemaCompatibility(true, "Nenhum schema existente — compatível por padrão");
        }

        Schema latestSchema = versionMap.values().stream()
                .max((s1, s2) -> compararVersoes(s1.getVersion(), s2.getVersion()))
                .orElse(null);

        if (latestSchema == null) {
            return new SchemaCompatibility(true, "Nenhum schema anterior encontrado");
        }

        boolean compativel = latestSchema.isRetrocompativelCom(newSchema);
        String mensagem = compativel
                ? "Novo schema é retrocompatível com versão " + latestSchema.getVersion()
                : "Novo schema NÃO é retrocompatível com versão " + latestSchema.getVersion();

        log.info("Resultado da compatibilidade: {}", mensagem);
        return new SchemaCompatibility(compativel, mensagem);
    }

    /**
     * Registra um novo schema no registry.
     *
     * @param type   tipo de recurso.
     * @param schema schema a registrar.
     */
    public void registrarSchema(final ResourceType type, final Schema schema) {
        schemaRegistry.computeIfAbsent(type.name(), k -> new HashMap<>())
                .put(schema.getVersion(), schema);
        log.info("Schema registrado: tipo={}, versão={}", type, schema.getVersion());
    }

    /**
     * Inicializa schemas padrão para cada tipo de recurso.
     */
    private void inicializarSchemasPadrao() {
        for (ResourceType type : ResourceType.values()) {
            Schema schema = new Schema(type.name(), DEFAULT_SCHEMA_VERSION,
                    "Schema padrão Open Finance Brasil v1.0 para " + type.getDescricao(),
                    camposObrigatoriosPorTipo(type));
            registrarSchema(type, schema);
        }
    }

    /**
     * Retorna os campos canônicos obrigatórios por tipo de recurso conforme architecture/data-models.yaml.
     *
     * @param type tipo de recurso.
     * @return lista de campos canônicos obrigatórios.
     */
    private List<String> camposObrigatoriosPorTipo(final ResourceType type) {
        switch (type) {
            case CONTA:
                return List.of("accountId", "documentNumber", "accountType", "agency",
                        "number", "status", "createdAt");
            case TRANSACAO:
                return List.of("transactionId", "accountId", "type", "amount", "timestamp");
            case CARTAO:
                return List.of("cardId", "documentNumber", "brand", "cardNumber",
                        "status", "creditLimit", "availableLimit");
            case PIX:
                return List.of("pixId", "endToEndId", "payerAccountId", "payeeAccountId",
                        "amount", "status", "timestamp");
            case CREDITO:
                return List.of("loanId", "documentNumber", "type", "principalAmount",
                        "interestRate", "termMonths", "status");
            case INVESTIMENTO:
                return List.of("investmentId", "documentNumber", "type", "currentValue",
                        "appliedAmount", "status");
            default:
                return List.of();
        }
    }

    /**
     * Compara duas versões no formato X.Y.
     *
     * @param v1 primeira versão.
     * @param v2 segunda versão.
     * @return resultado da comparação.
     */
    private int compararVersoes(final String v1, final String v2) {
        String[] partes1 = v1.split("\\.");
        String[] partes2 = v2.split("\\.");
        int len = Math.max(partes1.length, partes2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < partes1.length ? Integer.parseInt(partes1[i]) : 0;
            int p2 = i < partes2.length ? Integer.parseInt(partes2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    /**
     * Representa um schema registrado no Schema Registry.
     */
    public static class Schema {
        private final String resourceType;
        private final String version;
        private final String description;
        private final List<String> requiredFields;

        /**
         * Cria um novo schema.
         *
         * @param resourceType  tipo de recurso.
         * @param version       versão do schema.
         * @param description   descrição.
         * @param requiredFields campos obrigatórios.
         */
        public Schema(final String resourceType, final String version,
                final String description, final List<String> requiredFields) {
            this.resourceType = resourceType;
            this.version = version;
            this.description = description;
            this.requiredFields = requiredFields;
        }

        /**
         * Retorna o tipo de recurso.
         *
         * @return resourceType.
         */
        public String getResourceType() {
            return resourceType;
        }

        /**
         * Retorna a versão.
         *
         * @return version.
         */
        public String getVersion() {
            return version;
        }

        /**
         * Retorna a descrição.
         *
         * @return description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Retorna os campos obrigatórios.
         *
         * @return requiredFields.
         */
        public List<String> getRequiredFields() {
            return requiredFields;
        }

        /**
         * Verifica se este schema é retrocompatível com um novo schema.
         *
         * @param newSchemaDefinition definição do novo schema.
         * @return true se retrocompatível.
         */
        public boolean isRetrocompativelCom(final String newSchemaDefinition) {
            // Verificação simplificada: schema novo deve conter todos os campos obrigatórios
            if (newSchemaDefinition == null) {
                return false;
            }
            for (String campo : requiredFields) {
                if (!newSchemaDefinition.contains(campo)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Resultado da verificação de compatibilidade entre schemas.
     */
    public static class SchemaCompatibility {
        private final boolean compativel;
        private final String mensagem;

        /**
         * Cria um resultado de compatibilidade.
         *
         * @param compativel true se compatível.
         * @param mensagem   mensagem descritiva.
         */
        public SchemaCompatibility(final boolean compativel, final String mensagem) {
            this.compativel = compativel;
            this.mensagem = mensagem;
        }

        /**
         * Retorna se é compatível.
         *
         * @return compativel.
         */
        public boolean isCompativel() {
            return compativel;
        }

        /**
         * Retorna a mensagem descritiva.
         *
         * @return mensagem.
         */
        public String getMensagem() {
            return mensagem;
        }
    }
}
