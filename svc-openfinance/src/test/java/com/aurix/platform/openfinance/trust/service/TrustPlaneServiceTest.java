package com.aurix.platform.openfinance.trust.service;

import com.aurix.platform.openfinance.trust.dto.ChannelIdentity;
import com.aurix.platform.openfinance.trust.dto.TrustResult;
import com.aurix.platform.openfinance.trust.verifier.ChannelTrustVerifier;
import com.aurix.platform.openfinance.trust.verifier.ProofOfPossessionVerifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa o orquestrador TrustPlaneService (canal mTLS + DPoP).
 */
class TrustPlaneServiceTest {

    private final ChannelTrustVerifier channelVerify = headers ->
            headers != null && headers.containsKey("X-Client-Cert-Subject")
                    ? new ChannelIdentity("CN=client", "jkt", true)
                    : null;

    private final ProofOfPossessionVerifier popVerify = (proof, method, uri, jkt) ->
            "valid-proof".equals(proof);

    @Test
    void devePermitirQuandoCanalEProvaValidos() {
        TrustPlaneService service = new TrustPlaneService(channelVerify, popVerify);
        TrustResult result = service.verify(
                Map.of("X-Client-Cert-Subject", "CN=client"),
                "GET", "https://api/consents", "jkt-token", "valid-proof");

        assertTrue(result.allowed());
        assertEquals(TrustResult.ChannelStatus.VERIFIED, result.channelStatus());
        assertEquals(TrustResult.PopStatus.VERIFIED, result.popStatus());
    }

    @Test
    void deveBloquearQuandoCanalNaoVerificado() {
        TrustPlaneService service = new TrustPlaneService(channelVerify, popVerify);
        TrustResult result = service.verify(
                Map.of(), "GET", "https://api/consents", "jkt-token", "valid-proof");

        assertFalse(result.allowed());
        assertEquals(TrustResult.ChannelStatus.NOT_VERIFIED, result.channelStatus());
    }

    @Test
    void deveBloquearQuandoDpopInvalido() {
        TrustPlaneService service = new TrustPlaneService(channelVerify, popVerify);
        TrustResult result = service.verify(
                Map.of("X-Client-Cert-Subject", "CN=client"),
                "GET", "https://api/consents", "jkt-token", "bad-proof");

        assertFalse(result.allowed());
        assertEquals(TrustResult.PopStatus.NOT_VERIFIED, result.popStatus());
    }
}
