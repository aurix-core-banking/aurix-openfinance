package com.aurix.platform.openfinance.event;

import java.util.Map;

/**
 * Evento Spring interno emitido quando {@code consent.granted.v1} é publicado,
 * disparando a orquestração {@code consent-to-execution} declarada em {@code architecture/flows.yaml}.
 */
public record ConsentGrantedEvent(
        String consentId,
        String clientId,
        Map<String, Object> envelope
) {
}
