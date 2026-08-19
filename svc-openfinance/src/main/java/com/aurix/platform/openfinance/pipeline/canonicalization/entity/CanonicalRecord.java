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
 * Entidade que representa um registro canônico no formato Open Finance.
 * Após a canonicalização, os dados seguem o modelo padrão do espectro Open Finance Brasil.
 */
@Entity
@Table(name = "canonical_records", schema = "aurix_openfinance")
public class CanonicalRecord {

    /**
     * Comprimento máximo do UUID canônico.
     */
    private static final int CANONICAL_ID_LENGTH = 64;

    /**
     * Comprimento máximo da versão.
     */
    private static final int VERSION_LENGTH = 20;

    /**
     * Comprimento máximo do checksum.
     */
    private static final int CHECKSUM_LENGTH = 128;

    /**
     * Comprimento máximo do rawRecordId.
     */
    private static final int RAW_RECORD_ID_LENGTH = 128;

    /**
     * Identificador único da entidade no banco.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID canônico único (UUID v4).
     */
    @Column(name = "canonical_id", nullable = false, unique = true, length = CANONICAL_ID_LENGTH)
    private String canonicalId;

    /**
     * Referência ao registro bruto de origem.
     */
    @Column(name = "raw_record_id", nullable = false, length = RAW_RECORD_ID_LENGTH)
    private String rawRecordId;

    /**
     * Tipo de recurso Open Finance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    /**
     * Dados canônicos em formato JSON.
     */
    @Lob
    @Column(name = "canonical_data", nullable = false, columnDefinition = "jsonb")
    private String canonicalData;

    /**
     * Versão do modelo canônico.
     */
    @Column(name = "version", nullable = false, length = VERSION_LENGTH)
    private String version;

    /**
     * Data e hora da canonicalização.
     */
    @Column(name = "canonicalized_at", nullable = false)
    private LocalDateTime canonicalizedAt;

    /**
     * Checksum SHA-256 dos dados canônicos para integridade.
     */
    @Column(name = "checksum", nullable = false, length = CHECKSUM_LENGTH)
    private String checksum;

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
    public CanonicalRecord() {
    }

    /**
     * Retorna o ID canônico.
     *
     * @return canonicalId.
     */
    public String getCanonicalId() {
        return canonicalId;
    }

    /**
     * Define o ID canônico.
     *
     * @param canonicalId o ID canônico.
     */
    public void setCanonicalId(final String canonicalId) {
        this.canonicalId = canonicalId;
    }

    /**
     * Retorna o ID do registro bruto.
     *
     * @return rawRecordId.
     */
    public String getRawRecordId() {
        return rawRecordId;
    }

    /**
     * Define o ID do registro bruto.
     *
     * @param rawRecordId o ID do registro bruto.
     */
    public void setRawRecordId(final String rawRecordId) {
        this.rawRecordId = rawRecordId;
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
     * Retorna os dados canônicos JSON.
     *
     * @return canonicalData.
     */
    public String getCanonicalData() {
        return canonicalData;
    }

    /**
     * Define os dados canônicos JSON.
     *
     * @param canonicalData os dados canônicos.
     */
    public void setCanonicalData(final String canonicalData) {
        this.canonicalData = canonicalData;
    }

    /**
     * Retorna a versão do modelo canônico.
     *
     * @return version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Define a versão do modelo canônico.
     *
     * @param version a versão.
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Retorna a data de canonicalização.
     *
     * @return canonicalizedAt.
     */
    public LocalDateTime getCanonicalizedAt() {
        return canonicalizedAt;
    }

    /**
     * Define a data de canonicalização.
     *
     * @param canonicalizedAt a data de canonicalização.
     */
    public void setCanonicalizedAt(final LocalDateTime canonicalizedAt) {
        this.canonicalizedAt = canonicalizedAt;
    }

    /**
     * Retorna o checksum SHA-256.
     *
     * @return checksum.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Define o checksum SHA-256.
     *
     * @param checksum o checksum.
     */
    public void setChecksum(final String checksum) {
        this.checksum = checksum;
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
     * Cria um novo CanonicalRecord.
     *
     * @param canonicalId   ID canônico.
     * @param rawRecordId   ID do registro bruto.
     * @param resourceType  tipo de recurso.
     * @param canonicalData dados canônicos.
     * @param version       versão do modelo.
     * @param checksum      checksum SHA-256.
     * @return novo CanonicalRecord.
     */
    public static CanonicalRecord criar(final String canonicalId, final String rawRecordId,
            final ResourceType resourceType, final String canonicalData,
            final String version, final String checksum) {
        CanonicalRecord record = new CanonicalRecord();
        record.setCanonicalId(canonicalId);
        record.setRawRecordId(rawRecordId);
        record.setResourceType(resourceType);
        record.setCanonicalData(canonicalData);
        record.setVersion(version);
        record.setCanonicalizedAt(LocalDateTime.now());
        record.setChecksum(checksum);
        record.setDataCriacao(LocalDateTime.now());
        return record;
    }
}
