package com.aurix.platform.openfinance.trust.filter;

import com.aurix.platform.openfinance.trust.service.TrustPlaneService;
import com.aurix.platform.openfinance.trust.verifier.ProofOfPossessionVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro Spring Security de DPoP (RFC 9449), executado antes do
 * {@code oauth2ResourceServer}.
 *
 * <p>Extrai o access token JWT (via {@link JwtAuthenticationToken} do contexto),
 * lê o header {@code DPoP}, o método e URI da requisição, e delega à
 * {@link ProofOfPossessionVerifier} a validação da prova de posse.
 *
 * <p>Quando {@code aurix.openfinance.trust.dpop.enabled=false} (dev), o filtro é
 * no-op para não bloquear testes sem DPoP.
 */
@Component
public class DpopFilter extends OncePerRequestFilter {

    public static final String FAPI_INTERACTION_ID_HEADER = "x-fapi-interaction-id";

    private final ProofOfPossessionVerifier proofOfPossessionVerifier;
    private final TrustPlaneService trustPlaneService;
    private final boolean enabled;
    private final boolean require;

    public DpopFilter(
            ProofOfPossessionVerifier proofOfPossessionVerifier,
            @Value("${aurix.openfinance.trust.dpop.enabled:true}") boolean enabled,
            @Value("${aurix.openfinance.trust.dpop.require:true}") boolean require) {
        this(proofOfPossessionVerifier, null, enabled, require);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DpopFilter(
            ProofOfPossessionVerifier proofOfPossessionVerifier,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            TrustPlaneService trustPlaneService,
            @Value("${aurix.openfinance.trust.dpop.enabled:true}") boolean enabled,
            @Value("${aurix.openfinance.trust.dpop.require:true}") boolean require) {
        this.proofOfPossessionVerifier = proofOfPossessionVerifier;
        this.trustPlaneService = trustPlaneService;
        this.enabled = enabled;
        this.require = require;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String interactionId = request.getHeader(FAPI_INTERACTION_ID_HEADER);
        if (!StringUtils.hasText(interactionId)) {
            interactionId = java.util.UUID.randomUUID().toString();
        }
        response.setHeader(FAPI_INTERACTION_ID_HEADER, interactionId);

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rotas de infra não exigem DPoP
        String path = request.getRequestURI();
        if (path.startsWith("/health") || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String dpopProof = request.getHeader("DPoP");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String accessTokenJkt = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var claims = jwtAuth.getToken().getClaims();
            if (claims.get("cnf") instanceof Map<?, ?> cnf) {
                Object jkt = cnf.get("jkt");
                if (jkt != null) {
                    accessTokenJkt = jkt.toString();
                }
            }
        }

        if (!StringUtils.hasText(dpopProof) && !require) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean verified;
        String sslVerify = request.getHeader("X-SSL-Client-Verify");
        if (trustPlaneService != null && StringUtils.hasText(sslVerify)) {
            Map<String, String> headers = new HashMap<>();
            headers.put("X-SSL-Client-Verify", sslVerify);
            String certSubject = request.getHeader("X-SSL-Client-S-DN");
            if (certSubject != null) {
                headers.put("subject", certSubject);
                headers.put("X-SSL-Client-S-DN", certSubject);
            }
            var trustResult = trustPlaneService.verify(
                    headers, request.getMethod(), request.getRequestURL().toString(), accessTokenJkt, dpopProof);
            verified = trustResult.allowed();
        } else {
            verified = proofOfPossessionVerifier.verify(dpopProof,
                    request.getMethod(), request.getRequestURL().toString(), accessTokenJkt);
        }

        if (!verified) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                    "DPoP/FAPI proof inválido: " + request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
