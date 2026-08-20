package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.temporal.workflow.dto.ExecutionPlanRequest;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow de orquestração da extração de dados Open Finance.
 * INV04: Este workflow NÃO pode tomar decisões de autorização.
 * Ele recebe um plano de execução já autorizado e o segue fielmente.
 */
@WorkflowInterface
public interface DataExtractionWorkflow {

    @WorkflowMethod
    ExtractionResult execute(ExecutionPlanRequest plano);

    @SignalMethod
    void cancelarExtracao(String motivo);

    @QueryMethod
    ExtractionStatus getStatus();
}
