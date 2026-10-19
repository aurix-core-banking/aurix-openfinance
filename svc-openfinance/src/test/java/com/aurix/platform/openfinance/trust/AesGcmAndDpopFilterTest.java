package com.aurix.platform.openfinance.trust;

import com.aurix.platform.openfinance.trust.crypto.AesGcmAttributeConverter;
import com.aurix.platform.openfinance.trust.dto.TrustResult;
import com.aurix.platform.openfinance.trust.filter.DpopFilter;
import com.aurix.platform.openfinance.trust.service.TrustPlaneService;
import com.aurix.platform.openfinance.trust.verifier.ProofOfPossessionVerifier;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de criptografia AES-256-GCM em repouso e filtro FAPI-Brazil/DPoP (Issue #10).
 */
class AesGcmAndDpopFilterTest {

    @Test
    @DisplayName("AesGcmAttributeConverter deve criptografar com AES-256-GCM (IV aleatório) e descriptografar fielmente")
    void deveCriptografarEDescriptografarComAes256Gcm() {
        AesGcmAttributeConverter converter = new AesGcmAttributeConverter("chave-teste-aurix-2026");
        String segredoOriginal = "whsec_openfinance_secret_987654321";

        String cifrado1 = converter.convertToDatabaseColumn(segredoOriginal);
        String cifrado2 = converter.convertToDatabaseColumn(segredoOriginal);

        assertNotNull(cifrado1);
        assertTrue(cifrado1.startsWith("ENC:v1:"));
        assertNotEquals(segredoOriginal, cifrado1);
        assertNotEquals(cifrado1, cifrado2, "IV aleatório de 12 bytes deve produzir ciphertexts distintos");

        assertEquals(segredoOriginal, converter.convertToEntityAttribute(cifrado1));
        assertEquals(segredoOriginal, converter.convertToEntityAttribute(cifrado2));
        assertEquals("legado-sem-prefixo", converter.convertToEntityAttribute("legado-sem-prefixo"));
    }

    @Test
    @DisplayName("DpopFilter deve ecoar ou gerar x-fapi-interaction-id e delegar ao TrustPlaneService quando mTLS presente")
    void deveEcoarFapiInteractionIdEDelegarAoTrustPlaneService() throws Exception {
        ProofOfPossessionVerifier popVerifier = mock(ProofOfPossessionVerifier.class);
        TrustPlaneService trustPlaneService = mock(TrustPlaneService.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(trustPlaneService.verify(anyMap(), eq("GET"), anyString(), any(), eq("dpop-jwt-valido")))
                .thenReturn(new TrustResult(
                        true,
                        TrustResult.ChannelStatus.VERIFIED,
                        TrustResult.PopStatus.VERIFIED,
                        "CN=tpp.openfinance.br"));

        DpopFilter filter = new DpopFilter(popVerifier, trustPlaneService, true, true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/consents/123");
        request.addHeader("DPoP", "dpop-jwt-valido");
        request.addHeader("X-SSL-Client-Verify", "SUCCESS");
        request.addHeader("X-SSL-Client-S-DN", "CN=tpp.openfinance.br");
        request.addHeader("x-fapi-interaction-id", "fapi-corr-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("fapi-corr-999", response.getHeader("x-fapi-interaction-id"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("DpopFilter deve gerar x-fapi-interaction-id automaticamente quando ausente e rejeitar 401 se DPoP inválido")
    void deveGerarFapiInteractionIdERejeitarDpopInvalido() throws Exception {
        ProofOfPossessionVerifier popVerifier = mock(ProofOfPossessionVerifier.class);
        TrustPlaneService trustPlaneService = mock(TrustPlaneService.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(popVerifier.verify(any(), anyString(), anyString(), any())).thenReturn(false);

        DpopFilter filter = new DpopFilter(popVerifier, trustPlaneService, true, true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/plans/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNotNull(response.getHeader("x-fapi-interaction-id"));
        verifyNoInteractions(filterChain);
    }
}
