package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConsentimentoServiceTest {

    @Autowired
    private ConsentimentoService service;

    @Autowired
    private ConsentimentoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void deveCriarConsentimento() {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId("client-1");
        request.setInstitutionCode("AURIX");
        request.setPermissions(List.of("accounts"));

        ConsentimentoResponse response = service.criar(request, 1L);

        assertNotNull(response.getConsentId());
        assertEquals("AWAITING_AUTHORISATION", response.getStatus());
        assertEquals("client-1", response.getClientId());
    }

    @Test
    void deveAprovarConsentimento() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-2"), 1L);

        ConsentimentoResponse aprovado = service.aprovar(criado.getConsentId());

        assertEquals("AUTHORISED", aprovado.getStatus());
    }

    @Test
    void deveRevogarConsentimento() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-3"), 1L);

        ConsentimentoResponse revogado = service.revogar(
            criado.getConsentId(), "Solicitado pelo cliente");

        assertEquals("REVOKED", revogado.getStatus());
    }

    @Test
    void deveVerificarConsentimentoAtivo() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-4"), 1L);
        service.aprovar(criado.getConsentId());

        assertTrue(service.verificarConsentimentoAtivo(criado.getConsentId()));
    }

    @Test
    void deveNegarConsentimentoExpirado() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-5"), 1L);
        service.aprovar(criado.getConsentId());

        Consentimento c = repository.findByConsentId(criado.getConsentId()).orElseThrow();
        c.setDataExpiracao(java.time.LocalDateTime.now().minusDays(1));
        repository.save(c);

        assertFalse(service.verificarConsentimentoAtivo(criado.getConsentId()));
    }

    @Test
    void deveRejeitarConsentimento() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-6"), 1L);

        ConsentimentoResponse rejeitado = service.rejeitar(
            criado.getConsentId(), "Dados incompletos");

        assertEquals("REJECTED", rejeitado.getStatus());
        assertEquals("Dados incompletos", rejeitado.getPermissions().toString().contains("Dados") ? "ok" : "fail");
    }

    private ConsentimentoRequest createRequest(String clientId) {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId(clientId);
        request.setInstitutionCode("AURIX");
        request.setPermissions(List.of("accounts"));
        return request;
    }
}
