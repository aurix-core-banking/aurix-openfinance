package com.aurix.platform.openfinance.planner.service;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.context.service.AuthorizedContextService;
import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.planner.dto.PlanRequest;
import com.aurix.platform.openfinance.planner.dto.PlanResponse;
import com.aurix.platform.openfinance.planner.entity.ExecutionPlan;
import com.aurix.platform.openfinance.planner.entity.PlanStatus;
import com.aurix.platform.openfinance.planner.repository.ExecutionPlanRepository;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import com.aurix.platform.openfinance.service.ConsentimentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que ExtractionPlannerService monta o DAG com dependências transitivas
 * corretas (transacoes/cartoes dependem de contas — que precisa aparecer antes
 * na ordenação topológica), persiste o plano e executa o fluxo consent-revocation
 * em cascata (cancelando planos em aberto e revogando contextos autorizados).
 */
@SpringBootTest
@ActiveProfiles("test")
class ExtractionPlannerServiceTest {

    @Autowired
    private ExtractionPlannerService plannerService;

    @Autowired
    private ExecutionPlanRepository planRepository;

    @Autowired
    private AuthorizedContextService authorizedContextService;

    @Autowired
    private AuthorizedContextRepository authorizedContextRepository;

    @Autowired
    private ConsentimentoService consentimentoService;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        authorizedContextRepository.deleteAll();
    }

    @Test
    void deveIncluirDependenciaTransitivaDeContasAoSolicitarCartoes() {
        PlanRequest request = new PlanRequest();
        request.setConsentId("consent-plan-test");
        request.setConsentVersion(1);
        request.setParticipantId("participant-1");
        request.setRequestedResources(List.of("cartoes"));

        PlanResponse response = plannerService.criarPlano(request);

        List<String> resources = response.getNodes().stream()
                .map(PlanResponse.PlanNodeInfo::getResource)
                .toList();

        assertTrue(resources.contains("contas"),
                "cartoes depende de contas — contas deveria ter sido incluído automaticamente");
        assertTrue(resources.contains("cartoes"));
    }

    @Test
    void devePersistirPlanoRecuperavelPorPlanId() {
        PlanRequest request = new PlanRequest();
        request.setConsentId("consent-plan-persist");
        request.setConsentVersion(1);
        request.setParticipantId("participant-1");
        request.setRequestedResources(List.of("contas"));

        PlanResponse response = plannerService.criarPlano(request);

        assertTrue(plannerService.buscarPorPlanId(response.getPlanId()).isPresent());
        assertEquals("consent-plan-persist",
                plannerService.buscarPorPlanId(response.getPlanId()).get().getConsentId());
    }

    @Test
    void deveCancelarPlanosAtivosERevogarContextosAoRevogarConsentimento() {
        ConsentimentoRequest consentReq = new ConsentimentoRequest();
        consentReq.setClientId("client-revogacao");
        consentReq.setPermissions(List.of("ACCOUNTS_READ"));
        consentReq.setExpirationDays(30);
        ConsentimentoResponse criado = consentimentoService.criar(consentReq, 1L);
        String consentId = criado.getConsentId();
        consentimentoService.aprovar(consentId);

        PolicyDecisionResponse decision = new PolicyDecisionResponse(
                PolicyDecisionType.ALLOWED, "OK", List.of("RULE-1"), 1,
                consentId, "res-1", "ACCOUNTS_READ");
        AuthorizedContext context = authorizedContextService.create(
                decision, "user-1", "OPEN_FINANCE", "[\"ACCOUNTS_READ\"]",
                "[\"contas\"]", "ES256", "thumb-1");

        PlanRequest planReq = new PlanRequest();
        planReq.setConsentId(consentId);
        planReq.setConsentVersion(2);
        planReq.setParticipantId("client-revogacao");
        planReq.setRequestedResources(List.of("contas"));
        PlanResponse plano = plannerService.criarPlano(planReq);

        assertEquals(1, authorizedContextService.listActiveByConsentId(consentId).size());

        consentimentoService.revogar(consentId, "Revogado pelo titular");

        ExecutionPlan planoAtualizado = plannerService.buscarPorPlanId(plano.getPlanId()).orElseThrow();
        assertEquals(PlanStatus.CANCELADO, planoAtualizado.getStatus(),
                "Plano ativo deveria ser cancelado quando o consentimento é revogado");
        assertTrue(authorizedContextService.listActiveByConsentId(consentId).isEmpty(),
                "Contextos autorizados do consentimento deveriam ser revogados em cascata");
        assertTrue(authorizedContextService.getByContextId(context.getContextId()).isRevoked(),
                "Contexto autorizado específico deveria estar marcado como revogado");
    }
}
