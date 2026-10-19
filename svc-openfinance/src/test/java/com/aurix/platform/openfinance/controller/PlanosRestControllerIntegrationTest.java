package com.aurix.platform.openfinance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração HTTP (MockMvc) dos controllers REST dos 5 planos
 * arquiteturais — Issue #20.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanosRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private static String id(String prefixo) {
        return prefixo + "-" + UUID.randomUUID();
    }

    // ─── Trust Plane ───

    @Test
    void trustDeveRetornarStatusComAlgoritmosEChavePublica() throws Exception {
        mockMvc.perform(get("/api/v1/trust/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signingAlgorithms", hasItem("ES256")))
                .andExpect(jsonPath("$.publicKeyBase64Url").isNotEmpty())
                .andExpect(header().exists("x-fapi-interaction-id"));
    }

    @Test
    void trustDeveRetornar404ParaReciboInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/trust/receipts/" + id("inexistente")).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void trustDeveNegarConfiancaSemCanalESemDpop() throws Exception {
        mockMvc.perform(post("/api/v1/trust/verify").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void trustDevePropagarFapiInteractionId() throws Exception {
        String interactionId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/trust/status").with(jwt())
                        .header("x-fapi-interaction-id", interactionId))
                .andExpect(status().isOk())
                .andExpect(header().string("x-fapi-interaction-id", interactionId));
    }

    // ─── Consent Plane: Policy Engine & Authorized Context ───

    @Test
    void policyDeveListarRegrasAtivas() throws Exception {
        mockMvc.perform(get("/api/v1/policy/rules").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void policyDeveRetornarTrilhaDeAuditoriaVaziaParaConsentimentoDesconhecido() throws Exception {
        mockMvc.perform(get("/api/v1/policy/decisions").with(jwt())
                        .param("consentId", id("consent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contextDeveRejeitarContextoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/contexts/" + id("ctx")).with(jwt()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void contextDeveListarContextosAtivosVaziosPorConsentimento() throws Exception {
        mockMvc.perform(get("/api/v1/contexts/consent/" + id("consent")).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── Resource Discovery ───

    @Test
    void discoveryDeveRejeitarGrafoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/resources/graph/" + id("consent")).with(jwt()))
                .andExpect(status().is4xxClientError());
    }

    // ─── Execution Plane: Planner & Reconciliation ───

    @Test
    void plannerDeveRejeitarPlanoSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/v1/plans").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("participante_id", "part-1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plannerDeveRetornar400ParaPlanoInexistente() throws Exception {
        String planId = id("plan");
        mockMvc.perform(get("/api/v1/plans/" + planId).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Plano não encontrado: " + planId));
        mockMvc.perform(get("/api/v1/plans/" + planId + "/status").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reconciliationDeveRetornarStatusZeradoParaPlanoSemRegistros() throws Exception {
        String planId = id("plan");
        mockMvc.perform(get("/api/v1/reconciliation/" + planId + "/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan_id").value(planId))
                .andExpect(jsonPath("$.total_registros").value(0))
                .andExpect(jsonPath("$.com_divergencia").value(0));
    }

    @Test
    void reconciliationRepairSemDivergenciasNaoReparaNada() throws Exception {
        String planId = id("plan");
        mockMvc.perform(post("/api/v1/reconciliation/" + planId + "/repair").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan_id").value(planId))
                .andExpect(jsonPath("$.records_repaired").value(0));
    }

    // ─── Distribution Plane: Data Products & Subscriptions ───

    private String criarDataProduct(String dominio) throws Exception {
        Map<String, Object> request = Map.of(
                "name", id("produto"),
                "description", "Produto de teste MockMvc",
                "domain", dominio,
                "resourceType", "ACCOUNTS",
                "format", "JSON",
                "schema", "{\"type\":\"object\"}",
                "endpoint", "/api/v1/products/teste");
        String body = mockMvc.perform(post("/api/v1/products").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("productId").asText();
    }

    @Test
    void dataProductDeveCriarListarERetornarSchema() throws Exception {
        String dominio = id("dominio");
        String productId = criarDataProduct(dominio);

        mockMvc.perform(get("/api/v1/products").with(jwt()).param("domain", dominio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(productId));

        mockMvc.perform(get("/api/v1/products/" + productId + "/schema").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"type\":\"object\"}"));
    }

    @Test
    void subscriptionDeveCriarListarRotacionarECancelar() throws Exception {
        String productId = criarDataProduct(id("dominio"));
        String participante = id("participante");

        Map<String, Object> request = Map.of(
                "participantId", participante,
                "dataProductId", productId,
                "callbackUrl", "https://participante.example.com/webhook",
                "events", "data.published.v1",
                "durationDays", 30);

        String body = mockMvc.perform(post("/api/v1/subscriptions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode criada = objectMapper.readTree(body);
        String subscriptionId = criada.get("subscriptionId").asText();

        mockMvc.perform(get("/api/v1/subscriptions").with(jwt()).param("participantId", participante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscriptionId").value(subscriptionId));

        mockMvc.perform(get("/api/v1/subscriptions").with(jwt()).param("dataProductId", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subscriptionId", hasItem(subscriptionId)));

        mockMvc.perform(get("/api/v1/subscriptions").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subscriptionId", hasItem(subscriptionId)));

        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/rotate-secret").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId));

        mockMvc.perform(delete("/api/v1/subscriptions/" + subscriptionId).with(jwt()))
                .andExpect(status().isNoContent());
    }

    @SuppressWarnings("unused")
    private static final List<String> PLANOS_COBERTOS =
            List.of("trust", "consent", "execution", "data", "distribution");
}
