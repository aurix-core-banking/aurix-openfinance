package com.aurix.platform.openfinance.trust.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova que a "assinatura" agora é assimétrica real, verificável e com
 * não-repúdio — não mais o hash disfarçado que existia antes (issue #1).
 */
class EcDsaSigningServiceTest {

    private final EcDsaSigningService signingService = new EcDsaSigningService();

    @Test
    void deveAssinarComEs256_E_Verificar() {
        String payload = "contexto-autorizado-123|conteudo";
        SignedPayload signed = signingService.sign(payload, "ES256");

        assertNotNull(signed.signature());
        assertNotEquals(payload, signed.signature(), "assinatura não pode ser igual ao payload");
        assertTrue(signingService.verify(payload, signed.signature(), "ES256"));
    }

    @Test
    void deveRejeitarAlteracaoDePayload() {
        String payload = "dados-consentidos";
        SignedPayload signed = signingService.sign(payload, "RS256");

        assertFalse(signingService.verify(payload + "alterado", signed.signature(), "RS256"),
                "alteração no payload deve invalidar a assinatura");
    }

    @Test
    void assinaturaJaNaoEhHashInvertivel() {
        // Antes: assinatura era SHA-256(payload) — deterministico e inversivel.
        // Agora: com EC, mesmo payload assina valores DIFERENTES (nonce aleatorio).
        String payload = "mesmo-payload";
        SignedPayload a = signingService.sign(payload, "ES256");
        SignedPayload b = signingService.sign(payload, "ES256");

        assertNotEquals(a.signature(), b.signature(),
                "ES256 é não-determinístico; duas assinaturas não podem ser iguais");
    }

    @Test
    void deveAceitarEs384_e_rs256() {
        SignedPayload es384 = signingService.sign("x", "ES384");
        assertTrue(signingService.verify("x", es384.signature(), "ES384"));

        SignedPayload rs256 = signingService.sign("x", "RS256");
        assertTrue(signingService.verify("x", rs256.signature(), "RS256"));
    }

    @Test
    void deveRejeitarAlgoritmoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> signingService.sign("x", "BOGUS"));
    }

    @Test
    void deveExporChavePublicaBase64Url() {
        String pub = signingService.getPublicKeyB64Url("ES256");
        assertNotNull(pub);
        assertFalse(pub.isEmpty());
    }
}
