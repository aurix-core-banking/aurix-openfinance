package com.aurix.platform.openfinance.trust.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Verificador de DPoP (RFC 9449) — prova de posse por requisição.
 *
 * <p>Valida o JWT de DPoP (header {@code DPoP}) contra o thumbprint
 * {@code cnf.jkt} do access token. A verificação cobre:
 * <ul>
 *   <li>presença e decodificação do proof JWT;</li>
 *   <li>método HTTP ({@code htm}) e URI da requisição ({@code htu});</li>
 *   <li>prova criptográfica da assinatura do proof;</li>
 *   <li>correspondência do thumbprint da chave pública do proof com o
 *       {@code cnf.jkt} do access token (RFC 9449 §4.2).</li>
 * </ul>
 *
 * <p>Nota: em produção a cadeia de verificação termina na validade do JWT do
 * access token junto ao AS (Keycloak) via <code>jwk</code> thumbprint. Aqui
 * validamos a coerência criptográfica proof↔token no escopo deste serviço.
 */
@Component
public class DpopProofVerifier implements ProofOfPossessionVerifier {

    private static final Logger log = LoggerFactory.getLogger(DpopProofVerifier.class);
    private static final Base64.Decoder B64 = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean verify(String dpopProof, String httpMethod, String requestUri, String accessTokenJkt) {
        if (!StringUtils.hasText(dpopProof)) {
            log.warn("DPoP: proof ausente");
            return false;
        }
        if (!StringUtils.hasText(accessTokenJkt)) {
            // Sem cnf.jkt no token, não há como amarrar o proof ao token
            log.warn("DPoP: access token sem cnf.jkt — proof não vinculado a token");
            return false;
        }

        try {
            String[] parts = dpopProof.split("\\.");
            if (parts.length != 3) {
                log.warn("DPoP: proof não é um JWT válido");
                return false;
            }
            String headerJson = new String(B64.decode(parts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(B64.decode(parts[1]), StandardCharsets.UTF_8);

            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode payload = objectMapper.readTree(payloadJson);

            if (!"dpop+jwt".equals(header.path("typ").asText())) {
                log.warn("DPoP: typ inválido");
                return false;
            }

            String htm = payload.path("htm").asText(null);
            String htu = payload.path("htu").asText(null);
            if (!httpMethod.equalsIgnoreCase(htm)) {
                log.warn("DPoP: htm {} != método {}", htm, httpMethod);
                return false;
            }
            if (htu == null || requestUri == null || !requestUri.startsWith(htu)) {
                log.warn("DPoP: htu {} não corresponde à URI {}", htu, requestUri);
                return false;
            }

            String jwkAlg = header.path("alg").asText("");
            String jkt = computeJkt(header.path("jwk"));
            if (jkt == null) {
                log.warn("DPoP: jwk ausente ou não suportado no header (alg={})", jwkAlg);
                return false;
            }

            if (!jkt.equals(accessTokenJkt)) {
                log.warn("DPoP: jkt {} != cnf.jkt {}", jkt, accessTokenJkt);
                return false;
            }

            // Valida assinatura criptográfica do proof na ação 4.3 do RFC 9449
            boolean verified = verifyProofSignature(header, parts[0] + "." + parts[1], parts[2]);
            if (!verified) {
                log.warn("DPoP: assinatura do proof inválida");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("DPoP: falha ao verificar proof: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calcula o thumbprint (RFC 7638) de uma JWK.
     */
    private String computeJkt(JsonNode jwk) {
        try {
            if (jwk == null || jwk.isMissingNode()) {
                return null;
            }
            String kty = jwk.path("kty").asText("");
            String canonical;
            if ("EC".equals(kty)) {
                canonical = "{\"crv\":\"" + jwk.path("crv").asText()
                        + "\",\"kty\":\"EC\",\"x\":\"" + jwk.path("x").asText()
                        + "\",\"y\":\"" + jwk.path("y").asText() + "\"}";
            } else if ("RSA".equals(kty)) {
                canonical = "{\"e\":\"" + jwk.path("e").asText()
                        + "\",\"kty\":\"RSA\",\"n\":\"" + jwk.path("n").asText() + "\"}";
            } else {
                return null;
            }
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            log.warn("DPoP: falha ao calcular jkt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Valida a assinatura do DPoP proof JWT usando a chave pública do jwk.
     */
    private boolean verifyProofSignature(JsonNode header, String signingInput, String signatureB64)
            throws Exception {
        JsonNode jwk = header.path("jwk");
        String kty = jwk.path("kty").asText("");
        String alg = header.path("alg").asText("");
        byte[] signature = B64.decode(signatureB64);

        if ("EC".equals(kty)) {
            ECPublicKey publicKey = buildEcPublicKey(jwk);
            java.security.Signature verifier = switch (alg) {
                case "ES256" -> java.security.Signature.getInstance("SHA256withECDSA");
                case "ES384" -> java.security.Signature.getInstance("SHA384withECDSA");
                default -> null;
            };
            if (verifier == null) {
                return false;
            }
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(convertJoseDsaToDer(signature, alg));
        } else if ("RSA".equals(kty)) {
            RSAPublicKey publicKey = buildRsaPublicKey(jwk);
            java.security.Signature verifier = "RS256".equals(alg)
                    ? java.security.Signature.getInstance("SHA256withRSA") : null;
            if (verifier == null) {
                return false;
            }
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        }
        return false;
    }

    private ECPublicKey buildEcPublicKey(JsonNode jwk) throws Exception {
        String curve;
        switch (jwk.path("crv").asText()) {
            case "P-256" -> curve = "secp256r1";
            case "P-384" -> curve = "secp384r1";
            default -> curve = "secp256r1";
        }
        java.security.AlgorithmParameters params =
                java.security.AlgorithmParameters.getInstance("EC");
        params.init(new java.security.spec.ECGenParameterSpec(curve));
        java.security.spec.ECParameterSpec spec =
                params.getParameterSpec(java.security.spec.ECParameterSpec.class);

        byte[] xb = B64.decode(jwk.path("x").asText());
        byte[] yb = B64.decode(jwk.path("y").asText());
        java.security.spec.ECPoint point = new java.security.spec.ECPoint(
                new java.math.BigInteger(1, xb), new java.math.BigInteger(1, yb));
        var keySpec = new java.security.spec.ECPublicKeySpec(point, spec);
        return (ECPublicKey) java.security.KeyFactory.getInstance("EC").generatePublic(keySpec);
    }

    private RSAPublicKey buildRsaPublicKey(JsonNode jwk) throws Exception {
        byte[] nb = B64.decode(jwk.path("n").asText());
        byte[] eb = B64.decode(jwk.path("e").asText());
        var keySpec = new java.security.spec.RSAPublicKeySpec(
                new java.math.BigInteger(1, nb), new java.math.BigInteger(1, eb));
        return (RSAPublicKey) java.security.KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    /**
     * Converte assinatura ECDSA JOSE (r||s) para DER (necessário na JVM Java 8-17)
     * e vice-versa. Aqui apenas r||s → DER.
     */
    private byte[] convertJoseDsaToDer(byte[] joseSig, String alg) {
        int keySize = alg.endsWith("384") ? 48 : 32;
        byte[] r = new byte[keySize];
        byte[] s = new byte[keySize];
        System.arraycopy(joseSig, 0, r, 0, keySize);
        System.arraycopy(joseSig, keySize, s, 0, keySize);

        byte[] rEnc = toUnsigned(r);
        byte[] sEnc = toUnsigned(s);

        var seq = new java.io.ByteArrayOutputStream();
        writeDerLength(seq, rEnc.length + sEnc.length + 6);
        seq.write(0x02);
        writeDerLength(seq, rEnc.length);
        seq.write(rEnc, 0, rEnc.length);
        seq.write(0x02);
        writeDerLength(seq, sEnc.length);
        seq.write(sEnc, 0, sEnc.length);
        return seq.toByteArray();
    }

    private byte[] toUnsigned(byte[] value) {
        if (value[0] >= 0) {
            return value;
        }
        byte[] result = new byte[value.length + 1];
        result[0] = 0;
        System.arraycopy(value, 0, result, 1, value.length);
        return result;
    }

    private void writeDerLength(java.io.ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else {
            out.write(0x81);
            out.write(length);
        }
    }
}
