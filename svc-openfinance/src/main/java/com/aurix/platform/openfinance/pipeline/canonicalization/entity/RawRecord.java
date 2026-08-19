package com.aurix.platform.openfinance.pipeline.canonicalization.entity;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidade que representa um registro bruto extraído de um sistema fonte.
 * Armazena os dados crus antes da canonicalização no pipeline Open Finance.
 */
@Entity
@Table(name = "raw_records", schema = "aurix_openfinance")
public class RawRecord {

    /**
     * Comprimento máximo do nome do sistema fonte.
     */
    private static final int SOURCE_SYSTEM_LENGTH = 100;

    /**
     * Comprimento máximo da versão do schema.
     */
    private static final int SCHEMA_VERSION_LENGTH = 20;

    /**
     * Comprimento máximo do ID de extração.
     */
    private static final int EXTRACTION_ID_LENGTH = 64;

    /**
     * Comprimento máximo do ID do registro.
     */
    private static final int RECORD_ID_LENGTH = 128;

    /**
     * Identificador único do registro bruto.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID externo do registro bruto.
     */
    @Column(name = "record_id", nullable = false, unique = true, length = RECORD_ID_LENGTH)
    private String recordId;

    /**
     * Sistema de origem dos dados (ex: CORE_BANKING, EXTERNO_BACEN).
     */
    @Column(name = "source_system", nullable = false, length = SOURCE_SYSTEM_LENGTH)
    private String sourceSystem;

    /**
     * ID da execução de extração que gerou este registro.
     */
    @Column(name = "extraction_id", nullable = false, length = EXTRACTION_ID_LENGTH)
    private String extractionId;

    /**
     * Tipo de recurso Open Finance (CONTA, TRANSACAO, CARTAO, PIX).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    /**
     * Dados brutos em formato JSON.
     */
    @Lob
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private String rawData;

    /**
     * Data e hora da extração dos dados.
     */
    @Column(name = "extracted_at", nullable = false)
    private LocalDateTime extractedAt;

    /**
     * Versão do schema da origem.
     */
    @Column(name = "schema_version", nullable = false, length = SCHEMA_VERSION_LENGTH)
    private String schemaVersion;

    /**
     * Data e hora de criação do registro.
     */
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /**
     * Data e hora da última atualização.
     */
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    /**
     * Construtor padrão para JPA.
     */
    public RawRecord() {
    }

    /**
     * Retorna o ID externo do registro.
     *
     * @return recordId.
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * Define o ID externo do registro.
     *
     * @param recordId o ID externo.
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * Retorna o sistema de origem.
     *
     * @return sourceSystem.
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * Define o sistema de origem.
     *
     * @param sourceSystem o sistema de origem.
     */
    public void setSourceSystem(final String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Retorna o ID de extração.
     *
     * @return extractionId.
     */
    public String getExtractionId() {
        return extractionId;
    }

    /**
     * Define o ID de extração.
     *
     * @param extractionId o ID de extração.
     */
    public void setExtractionId(final String extractionId) {
        this.extractionId = extractionId;
    }

    /**
     * Retorna o tipo de recurso.
     *
     * @return resourceType.
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Define o tipo de recurso.
     *
     * @param resourceType o tipo de recurso.
     */
    public void setResourceType(final ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Retorna os dados brutos JSON.
     *
     * @return rawData.
     */
    public String getRawData() {
        return rawData;
    }

    /**
     * Define os dados brutos JSON.
     *
     * @param rawData os dados brutos.
     */
    public void setRawData(final String rawData) {
        this.rawData = rawData;
    }

    /**
     * Retorna a data de extração.
     *
     * @return extractedAt.
     */
    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }

    /**
     * Define a data de extração.
     *
     * @param extractedAt a data de extração.
     */
    public void setExtractedAt(final LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }

    /**
     * Retorna a versão do schema da origem.
     *
     * @return schemaVersion.
     */
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Define a versão do schema da origem.
     *
     * @param schemaVersion a versão do schema.
     */
    public void setSchemaVersion(final String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * Retorna a data de criação.
     *
     * @return dataCriacao.
     */
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    /**
     * Define a data de criação.
     *
     * @param dataCriacao a data de criação.
     */
    public void setDataCriacao(final LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    /**
     * Retorna a data de atualização.
     *
     * @return dataAtualizacao.
     */
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    /**
     * Define a data de atualização.
     *
     * @param dataAtualizacao a data de atualização.
     */
    public void setDataAtualizacao(final LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    /**
     * Cria um novo RawRecord para persistência.
     *
     * @param recordId      ID externo.
     * @param sourceSystem  sistema de origem.
     * @param extractionId  ID de extração.
     * @param resourceType  tipo de recurso.
     * @param rawData       dados brutos JSON.
     * @param extractedAt   data de extração.
     * @param schemaVersion versão do schema.
     * @return novo RawRecord.
     */
    public static RawRecord criar(final String recordId, final String sourceSystem,
            final String extractionId, final ResourceType resourceType,
            final String rawData, final LocalDateTime extractedAt,
            final String schemaVersion) {
        RawRecord record = new RawRecord();
        record.setRecordId(recordId);
        record.setSourceSystem(sourceSystem);
        record.setExtractionId(extractionId);
        record.setResourceType(resourceType);
        record.setRawData(rawData);
        record.setExtractedAt(extractedAt);
        record.setSchemaVersion(schemaVersion);
        record.setDataCriacao(LocalDateTime.now());
        return record;
    }
}
