package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationStatus;
import com.aurix.platform.openfinance.reconciliation.service.ReconciliationService;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação da atividade Temporal de reconciliação pós-publicação e reparo.
 * Delega para {@link ReconciliationService}.
 */
@Component
public class ReconcileActivityImpl implements ReconcileActivity {

    private static final Logger log = LoggerFactory.getLogger(ReconcileActivityImpl.class);

    private final ReconciliationService reconciliationService;

    public ReconcileActivityImpl(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Override
    public ReconcileOutput reconcile(ReconcileInput input) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Executando ReconcileActivity para plano: {}, activity: {}", input.getPlanId(), activityId);

        Map<String, ReconciliationService.NodeCounts> contagens = new LinkedHashMap<>();
        if (input.getExtractedCounts() != null) {
            for (Map.Entry<String, Integer> entry : input.getExtractedCounts().entrySet()) {
                String nodeId = entry.getKey();
                int extracted = entry.getValue() != null ? entry.getValue() : 0;
                int published = input.getPublishedCounts() != null
                        ? input.getPublishedCounts().getOrDefault(nodeId, 0) : 0;
                contagens.put(nodeId, new ReconciliationService.NodeCounts(extracted, published));
            }
        }

        ReconciliationService.ReconciliationResult resultado =
                reconciliationService.reconcileAfterPublish(input.getPlanId(), contagens);

        ReconcileOutput output = new ReconcileOutput();
        output.setPlanId(input.getPlanId());
        output.setHasDivergences(resultado.isHasDivergences());
        output.setDivergences(resultado.getDivergences() != null ? resultado.getDivergences() : List.of());

        if (resultado.getRecords() != null) {
            List<String> divergentIds = resultado.getRecords().stream()
                    .filter(r -> r.getStatus() == ReconciliationStatus.DIVERGENCIA_DETECTADA)
                    .map(ReconciliationRecord::getReconciliationId)
                    .toList();
            output.setDivergentReconciliationIds(divergentIds);
        }

        return output;
    }

    @Override
    public RepairOutput repair(String reconciliationId) {
        log.info("Executando reparo Temporal para reconciliação: {}", reconciliationId);
        ReconciliationService.RepairResult resultado = reconciliationService.reparar(reconciliationId);

        RepairOutput output = new RepairOutput();
        output.setReconciliationId(reconciliationId);
        output.setSuccess(resultado.isSuccess());
        output.setRecordsRepaired(resultado.getRecordsRepaired());
        output.setErrorMessage(resultado.getErrorMessage());
        return output;
    }
}
