package com.aurix.platform.openfinance.dto;

import java.util.List;

/**
 * Requisição de Registro Dinâmico de Cliente (Dynamic Client Registration - RFC 7591 / FAPI-BR).
 */
public record DcrClientRegistrationRequest(
        String software_statement,
        List<String> redirect_uris,
        List<String> grant_types,
        List<String> response_types,
        String token_endpoint_auth_method,
        String token_endpoint_auth_signing_alg,
        String scope
) {
}
