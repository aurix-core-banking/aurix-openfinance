package com.aurix.platform.openfinance.trust.service;

/**
 * Serviço de assinatura criptográfica assimétrica real (não-repúdio).
 *
 * <p>Substitui o hash disfarçado de assinatura que existia em
 * {@code AuthorizedContextService.computeSignature()} (SHA-256 rotulado como
 * ES256/RS256, sem chave privada). Este serviço usa chave assimétrica real
 * (EC P-256 → ES256 / RSA → RS256) com verificação criptográfica verificável.
 *
 * <p>Referência: issue #1 (Trust Plane / WP2) e INV05 (Authorized Context
 * imutável e assinado) do architecture.yaml.
 */
public interface SigningService {

    /**
     * Assina o payload com a chave privada, retornando o payload assinado.
     *
     * @param payload   conteúdo a ser assinado
     * @param algorithm algoritmo JOSE (ES256, RS256, ES384...)
     * @return resultado com assinatura e o algoritmo utilizado
     */
    SignedPayload sign(String payload, String algorithm);

    /**
     * Verifica se a assinatura é válida para o payload e algoritmo dados.
     *
     * @param payload   conteúdo original
     * @param signature assinatura em Base64URL
     * @param algorithm algoritmo JOSE (ES256, RS256, ES384...)
     * @return true se a assinatura confere
     */
    boolean verify(String payload, String signature, String algorithm);
}
