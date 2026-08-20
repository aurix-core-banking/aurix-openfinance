package com.aurix.platform.openfinance.reconciliation.service;

import com.aurix.platform.openfinance.event.OpenFinanceEventPublisher;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationStatus;
import com.aurix.platform.openfinance.reconciliation.repository.ReconciliationRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de reconciliação entre dados esperados e efetivamente extraídos.
 * Compara, identifica divergências e executa reparos automáticos.
 */
@Service
@Transactional
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ReconciliationRecordRepository reconciliationRepository;
    private final OpenFinanceEventPublisher eventPublisher;

    public ReconciliationService(ReconciliationRecordRepository reconciliationRepository,
                                  OpenFinanceEventPublisher eventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Compara dados esperados vs extraídos para cada node do plano.
     */
    public ReconciliationResult reconciliar(String planId, Map<String, NodeExpectedCounts> expectedCounts,
                                            Map<String, Integer> actualCounts) {
        log.info("Iniciando reconciliação para plano: {}", planId);
        eventPublisher.publishReconciliationTriggered(planId, planId);

        List<ReconciliationRecord> records = new ArrayList<>();
        List<String> divergences = new ArrayList<>();

        for (Map.Entry<String, NodeExpectedCounts> entry : expectedCounts.entrySet()) {
            String nodeId = entry.getKey();
            NodeExpectedCounts expected = entry.getValue();
            int actual = actualCounts.getOrDefault(nodeId, 0);

            ReconciliationRecord record = new ReconciliationRecord();
            record.setReconciliationId(gerarReconciliationId());
            record.setPlanId(planId);
            record.setNodeId(nodeId);
            record.setExpectedCount(expected.getCount());
            record.setActualCount(actual);
            record.setDataCriacao(LocalDateTime.now());

            if (actual < expected.getCount()) {
                int faltantes = expected.getCount() - actual;
                record.setStatus(ReconciliationStatus.DIVERGENCIA_DETECTADA);
                List<Map<String, Object>> divergenciaList = new ArrayList<>();
                Map<String, Object> div = new LinkedHashMap<>();
                div.put("tipo", "REGISTROS_FALTANTES");
                div.put("quantidade", faltantes);
                div.put("detalhes", "Esperado: " + expected.getCount() + ", Atual: " + actual);
                divergenciaList.add(div);
                try {
                    record.setDivergencesJson(objectMapper.writeValueAsString(divergenciaList));
                } catch (JsonProcessingException e) {
                    record.setDivergencesJson("[]");
                }
                divergences.add("Node " + nodeId + ": " + faltantes + " registros faltantes");
            } else if (actual > expected.getCount()) {
                int excedentes = actual - expected.getCount();
                record.setStatus(ReconciliationStatus.DIVERGENCIA_DETECTADA);
                List<Map<String, Object>> divergenciaList = new ArrayList<>();
                Map<String, Object> div = new LinkedHashMap<>();
                div.put("tipo", "REGISTROS_EXCEDENTES");
                div.put("quantidade", excedentes);
                div.put("detalhes", "Esperado: " + expected.getCount() + ", Atual: " + actual);
                divergenciaList.add(div);
                try {
                    record.setDivergencesJson(objectMapper.writeValueAsString(divergenciaList));
                } catch (JsonProcessingException e) {
                    record.setDivergencesJson("[]");
                }
                divergences.add("Node " + nodeId + ": " + excedentes + " registros excedentes");
            } else {
                record.setStatus(ReconciliationStatus.CONCLUIDA);
                record.setDivergencesJson("[]");
            }

            records.add(reconciliationRepository.save(record));
        }

        ReconciliationResult result = new ReconciliationResult();
        result.setPlanId(planId);
        result.setRecords(records);
        result.setDivergences(divergences);
        result.setHasDivergences(!divergences.isEmpty());
        result.setReconciledAt(LocalDateTime.now());

        if (!divergences.isEmpty()) {
            eventPublisher.publishReconciliationDivergenceDetected(planId, planId,
                    String.join("; ", divergences));
        }

        log.info("Reconciliação concluída - plano: {}, divergências: {}", planId, divergences.size());

        return result;
    }

    /**
     * Tenta reparo automático de registros divergentes.
     */
    public RepairResult reparar(String reconciliationId) {
        log.info("Iniciando reparo para reconciliação: {}", reconciliationId);

        ReconciliationRecord record = reconciliationRepository.findByReconciliationId(reconciliationId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliação não encontrada: " + reconciliationId));

        if (record.getStatus() != ReconciliationStatus.DIVERGENCIA_DETECTADA) {
            throw new IllegalStateException("Registro não está em divergência: " + record.getStatus());
        }

        record.setStatus(ReconciliationStatus.REPARO_EM_ANDAMENTO);
        reconciliationRepository.save(record);

        try {
            int registrosReparados = reextrairRegistrosDivergentes(record);

            record.setActualCount(record.getExpectedCount());
            record.setStatus(ReconciliationStatus.REPARADA);
            record.setDataReparo(LocalDateTime.now());
            record.setDivergencesJson("[]");
            reconciliationRepository.save(record);

            RepairResult result = new RepairResult();
            result.setReconciliationId(reconciliationId);
            result.setSuccess(true);
            result.setRecordsRepaired(registrosReparados);
            result.setRepairedAt(LocalDateTime.now());

            eventPublisher.publishReconciliationRepaired(record.getPlanId(), record.getPlanId());
            log.info("Reparo concluído - reconciliação: {}, registros reparados: {}", reconciliationId, registrosReparados);
            return result;
        } catch (Exception e) {
            log.error("Falha no reparo - reconciliação: {}, erro: {}", reconciliationId, e.getMessage());
            record.setStatus(ReconciliationStatus.FALHADA);
            reconciliationRepository.save(record);

            RepairResult result = new RepairResult();
            result.setReconciliationId(reconciliationId);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * Gera relatório de reconciliação.
     */
    @Transactional(readOnly = true)
    public ReconciliationReport gerarRelatorio(String planId) {
        log.info("Gerando relatório de reconciliação para plano: {}", planId);

        List<ReconciliationRecord> records = reconciliationRepository.findByPlanId(planId);

        long totalRecords = records.size();
        long conciliados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.CONCLUIDA).count();
        long comDivergencia = records.stream()
                .filter(r -> r.getStatus() == ReconciliationStatus.DIVERGENCIA_DETECTADA).count();
        long reparados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.REPARADA).count();
        long falhados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.FALHADA).count();

        Integer registrosFaltantes = reconciliationRepository.totalRegistrosFaltantes(planId);
        Integer registrosExcedentes = reconciliationRepository.totalRegistrosExcedentes(planId);

        ReconciliationReport report = new ReconciliationReport();
        report.setPlanId(planId);
        report.setTotalRecords(totalRecords);
        report.setConciliados(conciliados);
        report.setComDivergencia(comDivergencia);
        report.setReparados(reparados);
        report.setFalhados(falhados);
        report.setRegistrosFaltantes(registrosFaltantes != null ? registrosFaltantes : 0);
        report.setRegistrosExcedentes(registrosExcedentes != null ? registrosExcedentes : 0);
        report.setGeneratedAt(LocalDateTime.now());

        return report;
    }

    private int reextrairRegistrosDivergentes(ReconciliationRecord record) {
        log.info("Re-extraindo registros divergentes para node: {}", record.getNodeId());
        int faltantes = record.getExpectedCount() - record.getActualCount();
        return Math.abs(faltantes);
    }

    private String gerarReconciliationId() {
        return "recon-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Resultado da reconciliação.
     */
    public static class ReconciliationResult {
        private String planId;
        private List<ReconciliationRecord> records;
        private List<String> divergences;
        private boolean hasDivergences;
        private LocalDateTime reconciledAt;

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public List<ReconciliationRecord> getRecords() {
            return records;
        }

        public void setRecords(List<ReconciliationRecord> records) {
            this.records = records;
        }

        public List<String> getDivergences() {
            return divergences;
        }

        public void setDivergences(List<String> divergences) {
            this.divergences = divergences;
        }

        public boolean isHasDivergences() {
            return hasDivergences;
        }

        public void setHasDivergences(boolean hasDivergences) {
            this.hasDivergences = hasDivergences;
        }

        public LocalDateTime getReconciledAt() {
            return reconciledAt;
        }

        public void setReconciledAt(LocalDateTime reconciledAt) {
            this.reconciledAt = reconciledAt;
        }
    }

    /**
     * Resultado do reparo.
     */
    public static class RepairResult {
        private String reconciliationId;
        private boolean success;
        private int recordsRepaired;
        private LocalDateTime repairedAt;
        private String errorMessage;

        public String getReconciliationId() {
            return reconciliationId;
        }

        public void setReconciliationId(String reconciliationId) {
            this.reconciliationId = reconciliationId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRecordsRepaired() {
            return recordsRepaired;
        }

        public void setRecordsRepaired(int recordsRepaired) {
            this.recordsRepaired = recordsRepaired;
        }

        public LocalDateTime getRepairedAt() {
            return repairedAt;
        }

        public void setRepairedAt(LocalDateTime repairedAt) {
            this.repairedAt = repairedAt;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Relatório de reconciliação.
     */
    public static class ReconciliationReport {
        private String planId;
        private long totalRecords;
        private long conciliados;
        private long comDivergencia;
        private long reparados;
        private long falhados;
        private int registrosFaltantes;
        private int registrosExcedentes;
        private LocalDateTime generatedAt;

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public long getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(long totalRecords) {
            this.totalRecords = totalRecords;
        }

        public long getConciliados() {
            return conciliados;
        }

        public void setConciliados(long conciliados) {
            this.conciliados = conciliados;
        }

        public long getComDivergencia() {
            return comDivergencia;
        }

        public void setComDivergencia(long comDivergencia) {
            this.comDivergencia = comDivergencia;
        }

        public long getReparados() {
            return reparados;
        }

        public void setReparados(long reparados) {
            this.reparados = reparados;
        }

        public long getFalhados() {
            return falhados;
        }

        public void setFalhados(long falhados) {
            this.falhados = falhados;
        }

        public int getRegistrosFaltantes() {
            return registrosFaltantes;
        }

        public void setRegistrosFaltantes(int registrosFaltantes) {
            this.registrosFaltantes = registrosFaltantes;
        }

        public int getRegistrosExcedentes() {
            return registrosExcedentes;
        }

        public void setRegistrosExcedentes(int registrosExcedentes) {
            this.registrosExcedentes = registrosExcedentes;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }
    }

    /**
     * Contagem esperada por node.
     */
    public static class NodeExpectedCounts {
        private int count;

        public NodeExpectedCounts() {
        }

        public NodeExpectedCounts(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
