package com.aurix.platform.openfinance.trust.service;

import com.aurix.platform.openfinance.trust.dto.TrustResult;
import com.aurix.platform.openfinance.trust.verifier.ChannelTrustVerifier;
import com.aurix.platform.openfinance.trust.verifier.ProofOfPossessionVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orquestra as duas portas de confiança (canal mTLS + prova de posse DPoP)
 * e expõe {@code verify(...) -> TrustResult}.
 *
 * <p>Per issue #1: mTLS e DPoP são concerns de naturezas diferentes. Este
 * serviço é a frente unificada que as duas pequenas portas alimentam.
 */
@Service
public class TrustPlaneService {

    private static final Logger log = LoggerFactory.getLogger(TrustPlaneService.class);

    private final ChannelTrustVerifier channelTrustVerifier;
    private final ProofOfPossessionVerifier proofOfPossessionVerifier;

    public TrustPlaneService(
            ChannelTrustVerifier channelTrustVerifier,
            ProofOfPossessionVerifier proofOfPossessionVerifier) {
        this.channelTrustVerifier = channelTrustVerifier;
        this.proofOfPossessionVerifier = proofOfPossessionVerifier;
    }

    /**
     * Verifica a confiança de uma requisição (canal + DPoP).
     */
    public TrustResult verify(Map<String, String> headers, String httpMethod,
                              String requestUri, String accessTokenJkt, String dpopProof) {
        TrustResult.ChannelStatus channelStatus = verifyChannel(headers);
        TrustResult.PopStatus popStatus = verifyProofOfPossession(dpopProof, httpMethod,
                requestUri, accessTokenJkt);

        boolean allowed = channelStatus == TrustResult.ChannelStatus.VERIFIED
                && popStatus == TrustResult.PopStatus.VERIFIED;

        TrustResult result = new TrustResult(allowed, channelStatus, popStatus, headers.get("subject"));
        if (allowed) {
            log.debug("Requisição confiável: método={}, uri={}", httpMethod, requestUri);
        } else {
            log.warn("Requisição NÃO confiável: canal={}, dpop={}", channelStatus, popStatus);
        }
        return result;
    }

    private TrustResult.ChannelStatus verifyChannel(Map<String, String> headers) {
        var identity = channelTrustVerifier.verify(headers);
        return identity != null && identity.mutualTls()
                ? TrustResult.ChannelStatus.VERIFIED
                : TrustResult.ChannelStatus.NOT_VERIFIED;
    }

    private TrustResult.PopStatus verifyProofOfPossession(String dpopProof, String httpMethod,
                                                          String requestUri, String accessTokenJkt) {
        boolean ok = proofOfPossessionVerifier.verify(dpopProof, httpMethod, requestUri, accessTokenJkt);
        return ok ? TrustResult.PopStatus.VERIFIED : TrustResult.PopStatus.NOT_VERIFIED;
    }
}
