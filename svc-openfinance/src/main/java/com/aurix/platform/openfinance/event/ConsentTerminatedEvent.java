package com.aurix.platform.openfinance.event;

import java.util.Map;

/**
 * Evento de domínio publicado quando um consentimento é revogado ({@code consent.revoked.v1})
 * ou expira ({@code consent.expired.v1}), permitindo que planos e contextos autorizados sejam
 * invalidados em cascata tanto em execução local quanto via mensageria.
 */
public record ConsentTerminatedEvent(
        String eventType,
        String consentId,
        String reason,
        Map<String, Object> envelope
) {
}
