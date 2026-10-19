package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.temporal.activity.ReconcileActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Implementação do workflow Temporal de reconciliação e auto-reparo
 * ({@code architecture/flows.yaml} — {@code reconciliation}).
 */
public class ReconciliationWorkflowImpl implements ReconciliationWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationWorkflowImpl.class);

    private final ReconcileActivity reconcileActivity = Workflow.newActivityStub(
            ReconcileActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    private String faseAtual = "INICIANDO";

    @Override
    public ReconciliationWorkflowResult reconciliar(ReconciliationWorkflowInput input) {
        log.info("Iniciando ReconciliationWorkflow para plano: {}", input.getPlanId());
        faseAtual = "COMPARANDO";

        ReconcileActivity.ReconcileInput reconcileInput = new ReconcileActivity.ReconcileInput();
        reconcileInput.setPlanId(input.getPlanId());
        reconcileInput.setExtractedCounts(input.getExtractedCounts());
        reconcileInput.setPublishedCounts(input.getPublishedCounts());

        ReconcileActivity.ReconcileOutput output = reconcileActivity.reconcile(reconcileInput);

        ReconciliationWorkflowResult result = new ReconciliationWorkflowResult();
        result.setPlanId(input.getPlanId());
        result.setHasDivergences(output.isHasDivergences());
        result.setDivergences(output.getDivergences());

        if (output.isHasDivergences() && input.isAutoRepair() && output.getDivergentReconciliationIds() != null) {
            faseAtual = "REPARANDO";
            int totalReparados = 0;
            boolean todosReparados = true;
            for (String reconciliationId : output.getDivergentReconciliationIds()) {
                ReconcileActivity.RepairOutput repairOutput = reconcileActivity.repair(reconciliationId);
                if (repairOutput.isSuccess()) {
                    totalReparados += repairOutput.getRecordsRepaired();
                } else {
                    todosReparados = false;
                }
            }
            result.setRepaired(todosReparados);
            result.setTotalRecordsRepaired(totalReparados);
        }

        faseAtual = "CONCLUIDO";
        return result;
    }

    @Override
    public String getFaseAtual() {
        return faseAtual;
    }
}
