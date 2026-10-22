package com.aurix.platform.openfinance.trust.service;

import com.aurix.platform.openfinance.dto.DcrClientRegistrationRequest;
import com.aurix.platform.openfinance.dto.DcrClientRegistrationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de Registro Dinâmico de Clientes (Dynamic Client Registration - DCR)
 * em conformidade com a RFC 7591 e perfil FAPI 1.0 Advanced / Open Finance Brasil.
 * Valida o Software Statement Assertion (SSA) emitido pelo Diretório Central.
 */
@Service
public class DcrService {

    private static final Logger log = LoggerFactory.getLogger(DcrService.class);
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, DcrClientRegistrationResponse> clientesRegistrados = new ConcurrentHashMap<>();

    public DcrService() {
        this(new ObjectMapper());
    }

    public DcrService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Registra um novo cliente TPP no gateway a partir do Software Statement Assertion (SSA).
     *
     * @param request dados da solicitação DCR incluindo o JWT de SSA
     * @return credenciais emitidas e metadados do cliente registrado
     */
    public DcrClientRegistrationResponse registrarCliente(DcrClientRegistrationRequest request) {
        if (request == null || request.software_statement() == null || request.software_statement().isBlank()) {
            throw new IllegalArgumentException("software_statement é obrigatório para registro DCR segundo o perfil FAPI-BR");
        }

        JsonNode ssaClaims = decodificarEValidarSsa(request.software_statement());

        String softwareId = obterCampoObrigatorio(ssaClaims, "software_id");
        String orgId = obterCampoObrigatorio(ssaClaims, "org_id");
        String clientName = obterCampoOpcional(ssaClaims, "software_client_name", "Open Finance Client");
        String jwksUri = obterCampoOpcional(ssaClaims, "software_jwks_uri",
                obterCampoOpcional(ssaClaims, "jwks_uri", ""));

        List<String> ssaRedirectUris = extrairLista(ssaClaims, "software_redirect_uris");
        List<String> requestRedirectUris = (request.redirect_uris() != null && !request.redirect_uris().isEmpty())
                ? request.redirect_uris()
                : ssaRedirectUris;

        validarRedirectUris(requestRedirectUris, ssaRedirectUris);

        String clientId = String.format("aurix-%s-%s", orgId, softwareId);
        String clientSecret = gerarSecretSeguro();
        long agora = Instant.now().getEpochSecond();

        List<String> grantTypes = (request.grant_types() != null && !request.grant_types().isEmpty())
                ? request.grant_types()
                : List.of("authorization_code", "client_credentials", "refresh_token");

        List<String> responseTypes = (request.response_types() != null && !request.response_types().isEmpty())
                ? request.response_types()
                : List.of("code id_token");

        String authMethod = (request.token_endpoint_auth_method() != null && !request.token_endpoint_auth_method().isBlank())
                ? request.token_endpoint_auth_method()
                : "private_key_jwt";

        String signingAlg = (request.token_endpoint_auth_signing_alg() != null && !request.token_endpoint_auth_signing_alg().isBlank())
                ? request.token_endpoint_auth_signing_alg()
                : "PS256";

        String scope = (request.scope() != null && !request.scope().isBlank())
                ? request.scope()
                : "openid consent accounts payments";

        DcrClientRegistrationResponse response = new DcrClientRegistrationResponse(
                clientId,
                clientSecret,
                agora,
                0L,
                clientName,
                requestRedirectUris,
                grantTypes,
                responseTypes,
                authMethod,
                signingAlg,
                scope,
                softwareId,
                orgId,
                jwksUri
        );

        clientesRegistrados.put(clientId, response);
        log.info("Cliente DCR registrado com sucesso: clientId={}, orgId={}, softwareId={}", clientId, orgId, softwareId);
        return response;
    }

    public Optional<DcrClientRegistrationResponse> buscarPorClientId(String clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(clientesRegistrados.get(clientId));
    }

    public int totalClientesRegistrados() {
        return clientesRegistrados.size();
    }

    public void limpar() {
        clientesRegistrados.clear();
    }

    private JsonNode decodificarEValidarSsa(String ssaJwt) {
        String[] partes = ssaJwt.split("\\.");
        if (partes.length < 2) {
            throw new IllegalArgumentException("Formato inválido de SSA: JWT deve conter pelo menos header e payload");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(partes[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payloadJson);

            if (claims.has("exp")) {
                long exp = claims.get("exp").asLong();
                if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                    throw new IllegalArgumentException("Software Statement Assertion expirado no Diretório Central");
                }
            }

            return claims;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao decodificar payload JSON do SSA: " + e.getMessage(), e);
        }
    }

    private String obterCampoObrigatorio(JsonNode json, String campo) {
        if (!json.has(campo) || json.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente no SSA: " + campo);
        }
        return json.get(campo).asText().trim();
    }

    private String obterCampoOpcional(JsonNode json, String campo, String valorPadrao) {
        if (json.has(campo) && !json.get(campo).asText().isBlank()) {
            return json.get(campo).asText().trim();
        }
        return valorPadrao;
    }

    private List<String> extrairLista(JsonNode json, String campo) {
        if (!json.has(campo) || !json.get(campo).isArray()) {
            return Collections.emptyList();
        }
        List<String> lista = new ArrayList<>();
        json.get(campo).forEach(node -> lista.add(node.asText()));
        return Collections.unmodifiableList(lista);
    }

    private void validarRedirectUris(List<String> solicitadas, List<String> autorizadasNoSsa) {
        if (autorizadasNoSsa.isEmpty()) {
            return;
        }
        for (String uri : solicitadas) {
            if (!autorizadasNoSsa.contains(uri)) {
                throw new IllegalArgumentException("Redirect URI solicitada não autorizada no SSA: " + uri);
            }
        }
    }

    private String gerarSecretSeguro() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
