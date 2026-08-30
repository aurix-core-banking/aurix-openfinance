package com.aurix.platform.openfinance.trust.dto;

/**
 * Resultado da verificação de confiança de uma requisição.
 * Imutável.
 */
public record TrustResult(
        boolean allowed,
        ChannelStatus channelStatus,
        PopStatus popStatus,
        String subject
) {
    public enum ChannelStatus {
        VERIFIED, NOT_VERIFIED, NOT_REQUIRED
    }

    public enum PopStatus {
        VERIFIED, NOT_VERIFIED
    }
}
