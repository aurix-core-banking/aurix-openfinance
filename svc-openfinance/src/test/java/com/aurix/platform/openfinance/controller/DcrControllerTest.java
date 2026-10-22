package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.DcrClientRegistrationRequest;
import com.aurix.platform.openfinance.dto.DcrClientRegistrationResponse;
import com.aurix.platform.openfinance.trust.service.DcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DcrControllerTest {

    private DcrService dcrService;
    private DcrController controller;

    @BeforeEach
    void setUp() {
        dcrService = new DcrService();
        dcrService.limpar();
        controller = new DcrController(dcrService);
    }

    private String criarSsaValido() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"PS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"software_id\":\"soft-abc\",\"org_id\":\"org-xyz\",\"software_client_name\":\"Fintech App\","
                        + "\"software_redirect_uris\":[\"https://app.com/cb\"]}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    @Test
    @DisplayName("POST /open-finance/v1/dcr/register deve retornar 201 Created com credenciais do cliente")
    void deveRegistrarClienteRetornando201() {
        DcrClientRegistrationRequest req = new DcrClientRegistrationRequest(
                criarSsaValido(),
                List.of("https://app.com/cb"),
                List.of("authorization_code"),
                List.of("code"),
                "private_key_jwt",
                "PS256",
                "openid accounts"
        );

        ResponseEntity<DcrClientRegistrationResponse> resp = controller.registrarCliente(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DcrClientRegistrationResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.client_id()).isEqualTo("aurix-org-xyz-soft-abc");
        assertThat(body.client_name()).isEqualTo("Fintech App");
    }

    @Test
    @DisplayName("GET /open-finance/v1/dcr/{clientId} deve retornar 200 OK quando existente e 404 quando inexistente")
    void deveConsultarClienteRegistrado() {
        DcrClientRegistrationRequest req = new DcrClientRegistrationRequest(
                criarSsaValido(), List.of("https://app.com/cb"), null, null, null, null, null);
        controller.registrarCliente(req);

        ResponseEntity<DcrClientRegistrationResponse> respSucesso = controller.consultarCliente("aurix-org-xyz-soft-abc");
        assertThat(respSucesso.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respSucesso.getBody()).isNotNull();

        ResponseEntity<DcrClientRegistrationResponse> respNaoEncontrado = controller.consultarCliente("cliente-fantasma");
        assertThat(respNaoEncontrado.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
