package com.aurix.platform.openfinance.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Resposta com métricas regulatórias de SLA de APIs (Instrução Normativa BACEN nº 106).
 */
public record MetricasSlaResponse(
        long totalRequisicoes,
        long requisicoesSucesso,
        double taxaSucessoPercentual,
        long requisicoesErroCliente,
        long requisicoesErroServidor,
        double latenciaMediaMs,
        double latenciaP50Ms,
        double latenciaP95Ms,
        double latenciaP99Ms,
        double limiteP95RegulatorioMs,
        double limiteP99RegulatorioMs,
        boolean slaAtendido,
        Map<String, MetricaDominioResponse> metricasPorDominio,
        Instant dataConsulta
) {
}
