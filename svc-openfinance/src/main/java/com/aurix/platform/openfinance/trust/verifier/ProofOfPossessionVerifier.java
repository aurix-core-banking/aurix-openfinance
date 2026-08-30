package com.aurix.platform.openfinance.trust.verifier;

/**
 * Porta de verificação de prova de posse (DPoP — RFC 9449).
 *
 * <p>Valida o DPoP proof contra o thumbprint {@code cnf.jkt} do access token
 * JWT. É concern app-level — não pode ir para o gateway.
 */
public interface ProofOfPossessionVerifier {

    /**
     * Verifica o DPoP proof para uma requisição.
     *
     * @param dpopProof header DPoP do JWT
     * @param httpMethod método HTTP (GET, POST, PUT...)
     * @param requestUri URI da requisição (htu)
     * @param accessTokenJkt thumbprint {@code cnf.jkt} extraído do access token
     * @return true se o proof é válido e corresponde ao token
     */
    boolean verify(String dpopProof, String httpMethod, String requestUri, String accessTokenJkt);
}
