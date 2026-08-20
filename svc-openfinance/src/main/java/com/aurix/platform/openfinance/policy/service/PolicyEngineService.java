package com.aurix.platform.openfinance.policy.service;

import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import com.aurix.platform.openfinance.discovery.repository.ResourceGraphRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceNodeRepository;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.dto.PolicyEvaluationRequest;
import com.aurix.platform.openfinance.policy.entity.*;
import com.aurix.platform.openfinance.policy.repository.PolicyDecisionRepository;
import com.aurix.platform.openfinance.policy.repository.PolicyRuleRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Motor de políticas — ÚNICO ponto de decisão de autorização (INV04).
 * Avalia consentimento + recurso + permissão + propósito → decisão de autorização.
 */
@Service
public class PolicyEngineService {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngineService.class);

    private final PolicyRuleRepository ruleRepository;
    private final PolicyDecisionRepository decisionRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final ResourceGraphRepository resourceGraphRepository;
    private final ResourceNodeRepository resourceNodeRepository;

    public PolicyEngineService(PolicyRuleRepository ruleRepository,
                               PolicyDecisionRepository decisionRepository,
                               ConsentimentoRepository consentimentoRepository,
                               ResourceGraphRepository resourceGraphRepository,
                               ResourceNodeRepository resourceNodeRepository) {
        this.ruleRepository = ruleRepository;
        this.decisionRepository = decisionRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.resourceGraphRepository = resourceGraphRepository;
        this.resourceNodeRepository = resourceNodeRepository;
    }

    /**
     * Avalia uma requisição de autorização contra todas as regras ativas.
     * POST /api/v1/policy/evaluate
     *
     * @param request dados da requisição (consentId, resourceId, permission, purpose)
     * @return resposta com decisão, razão e trilha de auditoria
     */
    @Transactional
    public PolicyDecisionResponse evaluate(PolicyEvaluationRequest request) {
        long startTime = System.currentTimeMillis();

        List<PolicyRule> activeRules = ruleRepository.findByActiveTrueOrderByPriorityAsc();
        List<String> evaluatedRules = new ArrayList<>();
        PolicyDecisionType finalDecision = PolicyDecisionType.ALLOWED;
        String denyReason = null;

        for (PolicyRule rule : activeRules) {
            evaluatedRules.add(rule.getRuleCode());

            boolean passed = evaluateRule(rule, request);
            if (!passed) {
                log.info("Regra {} não satisfeita para consent {}: {}", rule.getRuleCode(),
                        request.getConsentId(), rule.getRuleName());

                if (rule.getSeverity() == PolicyRuleSeverity.CRITICAL) {
                    finalDecision = PolicyDecisionType.DENIED;
                    denyReason = "Regra crítica falhou: " + rule.getRuleName();
                    break;
                }

                if (finalDecision != PolicyDecisionType.DENIED) {
                    finalDecision = PolicyDecisionType.CONDITIONAL;
                    denyReason = "Regra não crítica falhou: " + rule.getRuleName();
                }
            }
        }

        int evaluationTimeMs = (int) (System.currentTimeMillis() - startTime);

        if (finalDecision == PolicyDecisionType.ALLOWED) {
            denyReason = "Todas as regras foram satisfeitas";
        }

        PolicyDecision decision = new PolicyDecision(
                request.getConsentId(),
                request.getResourceId(),
                request.getPermission(),
                finalDecision,
                denyReason,
                evaluatedRules.toString(),
                evaluationTimeMs,
                request.getSubject()
        );
        decisionRepository.save(decision);

        log.info("Decisão de política: consent={}, recurso={}, decisão={}, tempo={}ms",
                request.getConsentId(), request.getResourceId(), finalDecision, evaluationTimeMs);

        return new PolicyDecisionResponse(
                finalDecision,
                denyReason,
                evaluatedRules,
                evaluationTimeMs,
                request.getConsentId(),
                request.getResourceId(),
                request.getPermission()
        );
    }

    @Transactional(readOnly = true)
    public List<PolicyRule> listActiveRules() {
        return ruleRepository.findByActiveTrueOrderByPriorityAsc();
    }

    @Transactional
    public PolicyRule createRule(PolicyRule rule) {
        if (ruleRepository.existsByRuleCode(rule.getRuleCode())) {
            throw new IllegalArgumentException(
                    "Código de regra já existe: " + rule.getRuleCode());
        }
        rule.setCreatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<PolicyDecision> getAuditTrail(String consentId) {
        return decisionRepository.findByConsentIdOrderByEvaluatedAtDesc(consentId);
    }

    private boolean evaluateRule(PolicyRule rule, PolicyEvaluationRequest request) {
        try {
            return switch (rule.getType()) {
                case CONSENT_VALIDATION -> validateConsent(request);
                case RESOURCE_ACCESS -> validateResourceAccess(request);
                case PURPOSE_VALIDATION -> validatePurpose(request);
                case TOKEN_VALIDATION -> validateToken(request);
                case RATE_LIMITING -> checkRateLimit(request);
                case TIME_CONSTRAINT -> checkTimeConstraint(request);
            };
        } catch (Exception e) {
            log.error("Erro ao avaliar regra {}: {}", rule.getRuleCode(), e.getMessage());
            return false;
        }
    }

    /**
     * INV01 "No consent, no execution": o consentimento precisa existir, estar
     * AUTHORISED e não expirado — não basta o campo estar preenchido.
     */
    private boolean validateConsent(PolicyEvaluationRequest request) {
        if (request.getConsentId() == null || request.getConsentId().isBlank()) {
            return false;
        }
        Optional<Consentimento> consentimento = consentimentoRepository.findByConsentId(request.getConsentId());
        if (consentimento.isEmpty()) {
            return false;
        }
        Consentimento c = consentimento.get();
        if (c.getStatus() != Consentimento.StatusConsentimento.AUTHORISED) {
            return false;
        }
        return c.getDataExpiracao() != null && c.getDataExpiracao().isAfter(LocalDateTime.now());
    }

    /**
     * INV02 "No authorized resource, no data access": o recurso pedido precisa
     * estar presente e ativo no grafo de recursos descoberto para este consentimento.
     */
    private boolean validateResourceAccess(PolicyEvaluationRequest request) {
        if (request.getResourceId() == null || request.getResourceId().isBlank()
                || request.getConsentId() == null) {
            return false;
        }
        Optional<ResourceGraph> graph = resourceGraphRepository.findLatestByConsentId(request.getConsentId());
        if (graph.isEmpty()) {
            return false;
        }
        List<ResourceNode> activeNodes = resourceNodeRepository
                .findByGraphIdAndActiveTrue(graph.get().getGraphId());
        return activeNodes.stream().anyMatch(node ->
                request.getResourceId().equals(node.getNodeId())
                        || request.getResourceId().equals(node.getPath()));
    }

    /**
     * O propósito/permissão requisitado precisa estar entre as permissões
     * efetivamente concedidas no consentimento (não apenas presente na requisição).
     */
    private boolean validatePurpose(PolicyEvaluationRequest request) {
        if (request.getPurpose() == null || request.getPurpose().isBlank()
                || request.getConsentId() == null) {
            return false;
        }
        Optional<Consentimento> consentimento = consentimentoRepository.findByConsentId(request.getConsentId());
        if (consentimento.isEmpty() || consentimento.get().getPermissions() == null) {
            return false;
        }
        List<String> granted = Arrays.asList(consentimento.get().getPermissions().split(","));
        return granted.stream().anyMatch(p -> p.trim().equalsIgnoreCase(request.getPurpose().trim()));
    }

    private boolean validateToken(PolicyEvaluationRequest request) {
        if (request.getTokenThumbprint() == null || request.getTokenThumbprint().isBlank()) {
            return false;
        }
        return true;
    }

    /**
     * RATE-002 (policies.yaml): limite por consentimento, não contador global do
     * sistema inteiro — senão um único cliente barulhento bloquearia todos os outros.
     */
    private boolean checkRateLimit(PolicyEvaluationRequest request) {
        if (request.getConsentId() == null) {
            return false;
        }
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentDecisions = decisionRepository
                .findByConsentIdOrderByEvaluatedAtDesc(request.getConsentId())
                .stream()
                .filter(d -> d.getEvaluatedAt() != null && d.getEvaluatedAt().isAfter(oneMinuteAgo))
                .count();
        return recentDecisions < 100;
    }

    private boolean checkTimeConstraint(PolicyEvaluationRequest request) {
        if (request.getConsentId() == null) {
            return false;
        }
        Optional<Consentimento> consentimento = consentimentoRepository.findByConsentId(request.getConsentId());
        return consentimento.isPresent()
                && consentimento.get().getDataExpiracao() != null
                && consentimento.get().getDataExpiracao().isAfter(LocalDateTime.now());
    }
}
