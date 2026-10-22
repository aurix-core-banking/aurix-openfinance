package com.aurix.platform.openfinance.trust.service;

import com.aurix.platform.openfinance.dto.DcrClientRegistrationRequest;
import com.aurix.platform.openfinance.dto.DcrClientRegistrationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DcrServiceTest {

    private DcrService dcrService;

    @BeforeEach
    void setUp() {
        dcrService = new DcrService();
        dcrService.limpar();
    }

    private String criarSsaJwt(String orgId, String softwareId, String clientName,
                               List<String> redirectUris, Long expEpochSeconds) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"PS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        StringBuilder urisJson = new StringBuilder("[");
        if (redirectUris != null) {
            for (int i = 0; i < redirectUris.size(); i++) {
                urisJson.append("\"").append(redirectUris.get(i)).append("\"");
                if (i < redirectUris.size() - 1) urisJson.append(",");
            }
        }
        urisJson.append("]");

        String expClause = (expEpochSeconds != null) ? ",\"exp\":" + expEpochSeconds : "";

        String payloadJson = String.format(
                "{\"software_id\":\"%s\",\"org_id\":\"%s\",\"software_client_name\":\"%s\",\"software_redirect_uris\":%s%s}",
                softwareId, orgId, clientName, urisJson, expClause
        );

        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = "mock_fapi_signature";

        return header + "." + payload + "." + signature;
    }

    @Test
    @DisplayName("Deve registrar cliente DCR com sucesso a partir de SSA válido")
    void deveRegistrarClienteComSucesso() {
        String ssa = criarSsaJwt(
                "org-bacen-001",
                "soft-pix-aggregator",
                "Agregador Financeiro Brasil",
                List.of("https://tpp.com.br/callback", "https://tpp.com.br/redirect"),
                Instant.now().plusSeconds(3600).getEpochSecond()
        );

        DcrClientRegistrationRequest request = new DcrClientRegistrationRequest(
                ssa,
                List.of("https://tpp.com.br/callback"),
                List.of("authorization_code", "refresh_token"),
                List.of("code"),
                "private_key_jwt",
                "PS256",
                "openid accounts consents"
        );

        DcrClientRegistrationResponse response = dcrService.registrarCliente(request);

        assertThat(response).isNotNull();
        assertThat(response.client_id()).isEqualTo("aurix-org-bacen-001-soft-pix-aggregator");
        assertThat(response.client_secret()).isNotBlank();
        assertThat(response.client_name()).isEqualTo("Agregador Financeiro Brasil");
        assertThat(response.software_id()).isEqualTo("soft-pix-aggregator");
        assertThat(response.org_id()).isEqualTo("org-bacen-001");
        assertThat(response.redirect_uris()).containsExactly("https://tpp.com.br/callback");
        assertThat(response.grant_types()).contains("authorization_code", "refresh_token");

        assertThat(dcrService.buscarPorClientId(response.client_id())).isPresent();
    }

    @Test
    @DisplayName("Deve lançar exceção se software_statement for ausente")
    void deveRejeitarRequestSemSoftwareStatement() {
        DcrClientRegistrationRequest request = new DcrClientRegistrationRequest(
                null, List.of(), List.of(), List.of(), null, null, null);

        assertThatThrownBy(() -> dcrService.registrarCliente(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("software_statement é obrigatório");
    }

    @Test
    @DisplayName("Deve lançar exceção se redirect URI solicitada não estiver autorizada no SSA")
    void deveRejeitarRedirectUriNaoAutorizada() {
        String ssa = criarSsaJwt(
                "org-001",
                "soft-001",
                "TPP",
                List.of("https://tpp.com.br/callback"),
                null
        );

        DcrClientRegistrationRequest request = new DcrClientRegistrationRequest(
                ssa,
                List.of("https://attacker.com/steal-token"),
                List.of(), List.of(), null, null, null
        );

        assertThatThrownBy(() -> dcrService.registrarCliente(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Redirect URI solicitada não autorizada no SSA");
    }

    @Test
    @DisplayName("Deve lançar exceção se SSA estiver expirado")
    void deveRejeitarSsaExpirado() {
        String ssaExpirado = criarSsaJwt(
                "org-001",
                "soft-001",
                "TPP",
                List.of("https://tpp.com.br/callback"),
                Instant.now().minusSeconds(100).getEpochSecond()
        );

        DcrClientRegistrationRequest request = new DcrClientRegistrationRequest(
                ssaExpirado, List.of(), List.of(), List.of(), null, null, null);

        assertThatThrownBy(() -> dcrService.registrarCliente(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Software Statement Assertion expirado");
    }
}
