package com.aurix.platform.openfinance.dto;

/**
 * Resposta com métricas consolidadas por domínio de dados do Open Finance.
 */
public record MetricaDominioResponse(
        String dominio,
        long totalRequisicoes,
        double latenciaMediaMs,
        double latenciaP95Ms,
        double taxaErroPercentual
) {
}
