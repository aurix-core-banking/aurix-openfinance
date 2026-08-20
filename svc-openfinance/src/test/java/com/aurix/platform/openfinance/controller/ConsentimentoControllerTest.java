package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsentimentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveCriarConsentimento() throws Exception {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId("client-abc-123");
        request.setInstitutionCode("AURIX");
        request.setPermissions(java.util.List.of("accounts", "transactions"));

        mockMvc.perform(post("/open-finance/v1/consents")
                .with(jwt())
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consentId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("AWAITING_AUTHORISATION"))
            .andExpect(jsonPath("$.clientId").value("client-abc-123"));
    }

    @Test
    void deveRejeitarRequestSemClientId() throws Exception {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setInstitutionCode("AURIX");

        mockMvc.perform(post("/open-finance/v1/consents")
                .with(jwt())
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveBuscarConsentimento() throws Exception {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId("client-test");
        request.setInstitutionCode("AURIX");

        String responseBody = mockMvc.perform(post("/open-finance/v1/consents")
                .with(jwt())
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();

        String consentId = objectMapper.readTree(responseBody).get("consentId").asText();

        mockMvc.perform(get("/open-finance/v1/consents/" + consentId).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentId").value(consentId))
            .andExpect(jsonPath("$.status").value("AWAITING_AUTHORISATION"));
    }

    @Test
    void deveAprovarConsentimento() throws Exception {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId("client-approve");
        request.setInstitutionCode("AURIX");

        String responseBody = mockMvc.perform(post("/open-finance/v1/consents")
                .with(jwt())
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();

        String consentId = objectMapper.readTree(responseBody).get("consentId").asText();

        mockMvc.perform(post("/open-finance/v1/consents/" + consentId + "/authorise").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AUTHORISED"));
    }

    @Test
    void deveRevogarConsentimento() throws Exception {
        ConsentimentoRequest request = new ConsentimentoRequest();
        request.setClientId("client-revoke");
        request.setInstitutionCode("AURIX");

        String responseBody = mockMvc.perform(post("/open-finance/v1/consents")
                .with(jwt())
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();

        String consentId = objectMapper.readTree(responseBody).get("consentId").asText();

        mockMvc.perform(post("/open-finance/v1/consents/" + consentId + "/revoke")
                .with(jwt())
                .param("motivo", "Cliente solicitou"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void deveRetornar404ParaConsentimentoInexistente() throws Exception {
        mockMvc.perform(get("/open-finance/v1/consents/inexistente-123").with(jwt()))
            .andExpect(status().isBadRequest());
    }
}
