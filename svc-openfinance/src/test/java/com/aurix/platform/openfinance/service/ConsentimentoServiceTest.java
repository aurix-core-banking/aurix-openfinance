package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.ConsentimentoVersao;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoVersaoRepository;
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

    @Autowired
    private ConsentimentoVersaoRepository versaoRepository;

    @BeforeEach
    void setUp() {
        versaoRepository.deleteAll();
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
    }

    // INV-006: cada transição gera uma nova versão append-only
    @Test
    void deveRegistrarVersoesNoCicloDeVida() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-7"), 1L);
        service.aprovar(criado.getConsentId());
        service.revogar(criado.getConsentId(), "Solicitado pelo cliente");

        List<ConsentimentoVersao> versoes =
            versaoRepository.findByConsentIdOrderByVersaoAsc(criado.getConsentId());

        assertEquals(3, versoes.size());
        assertEquals(1, versoes.get(0).getVersao());
        assertEquals(Consentimento.StatusConsentimento.AWAITING_AUTHORISATION, versoes.get(0).getStatus());
        assertEquals(2, versoes.get(1).getVersao());
        assertEquals(Consentimento.StatusConsentimento.AUTHORISED, versoes.get(1).getStatus());
        assertEquals(3, versoes.get(2).getVersao());
        assertEquals(Consentimento.StatusConsentimento.REVOKED, versoes.get(2).getStatus());
    }

    // INV-006: histórico imutável — versões anteriores permanecem intactas
    // (após aprovar, a versão AWAITING continua registrada como versão 1).
    @Test
    void devePreservarVersaoAnteriorAposTransicao() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-8"), 1L);
        service.aprovar(criado.getConsentId());

        ConsentimentoVersao v1 = versaoRepository
            .findByConsentIdAndVersao(criado.getConsentId(), 1).orElseThrow();
        ConsentimentoVersao v2 = versaoRepository
            .findByConsentIdAndVersao(criado.getConsentId(), 2).orElseThrow();

        assertEquals(Consentimento.StatusConsentimento.AWAITING_AUTHORISATION, v1.getStatus());
        assertEquals(Consentimento.StatusConsentimento.AUTHORISED, v2.getStatus());
        assertEquals("AUTHORISED", repository.findByConsentId(criado.getConsentId())
            .orElseThrow().getStatus().name());
    }

    // INV-006: mutação inválida é rejeitada (reaprovar consentimento revogado)
    @Test
    void deveRejeitarMutacaoInvalida() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-9"), 1L);
        service.revogar(criado.getConsentId(), "Solicitado pelo cliente");

        assertThrows(IllegalStateException.class,
            () -> service.aprovar(criado.getConsentId()));
    }

    @Test
    void deveNegarAprovacaoDeConsentimentoJaRejeitado() {
        ConsentimentoResponse criado = service.criar(
            createRequest("client-10"), 1L);
        service.rejeitar(criado.getConsentId(), "Dados incompletos");

        assertThrows(IllegalStateException.class,
            () -> service.aprovar(criado.getConsentId()));
    }

    private ConsentimentoRequest createRequest(String clientId) {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId(clientId);
        request.setInstitutionCode("AURIX");
        request.setPermissions(List.of("accounts"));
        return request;
    }
}
