package com.aurix.platform.openfinance.trust.verifier;

import com.aurix.platform.openfinance.trust.dto.ChannelIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

/**
 * Implementação de produção da verificação de canal com mTLS real.
 *
 * <p>Valida o certificado do cliente (repassado como PEM no header
 * {@code X-Client-Cert} quando terminado no gateway, ou em requisição direta)
 * contra a cadeia de uma CA raiz confiável configurada.
 *
 * <p>O certificado real ICP-Brasil exige credenciamento no sandbox do BACEN e
 * está fora do escopo de dev/local — aqui aceitamos uma CA autoassinada local
 * claramente documentada como não-produção (via
 * {@code aurix.openfinance.trust.mtls.trusted-ca-pem}).
 *
 * <p>Ativo apenas sob o perfil {@code prod}; em dev usa o
 * {@code GatewayHeaderTrustVerifier} (header repassado pelo gateway) — assim
 * só um bean de {@link ChannelTrustVerifier} existe por vez.
 */
@Component
@Profile("prod")
public class MutualTlsTrustVerifier implements ChannelTrustVerifier {

    private static final Logger log = LoggerFactory.getLogger(MutualTlsTrustVerifier.class);

    private final String trustedCaPem;
    private final String certificateHeader;

    public MutualTlsTrustVerifier(
            @Value("${aurix.openfinance.trust.mtls.trusted-ca-pem:}") String trustedCaPem,
            @Value("${aurix.openfinance.trust.mtls.certificate-header:X-Client-Cert}") String certificateHeader) {
        this.trustedCaPem = trustedCaPem == null ? "" : trustedCaPem;
        this.certificateHeader = certificateHeader;
    }

    @Override
    public ChannelIdentity verify(Map<String, String> headers) {
        String certPem = headerValue(headers, certificateHeader);
        if (certPem == null || certPem.isBlank()) {
            log.warn("mTLS: certificado de cliente ausente no header {}", certificateHeader);
            return null;
        }

        if (trustedCaPem.isBlank()) {
            log.warn("mTLS: CA confiável não configurada — não é possível validar a cadeia. "
                    + "Configure aurix.openfinance.trust.mtls.trusted-ca-pem.");
            return null;
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate clientCert = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));

            X509Certificate caCert = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(trustedCaPem.getBytes(StandardCharsets.UTF_8)));

            clientCert.verify(caCert.getPublicKey());

            String sha256 = sha256Fingerprint(clientCert.getEncoded());
            log.debug("mTLS valido: subject={}", clientCert.getSubjectX500Principal().getName());
            return new ChannelIdentity(
                    clientCert.getSubjectX500Principal().getName(),
                    sha256,
                    true);
        } catch (Exception e) {
            log.warn("mTLS: falha ao validar cadeia de certificados: {}", e.getMessage());
            return null;
        }
    }

    private String sha256Fingerprint(byte[] encoded) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String headerValue(Map<String, String> headers, String key) {
        if (headers == null) {
            return null;
        }
        String value = headers.get(key);
        if (value == null) {
            value = headers.get(key.toLowerCase());
        }
        return value;
    }
}
