package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.PixPaymentInitiationRequest;
import com.aurix.platform.openfinance.dto.PixPaymentInitiationResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.PixConsentido;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.PixConsentidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private PixConsentidoRepository repo;

    @Mock
    private ConsentimentoRepository consentimentoRepository;

    @InjectMocks
    private PixService service;

    private Consentimento consentimentoAutorizado;

    @BeforeEach
    void setUp() {
        consentimentoAutorizado = new Consentimento();
        consentimentoAutorizado.setConsentId("urn:aurix:consent-pix-123");
        consentimentoAutorizado.setClientId("itp-client-01");
        consentimentoAutorizado.setUserId(1001L);
        consentimentoAutorizado.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimentoAutorizado.setPermissions("payments,pix");
        consentimentoAutorizado.setDataCriacao(LocalDateTime.now().minusDays(1));
        consentimentoAutorizado.setDataExpiracao(LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("INV-001: Iniciação de Pix com consentimento AUTHORISED deve liquidar com status ACSC")
    void testIniciarPagamentoSucesso() {
        when(consentimentoRepository.findByConsentId("urn:aurix:consent-pix-123"))
                .thenReturn(Optional.of(consentimentoAutorizado));
        when(repo.save(any(PixConsentido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                "urn:aurix:consent-pix-123",
                new BigDecimal("250.75"),
                "destinatario@pix.com.br",
                "EMAIL",
                "Pagamento de Serviços via Open Finance"
        );

        PixPaymentInitiationResponse response = service.iniciarPagamento(request);

        assertNotNull(response);
        assertNotNull(response.getPaymentId());
        assertTrue(response.getPaymentId().startsWith("PAY-"));
        assertEquals("urn:aurix:consent-pix-123", response.getConsentId());
        assertEquals("ACSC", response.getStatus());
        assertEquals(new BigDecimal("250.75"), response.getValor());
        assertEquals("BRL", response.getMoeda());
        assertEquals("destinatario@pix.com.br", response.getChavePix());
        assertNotNull(response.getDataCriacao());
        assertNotNull(response.getDataLiquidacao());

        ArgumentCaptor<PixConsentido> captor = ArgumentCaptor.forClass(PixConsentido.class);
        verify(repo).save(captor.capture());
        PixConsentido salvo = captor.getValue();
        assertEquals("urn:aurix:consent-pix-123", salvo.getConsentId());
        assertEquals("destinatario@pix.com.br", salvo.getChavedePix());
        assertEquals("ACSC", salvo.getStatusPix());
    }

    @Test
    @DisplayName("INV-001: Iniciação de Pix sem consentimento existente deve falhar com erro regulatório")
    void testIniciarPagamentoSemConsentimentoFalha() {
        when(consentimentoRepository.findByConsentId("urn:aurix:consent-inexistente"))
                .thenReturn(Optional.empty());

        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                "urn:aurix:consent-inexistente",
                new BigDecimal("100.00"),
                "12345678901",
                "CPF",
                "Tentativa sem consentimento"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.iniciarPagamento(request));
        assertTrue(ex.getMessage().contains("INV-001: Consentimento não encontrado"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("INV-001: Iniciação de Pix com consentimento não autorizado (AWAITING_AUTHORISATION) deve falhar")
    void testIniciarPagamentoConsentimentoNaoAutorizadoFalha() {
        consentimentoAutorizado.setStatus(Consentimento.StatusConsentimento.AWAITING_AUTHORISATION);
        when(consentimentoRepository.findByConsentId("urn:aurix:consent-pix-123"))
                .thenReturn(Optional.of(consentimentoAutorizado));

        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                "urn:aurix:consent-pix-123",
                new BigDecimal("50.00"),
                "11987654321",
                "TELEFONE",
                "Pagamento não autorizado"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.iniciarPagamento(request));
        assertTrue(ex.getMessage().contains("INV-001: Pagamento Pix rejeitado"));
        assertTrue(ex.getMessage().contains("AWAITING_AUTHORISATION"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("INV-001: Iniciação de Pix com consentimento expirado deve falhar")
    void testIniciarPagamentoConsentimentoExpiradoFalha() {
        consentimentoAutorizado.setDataExpiracao(LocalDateTime.now().minusHours(2));
        when(consentimentoRepository.findByConsentId("urn:aurix:consent-pix-123"))
                .thenReturn(Optional.of(consentimentoAutorizado));

        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                "urn:aurix:consent-pix-123",
                new BigDecimal("80.00"),
                "chave-aleatoria-uuid",
                "ALEATORIA",
                "Consentimento expirado"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.iniciarPagamento(request));
        assertTrue(ex.getMessage().contains("INV-001: Pagamento Pix rejeitado"));
        assertTrue(ex.getMessage().contains("expirou"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Consulta de pagamento por ID existente deve retornar dados correspondentes")
    void testBuscarPagamentoExistente() {
        PixConsentido pix = new PixConsentido();
        pix.setPixId("PAY-123456789012345678");
        pix.setConsentId("urn:aurix:consent-pix-123");
        pix.setChavedePix("pix@banco.com.br");
        pix.setValor(new BigDecimal("1500.00"));
        pix.setMoeda("BRL");
        pix.setStatusPix("ACSC");
        pix.setDescricao("Compra de Equipamento");
        pix.setDataPix(LocalDateTime.now().minusMinutes(5));
        pix.setDataAtualizacao(LocalDateTime.now().minusMinutes(4));

        when(repo.findByPixId("PAY-123456789012345678")).thenReturn(Optional.of(pix));

        Optional<PixPaymentInitiationResponse> resultado = service.buscarPagamento("PAY-123456789012345678");

        assertTrue(resultado.isPresent());
        assertEquals("PAY-123456789012345678", resultado.get().getPaymentId());
        assertEquals("urn:aurix:consent-pix-123", resultado.get().getConsentId());
        assertEquals(new BigDecimal("1500.00"), resultado.get().getValor());
        assertEquals("ACSC", resultado.get().getStatus());
    }
}
