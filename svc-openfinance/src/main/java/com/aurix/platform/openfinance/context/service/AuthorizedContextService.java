package com.aurix.platform.openfinance.context.service;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de Contexto Autorizado — contexto imutável (INV05).
 * Cria, valida e revoga contextos baseados em decisões de política.
 */
@Service
public class AuthorizedContextService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizedContextService.class);

    private final AuthorizedContextRepository contextRepository;

    public AuthorizedContextService(AuthorizedContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    /**
     * Cria um contexto autorizado a partir de uma decisão de política.
     * O contexto é imutável — não pode ser modificado após criação.
     */
    @Transactional
    public AuthorizedContext create(PolicyDecisionResponse policyDecision, String subject,
                                   String purpose, String permissions, String resourceGraph,
                                   String signingAlgorithm, String dpopThumbprint) {
        if (policyDecision.getDecision() != PolicyDecisionType.ALLOWED) {
            throw new IllegalStateException(
                    "Não é possível criar contexto para decisão: " + policyDecision.getDecision());
        }

        String contextId = UUID.randomUUID().toString();
        LocalDateTime validUntil = LocalDateTime.now().plusHours(1);

        String signatureData = contextId + subject + policyDecision.getConsentId()
                + purpose + validUntil + dpopThumbprint;
        String signature = computeSignature(signatureData, signingAlgorithm);

        AuthorizedContext context = new AuthorizedContext(
                contextId,
                subject,
                policyDecision.getConsentId(),
                1,
                purpose,
                permissions,
                resourceGraph,
                validUntil,
                signingAlgorithm,
                dpopThumbprint,
                signature
        );

        AuthorizedContext saved = contextRepository.save(context);
        log.info("Contexto autorizado criado: contextId={}, subject={}, consent={}",
                contextId, subject, policyDecision.getConsentId());

        return saved;
    }

    /**
     * Valida se um contexto está ativo, não expirado e não revogado.
     */
    @Transactional(readOnly = true)
    public AuthorizedContext validate(String contextId) {
        AuthorizedContext context = contextRepository.findByContextId(contextId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contexto não encontrado: " + contextId));

        if (context.isRevoked()) {
            throw new IllegalStateException("Contexto revogado: " + contextId);
        }

        if (context.isExpired()) {
            throw new IllegalStateException("Contexto expirado: " + contextId);
        }

        return context;
    }

    /**
     * Revoga um contexto — nunca deleta, apenas marca como revogado.
     */
    @Transactional
    public AuthorizedContext revoke(String contextId) {
        AuthorizedContext context = contextRepository.findByContextId(contextId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contexto não encontrado: " + contextId));

        if (!context.isRevoked()) {
            context.revoke();
            AuthorizedContext saved = contextRepository.save(context);
            log.info("Contexto revogado: contextId={}", contextId);
            return saved;
        }

        log.warn("Tentativa de revogar contexto já revogado: contextId={}", contextId);
        return context;
    }

    /**
     * Lista contextos ativos para um consentimento.
     */
    @Transactional(readOnly = true)
    public List<AuthorizedContext> listActiveByConsentId(String consentId) {
        return contextRepository.findActiveContexts(consentId);
    }

    /**
     * Obtém contexto por ID.
     */
    @Transactional(readOnly = true)
    public AuthorizedContext getByContextId(String contextId) {
        return contextRepository.findByContextId(contextId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contexto não encontrado: " + contextId));
    }

    private String computeSignature(String data, String algorithm) {
        try {
            MessageDigest digest;
            switch (algorithm) {
                case "ES256", "RS256" -> digest = MessageDigest.getInstance("SHA-256");
                default -> digest = MessageDigest.getInstance("SHA-256");
            }
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de assinatura não disponível: " + algorithm, e);
        }
    }
}
