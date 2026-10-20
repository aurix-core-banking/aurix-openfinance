package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.PixPaymentInitiationRequest;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PixControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    private String consentIdValido;

    @BeforeEach
    void setUp() {
        consentIdValido = "urn:aurix:consent-test-" + UUID.randomUUID();
        Consentimento c = new Consentimento();
        c.setConsentId(consentIdValido);
        c.setClientId("itp-client-app");
        c.setUserId(2002L);
        c.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        c.setPermissions("payments,pix");
        c.setDataCriacao(LocalDateTime.now().minusDays(1));
        c.setDataAprovacao(LocalDateTime.now().minusDays(1));
        c.setDataExpiracao(LocalDateTime.now().plusDays(90));
        consentimentoRepository.save(c);
    }

    @Test
    @DisplayName("POST /open-finance/v1/pix/payments - deve iniciar pagamento Pix com sucesso (201 Created)")
    void deveIniciarPagamentoPixComSucesso() throws Exception {
        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                consentIdValido,
                new BigDecimal("125.50"),
                "contato@fornecedor.com.br",
                "EMAIL",
                "Pagamento de Fatura via Open Finance"
        );

        mockMvc.perform(post("/open-finance/v1/pix/payments")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.consentId").value(consentIdValido))
                .andExpect(jsonPath("$.status").value("ACSC"))
                .andExpect(jsonPath("$.valor").value(125.50))
                .andExpect(jsonPath("$.chavePix").value("contato@fornecedor.com.br"))
                .andExpect(jsonPath("$.dataLiquidacao").isNotEmpty());
    }

    @Test
    @DisplayName("POST /open-finance/v1/pix/payments - deve rejeitar requisição sem consentId (400 Bad Request)")
    void deveRejeitarPagamentoSemConsentId() throws Exception {
        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest();
        request.setValor(new BigDecimal("50.00"));
        request.setChavePix("12345678901");

        mockMvc.perform(post("/open-finance/v1/pix/payments")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /open-finance/v1/pix/payments - deve rejeitar requisição com valor zero ou negativo (400 Bad Request)")
    void deveRejeitarPagamentoComValorInvalido() throws Exception {
        PixPaymentInitiationRequest request = new PixPaymentInitiationRequest(
                consentIdValido,
                BigDecimal.ZERO,
                "chave-aleatoria",
                "ALEATORIA",
                "Valor zero"
        );

        mockMvc.perform(post("/open-finance/v1/pix/payments")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /open-finance/v1/pix/payments/{id} - deve retornar 404 para pagamento inexistente")
    void deveRetornar404ParaPagamentoInexistente() throws Exception {
        mockMvc.perform(get("/open-finance/v1/pix/payments/PAY-NAO-EXISTE")
                .with(jwt()))
                .andExpect(status().isNotFound());
    }
}
