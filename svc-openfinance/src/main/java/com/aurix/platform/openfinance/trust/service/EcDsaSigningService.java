package com.aurix.platform.openfinance.trust.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação real de {@link SigningService} usando criptografia assimétrica.
 *
 * <p>Gera pares de chaves em memória (dev/local). Em produção a chave deve ser
 * gerenciada via Vault ({@code spring.cloud.vault.*}) — quando a chave for
 * injetada de fora deste serviço, este componente é trocado por uma impl que lê
 * do Vault. A interface permanece a mesma.
 *
 * <p>Algoritmos suportados:
 * <ul>
 *   <li>{@code ES256} → EC P-256 + SHA-256withECDSA (padrão FAPI-Brasil)</li>
 *   <li>{@code ES384} → EC P-384 + SHA-384withECDSA</li>
 *   <li>{@code RS256} → RSA 2048 + SHA256withRSA</li>
 * </ul>
 */
@Service
public class EcDsaSigningService implements SigningService {

    private static final Logger log = LoggerFactory.getLogger(EcDsaSigningService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final java.util.Set<String> SUPPORTED =
            java.util.Set.of("ES256", "ES384", "RS256");

    private final Map<String, KeyPair> keyPairs = new ConcurrentHashMap<>();

    public EcDsaSigningService() {
        // Pré-gera os pares mais comuns para acelerar o primeiro uso
        ensureKeyPair("ES256");
        ensureKeyPair("ES384");
        ensureKeyPair("RS256");
    }

    @Override
    public SignedPayload sign(String payload, String algorithm) {
        String normalized = normalize(algorithm);
        if (!SUPPORTED.contains(normalized)) {
            throw new IllegalArgumentException("Algoritmo não suportado: " + normalized);
        }
        KeyPair keyPair = ensureKeyPair(normalized);
        try {
            Signature signer = signatureFor(normalized);
            signer.initSign(keyPair.getPrivate());
            signer.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signer.sign();
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            return new SignedPayload(payload, signature, normalized, LocalDateTime.now());
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao assinar com algoritmo " + normalized, e);
        }
    }

    @Override
    public boolean verify(String payload, String signature, String algorithm) {
        String normalized = normalize(algorithm);
        try {
            KeyPair keyPair = ensureKeyPair(normalized);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signature);

            Signature verifier = signatureFor(normalized);
            verifier.initVerify(keyPair.getPublic());
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            log.warn("Falha ao verificar assinatura: {}", e.getMessage());
            return false;
        }
    }

    /** Retorna a chave pública (Base64URL X.509) — útil para ferramentas de debug. */
    public String getPublicKeyB64Url(String algorithm) {
        KeyPair keyPair = ensureKeyPair(normalize(algorithm));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(keyPair.getPublic().getEncoded());
    }

    private KeyPair ensureKeyPair(String algorithm) {
        return keyPairs.computeIfAbsent(algorithm, this::generateKeyPair);
    }

    private KeyPair generateKeyPair(String algorithm) {
        try {
            switch (algorithm) {
                case "ES256" -> {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                    generator.initialize(new ECGenParameterSpec("secp256r1"));
                    return generator.generateKeyPair();
                }
                case "ES384" -> {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                    generator.initialize(new ECGenParameterSpec("secp384r1"));
                    return generator.generateKeyPair();
                }
                case "RS256" -> {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
                    return generator.generateKeyPair();
                }
                default -> throw new IllegalArgumentException("Algoritmo não suportado: " + algorithm);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar par de chaves para " + algorithm, e);
        }
    }

    private Signature signatureFor(String algorithm) throws Exception {
        return switch (algorithm) {
            case "ES256" -> Signature.getInstance("SHA256withECDSA");
            case "ES384" -> Signature.getInstance("SHA384withECDSA");
            case "RS256" -> Signature.getInstance("SHA256withRSA");
            default -> throw new IllegalArgumentException("Algoritmo não suportado: " + algorithm);
        };
    }

    private String normalize(String algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm não pode ser nulo");
        }
        return algorithm.trim().toUpperCase();
    }
}
