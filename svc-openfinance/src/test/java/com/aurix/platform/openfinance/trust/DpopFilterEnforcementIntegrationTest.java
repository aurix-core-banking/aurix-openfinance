package com.aurix.platform.openfinance.trust;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enforcement FAPI-Brazil/DPoP (RFC 9449) na cadeia HTTP com o {@code DpopFilter}
 * habilitado e obrigatório — Issue #20.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "aurix.openfinance.trust.dpop.enabled=true",
        "aurix.openfinance.trust.dpop.require=true"
})
class DpopFilterEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRejeitarEndpointProtegidoSemProvaDpop() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions").with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("x-fapi-interaction-id"));
    }

    @Test
    void deveRejeitarProvaDpopInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/policy/rules").with(jwt())
                        .header("DPoP", "nao.e.um.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoDeveExigirDpopEmRotasDeInfraestrutura() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int codigo = result.getResponse().getStatus();
                    if (codigo == 401) {
                        throw new AssertionError("Rota de infraestrutura não deve exigir DPoP");
                    }
                });
    }
}
