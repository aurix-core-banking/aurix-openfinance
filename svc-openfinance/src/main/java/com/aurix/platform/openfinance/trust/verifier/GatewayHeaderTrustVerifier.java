package com.aurix.platform.openfinance.trust.verifier;

import com.aurix.platform.openfinance.trust.dto.ChannelIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Implementação de desenvolvimento (perfil default/dev) da verificação de canal.
 *
 * <p>Não termina mTLS localmente — confia em cabeçalhos repassados pelo gateway
 * (Traefik), que é o padrão de mercado para FAPI-Brasil. Em produção real, o
 * mTLS é terminado no gateway e o subject/impressão do certificado chega num
 * header confiável repassado por ele.
 *
 * <p>Em dev/local onde não há gateway, esse verifier aceita identidades
 * configuradas explicitamente para permitir testes sem certificado.
 *
 * <p>Ativa por default; a implementação de produção ({@code MutualTlsTrustVerifier})
 * assume quando o perfil {@code prod} está ativo — assim só um bean de
 * {@link ChannelTrustVerifier} existe por vez.
 */
@Component
@Profile("!prod")
public class GatewayHeaderTrustVerifier implements ChannelTrustVerifier {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderTrustVerifier.class);
    private static final String DEFAULT_SUBJECT_HEADER = "X-Client-Cert-Subject";
    private static final String DEFAULT_FINGERPRINT_HEADER = "X-Client-Cert-Sha256";

    private final String subjectHeader;
    private final String fingerprintHeader;
    private final boolean strict;

    public GatewayHeaderTrustVerifier(
            @Value("${aurix.openfinance.trust.mtls.subject-header:X-Client-Cert-Subject}") String subjectHeader,
            @Value("${aurix.openfinance.trust.mtls.fingerprint-header:X-Client-Cert-Sha256}") String fingerprintHeader,
            @Value("${aurix.openfinance.trust.mtls.strict:false}") boolean strict) {
        this.subjectHeader = subjectHeader;
        this.fingerprintHeader = fingerprintHeader;
        this.strict = strict;
    }

    @Override
    public ChannelIdentity verify(Map<String, String> headers) {
        String subject = headerValue(headers, subjectHeader);
        if (subject == null || subject.isBlank()) {
            if (strict) {
                log.warn("mTLS estrito: certificado de cliente ausente");
                return null;
            }
            // Dev sem gateway: identidade não verificada
            log.warn("Identidade de canal não verificada (sem header {}). strict={}",
                    subjectHeader, strict);
            return null;
        }

        String fingerprint = headerValue(headers, fingerprintHeader);
        log.debug("Canal verificado via gateway: subject={}", subject);
        return new ChannelIdentity(subject, fingerprint, true);
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
