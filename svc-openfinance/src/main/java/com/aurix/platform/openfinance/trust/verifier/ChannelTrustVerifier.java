package com.aurix.platform.openfinance.trust.verifier;

import com.aurix.platform.openfinance.trust.dto.ChannelIdentity;

/**
 * Porta de verificação de identidade do canal (mTLS).
 *
 * <p>Per issue #1, mTLS e DPoP são concerns de naturezas diferentes — duas portas,
 * um serviço. Esta porta cuida da identidade do canal de transporte.
 */
public interface ChannelTrustVerifier {

    /**
     * Extrai a identidade verificada do canal.
     *
     * @param headers mapa de cabeçalhos da requisição
     * @return a identidade verificada do cliente no canal, ou null se não verificada
     */
    ChannelIdentity verify(java.util.Map<String, String> headers);
}
