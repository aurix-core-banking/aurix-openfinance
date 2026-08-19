package com.aurix.platform.openfinance.pipeline.lineage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidade que registra a linhagem completa de um dado no pipeline Open Finance.
 * Cadeia de rastreabilidade: consentId → resourceId → executionPlanId → workflowId
 * → pipelineExecutionId → sourceRecordId → canonicalRecordId → publicationId.
 *
 * INV03: Sem linhagem, sem publicação.
 */
@Entity
@Table(name = "lineage_records", schema = "aurix_openfinance")
public class LineageRecord {

    /**
     * Comprimento máximo dos IDs de linhagem.
     */
    private static final int LINEAGE_ID_LENGTH = 64;

    /**
     * Identificador único da entidade no banco.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID único do registro de linhagem (UUID).
     */
    @Column(name = "lineage_id", nullable = false, unique = true, length = LINEAGE_ID_LENGTH)
    private String lineageId;

    /**
     * ID do consentimento Open Finance que autorizou a compartilhamento.
     */
    @Column(name = "consent_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String consentId;

    /**
     * ID do recurso compartilhado (conta, cartão, etc).
     */
    @Column(name = "resource_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String resourceId;

    /**
     * ID do plano de execução que orquestrou o processamento.
     */
    @Column(name = "execution_plan_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String executionPlanId;

    /**
     * ID do workflow associado ao plano de execução.
     */
    @Column(name = "workflow_id", length = LINEAGE_ID_LENGTH)
    private String workflowId;

    /**
     * ID da execução específica do pipeline.
     */
    @Column(name = "pipeline_execution_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String pipelineExecutionId;

    /**
     * ID do registro fonte bruto.
     */
    @Column(name = "source_record_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String sourceRecordId;

    /**
     * ID do registro canônico resultante.
     */
    @Column(name = "canonical_record_id", nullable = false, length = LINEAGE_ID_LENGTH)
    private String canonicalRecordId;

    /**
     * ID da publicação resultante (se publicado com sucesso).
     */
    @Column(name = "publication_id", length = LINEAGE_ID_LENGTH)
    private String publicationId;

    /**
     * Data e hora de criação do registro de linhagem.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora da última atualização.
     */
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    /**
     * Construtor padrão para JPA.
     */
    public LineageRecord() {
    }

    /**
     * Cria um novo registro de linhagem.
     *
     * @param lineageId           ID da linhagem.
     * @param consentId           ID do consentimento.
     * @param resourceId          ID do recurso.
     * @param executionPlanId     ID do plano de execução.
     * @param workflowId          ID do workflow.
     * @param pipelineExecutionId ID da execução do pipeline.
     * @param sourceRecordId      ID do registro fonte.
     * @param canonicalRecordId   ID do registro canônico.
     * @return novo LineageRecord.
     */
    public static LineageRecord criar(final String lineageId, final String consentId,
            final String resourceId, final String executionPlanId,
            final String workflowId, final String pipelineExecutionId,
            final String sourceRecordId, final String canonicalRecordId) {
        LineageRecord record = new LineageRecord();
        record.setLineageId(lineageId);
        record.setConsentId(consentId);
        record.setResourceId(resourceId);
        record.setExecutionPlanId(executionPlanId);
        record.setWorkflowId(workflowId);
        record.setPipelineExecutionId(pipelineExecutionId);
        record.setSourceRecordId(sourceRecordId);
        record.setCanonicalRecordId(canonicalRecordId);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    /**
     * Define o ID da publicação (atualizado após publicação bem-sucedida).
     *
     * @param publicationId ID da publicação.
     */
    public void setPublicationId(final String publicationId) {
        this.publicationId = publicationId;
    }

    /**
     * Retorna o ID da linhagem.
     *
     * @return lineageId.
     */
    public String getLineageId() {
        return lineageId;
    }

    /**
     * Define o ID da linhagem.
     *
     * @param lineageId o ID.
     */
    public void setLineageId(final String lineageId) {
        this.lineageId = lineageId;
    }

    /**
     * Retorna o ID do consentimento.
     *
     * @return consentId.
     */
    public String getConsentId() {
        return consentId;
    }

    /**
     * Define o ID do consentimento.
     *
     * @param consentId o ID.
     */
    public void setConsentId(final String consentId) {
        this.consentId = consentId;
    }

    /**
     * Retorna o ID do recurso.
     *
     * @return resourceId.
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Define o ID do recurso.
     *
     * @param resourceId o ID.
     */
    public void setResourceId(final String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Retorna o ID do plano de execução.
     *
     * @return executionPlanId.
     */
    public String getExecutionPlanId() {
        return executionPlanId;
    }

    /**
     * Define o ID do plano de execução.
     *
     * @param executionPlanId o ID.
     */
    public void setExecutionPlanId(final String executionPlanId) {
        this.executionPlanId = executionPlanId;
    }

    /**
     * Retorna o ID do workflow.
     *
     * @return workflowId.
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Define o ID do workflow.
     *
     * @param workflowId o ID.
     */
    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Retorna o ID da execução do pipeline.
     *
     * @return pipelineExecutionId.
     */
    public String getPipelineExecutionId() {
        return pipelineExecutionId;
    }

    /**
     * Define o ID da execução do pipeline.
     *
     * @param pipelineExecutionId o ID.
     */
    public void setPipelineExecutionId(final String pipelineExecutionId) {
        this.pipelineExecutionId = pipelineExecutionId;
    }

    /**
     * Retorna o ID do registro fonte.
     *
     * @return sourceRecordId.
     */
    public String getSourceRecordId() {
        return sourceRecordId;
    }

    /**
     * Define o ID do registro fonte.
     *
     * @param sourceRecordId o ID.
     */
    public void setSourceRecordId(final String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    /**
     * Retorna o ID do registro canônico.
     *
     * @return canonicalRecordId.
     */
    public String getCanonicalRecordId() {
        return canonicalRecordId;
    }

    /**
     * Define o ID do registro canônico.
     *
     * @param canonicalRecordId o ID.
     */
    public void setCanonicalRecordId(final String canonicalRecordId) {
        this.canonicalRecordId = canonicalRecordId;
    }

    /**
     * Retorna o ID da publicação.
     *
     * @return publicationId.
     */
    public String getPublicationId() {
        return publicationId;
    }

    /**
     * Retorna a data de criação.
     *
     * @return createdAt.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Define a data de criação.
     *
     * @param createdAt a data.
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
     * @param dataAtualizacao a data.
     */
    public void setDataAtualizacao(final LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
