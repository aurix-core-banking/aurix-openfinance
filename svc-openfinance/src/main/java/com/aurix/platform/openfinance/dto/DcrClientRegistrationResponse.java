package com.aurix.platform.openfinance.dto;

import java.util.List;

/**
 * Resposta de Registro Dinâmico de Cliente (Dynamic Client Registration - RFC 7591 / FAPI-BR).
 */
public record DcrClientRegistrationResponse(
        String client_id,
        String client_secret,
        long client_id_issued_at,
        long client_secret_expires_at,
        String client_name,
        List<String> redirect_uris,
        List<String> grant_types,
        List<String> response_types,
        String token_endpoint_auth_method,
        String token_endpoint_auth_signing_alg,
        String scope,
        String software_id,
        String org_id,
        String jwks_uri
) {
}
