package com.aurix.platform.openfinance.policy.service;

import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import com.aurix.platform.openfinance.discovery.repository.ResourceGraphRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceNodeRepository;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.dto.PolicyEvaluationRequest;
import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import com.aurix.platform.openfinance.policy.entity.PolicyRule;
import com.aurix.platform.openfinance.policy.entity.PolicyRuleSeverity;
import com.aurix.platform.openfinance.policy.entity.PolicyRuleType;
import com.aurix.platform.openfinance.policy.repository.PolicyRuleRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prova que o PolicyEngineService avalia consentimento/recurso/propósito de
 * verdade (INV01/INV02) em vez de só checar campos não-vazios.
 */
@SpringBootTest
@ActiveProfiles("test")
class PolicyEngineServiceTest {

    @Autowired
    private PolicyEngineService policyEngineService;

    @Autowired
    private PolicyRuleRepository ruleRepository;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @Autowired
    private ResourceGraphRepository resourceGraphRepository;

    @Autowired
    private ResourceNodeRepository resourceNodeRepository;

    @BeforeEach
    void setUp() {
        resourceNodeRepository.deleteAll();
        resourceGraphRepository.deleteAll();
        consentimentoRepository.deleteAll();
        ruleRepository.deleteAll();

        ruleRepository.save(regra("CONSENT_001", "Validação de Consentimento Ativo",
                PolicyRuleType.CONSENT_VALIDATION, PolicyRuleSeverity.CRITICAL,
                "consent.status == AUTHORISED", 1));
        ruleRepository.save(regra("RESOURCE_001", "Validação de Acesso ao Recurso",
                PolicyRuleType.RESOURCE_ACCESS, PolicyRuleSeverity.CRITICAL,
                "resource in authorized_graph", 2));
        ruleRepository.save(regra("PURPOSE_001", "Validação de Propósito",
                PolicyRuleType.PURPOSE_VALIDATION, PolicyRuleSeverity.HIGH,
                "purpose in consent.permissions", 3));
    }

    private PolicyRule regra(String code, String name, PolicyRuleType type,
                              PolicyRuleSeverity severity, String expression, int priority) {
        PolicyRule rule = new PolicyRule(code, name, type, severity, expression, priority);
        rule.setDescription(name);
        return rule;
    }

    private Consentimento criarConsentimento(String consentId, Consentimento.StatusConsentimento status,
                                              LocalDateTime expiracao) {
        Consentimento c = new Consentimento();
        c.setConsentId(consentId);
        c.setClientId("client-teste");
        c.setUserId(1L);
        c.setStatus(status);
        c.setPermissions("accounts,transactions");
        c.setDataCriacao(LocalDateTime.now());
        c.setDataExpiracao(expiracao);
        return consentimentoRepository.save(c);
    }

    private void criarGrafoComRecurso(String consentId, String graphId, String resourceId) {
        ResourceGraph graph = new ResourceGraph(graphId, consentId, 1);
        resourceGraphRepository.save(graph);

        ResourceNode node = new ResourceNode(resourceId, graphId, "ACCOUNTS",
                "/accounts", "[\"READ\"]", "[]", "{}");
        resourceNodeRepository.save(node);
    }

    @Test
    void devePermitirQuandoConsentimentoAtivoRecursoAutorizadoEPropositoConcedido() {
        criarConsentimento("consent-ok", Consentimento.StatusConsentimento.AUTHORISED,
                LocalDateTime.now().plusDays(30));
        criarGrafoComRecurso("consent-ok", "graph-ok", "resource-accounts");

        PolicyEvaluationRequest request = new PolicyEvaluationRequest(
                "consent-ok", "resource-accounts", "READ", "accounts",
                "subject-1", "thumb-1", null);

        PolicyDecisionResponse response = policyEngineService.evaluate(request);

        assertEquals(PolicyDecisionType.ALLOWED, response.getDecision());
    }

    @Test
    void deveNegarQuandoConsentimentoRevogado() {
        criarConsentimento("consent-revoked", Consentimento.StatusConsentimento.REVOKED,
                LocalDateTime.now().plusDays(30));
        criarGrafoComRecurso("consent-revoked", "graph-revoked", "resource-accounts");

        PolicyEvaluationRequest request = new PolicyEvaluationRequest(
                "consent-revoked", "resource-accounts", "READ", "accounts",
                "subject-1", "thumb-1", null);

        PolicyDecisionResponse response = policyEngineService.evaluate(request);

        assertEquals(PolicyDecisionType.DENIED, response.getDecision());
    }

    @Test
    void deveNegarQuandoConsentimentoExpirado() {
        criarConsentimento("consent-expired", Consentimento.StatusConsentimento.AUTHORISED,
                LocalDateTime.now().minusDays(1));
        criarGrafoComRecurso("consent-expired", "graph-expired", "resource-accounts");

        PolicyEvaluationRequest request = new PolicyEvaluationRequest(
                "consent-expired", "resource-accounts", "READ", "accounts",
                "subject-1", "thumb-1", null);

        PolicyDecisionResponse response = policyEngineService.evaluate(request);

        assertEquals(PolicyDecisionType.DENIED, response.getDecision());
    }

    @Test
    void deveNegarQuandoRecursoNaoEstaNoGrafoAutorizado() {
        criarConsentimento("consent-wrong-resource", Consentimento.StatusConsentimento.AUTHORISED,
                LocalDateTime.now().plusDays(30));
        criarGrafoComRecurso("consent-wrong-resource", "graph-wr", "resource-accounts");

        PolicyEvaluationRequest request = new PolicyEvaluationRequest(
                "consent-wrong-resource", "resource-nao-autorizado", "READ", "accounts",
                "subject-1", "thumb-1", null);

        PolicyDecisionResponse response = policyEngineService.evaluate(request);

        assertEquals(PolicyDecisionType.DENIED, response.getDecision());
    }

    @Test
    void deveNegarQuandoPropositoNaoFoiConcedidoNoConsentimento() {
        criarConsentimento("consent-wrong-purpose", Consentimento.StatusConsentimento.AUTHORISED,
                LocalDateTime.now().plusDays(30));
        criarGrafoComRecurso("consent-wrong-purpose", "graph-wp", "resource-accounts");

        PolicyEvaluationRequest request = new PolicyEvaluationRequest(
                "consent-wrong-purpose", "resource-accounts", "READ", "credit-cards",
                "subject-1", "thumb-1", null);

        PolicyDecisionResponse response = policyEngineService.evaluate(request);

        assertEquals(PolicyDecisionType.CONDITIONAL, response.getDecision());
    }
}
