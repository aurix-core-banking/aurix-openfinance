package com.aurix.platform.openfinance.pipeline.lineage.service;

import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageChain;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de linhagem do pipeline Open Finance.
 * Registra e consulta a cadeia de rastreabilidade completa:
 * consentId → resourceId → executionPlanId → workflowId →
 * pipelineExecutionId → sourceRecordId → canonicalRecordId → publicationId.
 *
 * INV03: Sem linhagem, sem publicação. Persistida em {@code aurix_openfinance.lineage_records}
 * (via {@link LineageRecordRepository}) — sobrevive a restart/deploy/scale-out, ao contrário
 * de um cache em memória por instância.
 */
@Service
public class LineageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LineageService.class);

    private final LineageRecordRepository repository;

    public LineageService(final LineageRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra a linhagem completa de um dado no pipeline.
     *
     * @param request dados da requisição de linhagem.
     * @return registro de linhagem criado.
     */
    @Transactional
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
        record.setPublicationId(request.getPublicationId());

        record = repository.save(record);

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
    @Transactional
    public void atualizarPublicacao(final String lineageId, final String publicationId) {
        repository.findByLineageId(lineageId).ifPresent(record -> {
            record.setPublicationId(publicationId);
            record.setDataAtualizacao(LocalDateTime.now());
            repository.save(record);
            log.info("Publicação atualizada na linhagem: lineageId={}, publicationId={}",
                    lineageId, publicationId);
        });
    }

    /**
     * Traz a linha completa de linhagem a partir de uma publicação.
     *
     * @param publicationId ID da publicação.
     * @return cadeia de linhagem.
     */
    @Transactional(readOnly = true)
    public LineageChain traceLineage(final String publicationId) {
        log.info("Trazendo linhagem para publicação: {}", publicationId);

        List<LineageRecord> records = repository.findByPublicationIdOrderByCreatedAtAsc(publicationId);
        if (records.isEmpty()) {
            log.warn("Nenhuma linhagem encontrada para publicação: {}", publicationId);
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
    @Transactional(readOnly = true)
    public LineageChain traceLineageByConsent(final String consentId) {
        log.info("Trazendo linhagem por consentimento: {}", consentId);

        List<LineageRecord> records = repository.findByConsentIdOrderByCreatedAtAsc(consentId);
        if (records.isEmpty()) {
            return LineageChain.vazia(consentId);
        }

        String pubId = records.stream()
                .map(LineageRecord::getPublicationId)
                .filter(p -> p != null && !p.isBlank())
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

        public String getConsentId() {
            return consentId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public String getExecutionPlanId() {
            return executionPlanId;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public String getPipelineExecutionId() {
            return pipelineExecutionId;
        }

        public String getSourceRecordId() {
            return sourceRecordId;
        }

        public String getCanonicalRecordId() {
            return canonicalRecordId;
        }

        public String getPublicationId() {
            return publicationId;
        }
    }
}
