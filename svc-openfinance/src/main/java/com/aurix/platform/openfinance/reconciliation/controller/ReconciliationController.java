package com.aurix.platform.openfinance.reconciliation.controller;

import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationStatus;
import com.aurix.platform.openfinance.reconciliation.repository.ReconciliationRecordRepository;
import com.aurix.platform.openfinance.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller para gestão de reconciliação Open Finance.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "Reconciliação", description = "API para reconciliação de dados Open Finance")
public class ReconciliationController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReconciliationController.class);
    private final ReconciliationService reconciliationService;
    private final ReconciliationRecordRepository reconciliationRepository;

    public ReconciliationController(ReconciliationService reconciliationService,
                                    ReconciliationRecordRepository reconciliationRepository) {
        this.reconciliationService = reconciliationService;
        this.reconciliationRepository = reconciliationRepository;
    }

    @GetMapping("/{planId}/status")
    @Operation(summary = "Status da reconciliação", description = "Retorna o status da reconciliação de um plano")
    public ResponseEntity<Map<String, Object>> consultarStatus(
            @Parameter(description = "ID do plano") @PathVariable String planId) {
        log.info("Consultando status de reconciliação - plano: {}", planId);

        List<ReconciliationRecord> records = reconciliationRepository.findByPlanId(planId);
        long totalRecords = records.size();
        long conciliados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.CONCLUIDA).count();
        long comDivergencia = records.stream()
                .filter(r -> r.getStatus() == ReconciliationStatus.DIVERGENCIA_DETECTADA).count();
        long reparados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.REPARADA).count();
        long falhados = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.FALHADA).count();

        return ResponseEntity.ok(Map.of(
                "plan_id", planId,
                "total_registros", totalRecords,
                "conciliados", conciliados,
                "com_divergencia", comDivergencia,
                "reparados", reparados,
                "falhados", falhados
        ));
    }

    @PostMapping("/{planId}/repair")
    @Operation(summary = "Reparar divergências", description = "Executa reparo automático das divergências de um plano")
    public ResponseEntity<Map<String, Object>> reparar(
            @Parameter(description = "ID do plano") @PathVariable String planId) {
        log.info("Iniciando reparo de divergências - plano: {}", planId);

        List<ReconciliationRecord> records = reconciliationRepository
                .findByPlanIdAndStatus(planId, ReconciliationStatus.DIVERGENCIA_DETECTADA);

        if (records.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "plan_id", planId,
                    "message", "Nenhuma divergência encontrada para reparo",
                    "records_repaired", 0
            ));
        }

        int totalReparados = 0;
        for (ReconciliationRecord record : records) {
            ReconciliationService.RepairResult result = reconciliationService.reparar(record.getReconciliationId());
            if (result.isSuccess()) {
                totalReparados += result.getRecordsRepaired();
            }
        }

        return ResponseEntity.ok(Map.of(
                "plan_id", planId,
                "records_repaired", totalReparados,
                "total_records_processed", records.size()
        ));
    }

    @GetMapping("/{planId}/report")
    @Operation(summary = "Relatório de reconciliação", description = "Gera relatório completo de reconciliação do plano")
    public ResponseEntity<ReconciliationService.ReconciliationReport> gerarRelatorio(
            @Parameter(description = "ID do plano") @PathVariable String planId) {
        log.info("Gerando relatório de reconciliação - plano: {}", planId);
        ReconciliationService.ReconciliationReport report = reconciliationService.gerarRelatorio(planId);
        return ResponseEntity.ok(report);
    }
}
