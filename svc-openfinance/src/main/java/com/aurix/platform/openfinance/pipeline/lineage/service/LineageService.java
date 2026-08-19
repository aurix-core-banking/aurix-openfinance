package com.aurix.platform.openfinance.pipeline.lineage.service;

import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageChain;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de linhagem do pipeline Open Finance.
 * Registra e consulta a cadeia de rastreabilidade completa:
 * consentId → resourceId → executionPlanId → workflowId →
 * pipelineExecutionId → sourceRecordId → canonicalRecordId → publicationId.
 *
 * INV03: Sem linhagem, sem publicação.
 */
@Service
public class LineageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LineageService.class);

    /**
     * Armazenamento em memória para simulação.
     * Em produção, seria um repositório JPA.
     */
    private final Map<String, LineageRecord> lineageStore = new ConcurrentHashMap<>();

    /**
     * Armazenamento por publicationId para consultas reversas.
     */
    private final Map<String, List<String>> publicationIndex = new ConcurrentHashMap<>();

    /**
     * Registra a linhagem completa de um dado no pipeline.
     *
     * @param request dados da requisição de linhagem.
     * @return registro de linhagem criado.
     */
    public LineageRecord registerLineage(final LineageRequest request) {
        log.info("Registrando linhagem: consentId={}, resourceId={}",
                request.getConsentId(), request.getResourceId());

        String lineageId = UUID.randomUUID().toString();

        LineageRecord record = LineageRecord.criar(
                lineageId,
                request.getConsentId(),
                request.getResourceId(),
                request.getExecutionPlanId(),
                request.getWorkflowId(),
                request.getPipelineExecutionId(),
                request.getSourceRecordId(),
                request.getCanonicalRecordId());

        lineageStore.put(lineageId, record);

        publicationIndex.computeIfAbsent(
                request.getPublicationId() != null ? request.getPublicationId() : "pending",
                k -> new ArrayList<>()).add(lineageId);

        log.info("Linhagem registrada: lineageId={}, publicationId={}",
                lineageId, request.getPublicationId());
        return record;
    }

    /**
     * Atualiza o publicationId de um registro de linhagem.
     * Chamado após publicação bem-sucedida.
     *
     * @param lineageId    ID da linhagem.
     * @param publicationId ID da publicação.
     */
    public void atualizarPublicacao(final String lineageId, final String publicationId) {
        LineageRecord record = lineageStore.get(lineageId);
        if (record != null) {
            record.setPublicationId(publicationId);
            record.setDataAtualizacao(LocalDateTime.now());
            log.info("Publicação atualizada na linhagem: lineageId={}, publicationId={}",
                    lineageId, publicationId);
        }
    }

    /**
     * Traz a linha completa de linhagem a partir de uma publicação.
     *
     * @param publicationId ID da publicação.
     * @return cadeia de linhagem.
     */
    public LineageChain traceLineage(final String publicationId) {
        log.info("Trazendo linhagem para publicação: {}", publicationId);

        List<String> lineageIds = publicationIndex.get(publicationId);
        if (lineageIds == null || lineageIds.isEmpty()) {
            log.warn("Nenhuma linhagem encontrada para publicação: {}", publicationId);
            return LineageChain.vazia(publicationId);
        }

        List<LineageRecord> records = new ArrayList<>();
        for (String lineageId : lineageIds) {
            LineageRecord record = lineageStore.get(lineageId);
            if (record != null) {
                records.add(record);
            }
        }

        if (records.isEmpty()) {
            return LineageChain.vazia(publicationId);
        }

        boolean completa = verificarCadeiaCompleta(records);
        return completa
                ? LineageChain.completa(publicationId, records)
                : LineageChain.incompleta(publicationId, records);
    }

    /**
     * Traz a linhagem por consentId.
     *
     * @param consentId ID do consentimento.
     * @return cadeia de linhagem.
     */
    public LineageChain traceLineageByConsent(final String consentId) {
        log.info("Trazendo linhagem por consentimento: {}", consentId);

        List<LineageRecord> records = lineageStore.values().stream()
                .filter(r -> consentId.equals(r.getConsentId()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        if (records.isEmpty()) {
            return LineageChain.vazia(consentId);
        }

        String pubId = records.stream()
                .map(LineageRecord::getPublicationId)
                .filter(p -> p != null && !"pending".equals(p))
                .findFirst()
                .orElse("pending");

        boolean completa = verificarCadeiaCompleta(records);
        return completa
                ? LineageChain.completa(pubId, records)
                : LineageChain.incompleta(pubId, records);
    }

    /**
     * Verifica se a cadeia de linhagem está completa.
     *
     * @param records registros da cadeia.
     * @return true se completa.
     */
    private boolean verificarCadeiaCompleta(final List<LineageRecord> records) {
        if (records.isEmpty()) {
            return false;
        }
        for (LineageRecord record : records) {
            if (record.getConsentId() == null || record.getConsentId().isBlank()) {
                return false;
            }
            if (record.getResourceId() == null || record.getResourceId().isBlank()) {
                return false;
            }
            if (record.getExecutionPlanId() == null || record.getExecutionPlanId().isBlank()) {
                return false;
            }
            if (record.getPipelineExecutionId() == null || record.getPipelineExecutionId().isBlank()) {
                return false;
            }
            if (record.getSourceRecordId() == null || record.getSourceRecordId().isBlank()) {
                return false;
            }
            if (record.getCanonicalRecordId() == null || record.getCanonicalRecordId().isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requisição de criação de linhagem.
     */
    public static class LineageRequest {
        private final String consentId;
        private final String resourceId;
        private final String executionPlanId;
        private final String workflowId;
        private final String pipelineExecutionId;
        private final String sourceRecordId;
        private final String canonicalRecordId;
        private final String publicationId;

        /**
         * Cria uma requisição de linhagem.
         *
         * @param consentId           ID do consentimento.
         * @param resourceId          ID do recurso.
         * @param executionPlanId     ID do plano de execução.
         * @param workflowId          ID do workflow.
         * @param pipelineExecutionId ID da execução do pipeline.
         * @param sourceRecordId      ID do registro fonte.
         * @param canonicalRecordId   ID do registro canônico.
         * @param publicationId       ID da publicação.
         */
        public LineageRequest(final String consentId, final String resourceId,
                final String executionPlanId, final String workflowId,
                final String pipelineExecutionId, final String sourceRecordId,
                final String canonicalRecordId, final String publicationId) {
            this.consentId = consentId;
            this.resourceId = resourceId;
            this.executionPlanId = executionPlanId;
            this.workflowId = workflowId;
            this.pipelineExecutionId = pipelineExecutionId;
            this.sourceRecordId = sourceRecordId;
            this.canonicalRecordId = canonicalRecordId;
            this.publicationId = publicationId;
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
         * Retorna o ID do recurso.
         *
         * @return resourceId.
         */
        public String getResourceId() {
            return resourceId;
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
         * Retorna o ID do workflow.
         *
         * @return workflowId.
         */
        public String getWorkflowId() {
            return workflowId;
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
         * Retorna o ID do registro fonte.
         *
         * @return sourceRecordId.
         */
        public String getSourceRecordId() {
            return sourceRecordId;
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
         * Retorna o ID da publicação.
         *
         * @return publicationId.
         */
        public String getPublicationId() {
            return publicationId;
        }
    }
}
