package com.aurix.platform.openfinance.dto;

import java.time.Instant;

/**
 * Resposta de métricas de disponibilidade regulatória segundo Instrução Normativa BACEN nº 106.
 */
public record DisponibilidadeResponse(
        String statusOperacional,
        double taxaDisponibilidadePercentual,
        double metaRegulatoriaPercentual,
        boolean emConformidade,
        long tempoAtividadeSegundos,
        String periodoReferencia,
        Instant dataConsulta
) {
}
