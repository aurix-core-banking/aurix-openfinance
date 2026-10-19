package com.aurix.platform.openfinance.reconciliation.service;

/**
 * Porta de reparo de reconciliação para re-extração de nós divergentes.
 * Reside no pacote {@code reconciliation.service} para respeitar a aciclicidade
 * arquitetural ({@code planner -> temporal -> reconciliation}).
 */
@FunctionalInterface
public interface ReconciliationRepairExecutor {

    /**
     * Re-dispara ou re-extrai os registros divergentes do nó de um plano de execução.
     *
     * @param planId             identificador do plano.
     * @param nodeId             identificador do nó divergente.
     * @param registrosFaltantes quantidade estimada de registros faltantes/divergentes.
     * @return quantidade de registros re-extraídos/reparados.
     */
    int reextrairNode(String planId, String nodeId, int registrosFaltantes);
}
