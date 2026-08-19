package com.aurix.platform.openfinance.pipeline.validation.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
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
     * Retorna os campos obrigatórios por tipo de recurso.
     *
     * @param type tipo de recurso.
     * @return lista de campos obrigatórios.
     */
    private List<String> camposObrigatoriosPorTipo(final ResourceType type) {
        switch (type) {
            case CONTA:
                return List.of("canonicalId", "rawRecordId", "resourceType", "canonicalData",
                        "version", "checksum");
            case TRANSACAO:
                return List.of("canonicalId", "rawRecordId", "resourceType", "canonicalData",
                        "version", "checksum");
            case CARTAO:
                return List.of("canonicalId", "rawRecordId", "resourceType", "canonicalData",
                        "version", "checksum");
            case PIX:
                return List.of("canonicalId", "rawRecordId", "resourceType", "canonicalData",
                        "version", "checksum");
            default:
                return List.of("canonicalId", "rawRecordId", "resourceType", "canonicalData");
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
