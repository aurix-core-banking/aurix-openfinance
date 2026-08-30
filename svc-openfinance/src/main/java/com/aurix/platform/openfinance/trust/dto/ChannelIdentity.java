package com.aurix.platform.openfinance.trust.dto;

/**
 * Identidade do canal de transporte (mTLS) já verificada.
 *
 * <p>Contém o subject do certificado do cliente e o hash da impressão digital
 * do certificado. Imutável.
 */
public record ChannelIdentity(
        String subject,
        String certificateSha256,
        boolean mutualTls
) {
}
