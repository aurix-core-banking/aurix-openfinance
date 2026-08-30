package com.aurix.platform.openfinance.trust.service;

import java.time.LocalDateTime;

/**
 * Payload assinado: conteúdo original + assinatura em Base64URL + algoritmo.
 * Imutável.
 */
public record SignedPayload(
        String payload,
        String signature,
        String algorithm,
        LocalDateTime signedAt
) {
    public SignedPayload {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload não pode ser nulo/vazio");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("signature não pode ser nula/vazia");
        }
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm não pode ser nulo/vazio");
        }
    }
}
