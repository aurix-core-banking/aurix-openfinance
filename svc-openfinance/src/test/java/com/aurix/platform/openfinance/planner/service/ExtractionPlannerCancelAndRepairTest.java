package com.aurix.platform.openfinance.planner.service;

import com.aurix.platform.openfinance.context.service.AuthorizedContextService;
import com.aurix.platform.openfinance.planner.entity.ExecutionPlan;
import com.aurix.platform.openfinance.planner.entity.PlanStatus;
import com.aurix.platform.openfinance.planner.repository.ExecutionPlanRepository;
import com.aurix.platform.openfinance.temporal.dispatcher.WorkflowExecutionDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários da integração Planner -> Temporal para cancelamento e reparo (Issue #7).
 */
class ExtractionPlannerCancelAndRepairTest {

    @Test
    @DisplayName("ExtractionPlannerService deve propagar sinal de cancelamento ao WorkflowExecutionDispatcher")
    void devePropagarCancelamentoAoDispatcherTemporal() {
        ExecutionPlanRepository planRepo = mock(ExecutionPlanRepository.class);
        WorkflowExecutionDispatcher dispatcher = mock(WorkflowExecutionDispatcher.class);
        AuthorizedContextService contextService = mock(AuthorizedContextService.class);

        ExecutionPlan plan = new ExecutionPlan();
        plan.setPlanId("plan-cancel-1");
        plan.setConsentId("consent-cancel-1");
        plan.setStatus(PlanStatus.EM_EXECUCAO);
        when(planRepo.findByConsentId("consent-cancel-1")).thenReturn(List.of(plan));
        when(planRepo.save(any(ExecutionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionPlannerService planner = new ExtractionPlannerService(planRepo, dispatcher, contextService);
        int cancelados = planner.cancelarPlanosPorConsentimento("consent-cancel-1", "Revogado pelo cliente");

        assertEquals(1, cancelados);
        verify(dispatcher).cancelarWorkflow("consent-cancel-1", "plan-cancel-1", "Revogado pelo cliente");
    }

    @Test
    @DisplayName("ExtractionPlannerService.reextrairNode deve despachar execução de reparo para o node divergente")
    void deveDespacharReparoParaNodeDivergente() {
        ExecutionPlanRepository planRepo = mock(ExecutionPlanRepository.class);
        WorkflowExecutionDispatcher dispatcher = mock(WorkflowExecutionDispatcher.class);
        AuthorizedContextService contextService = mock(AuthorizedContextService.class);

        ExecutionPlan plan = new ExecutionPlan();
        plan.setPlanId("plan-repair-1");
        plan.setConsentId("consent-repair-1");
        plan.setConsentVersion(1);
        plan.setParticipanteId("part-1");
        plan.setDagDefinition("{}");
        when(planRepo.findByPlanId("plan-repair-1")).thenReturn(Optional.of(plan));

        ExtractionPlannerService planner = new ExtractionPlannerService(planRepo, dispatcher, contextService);
        int reparados = planner.reextrairNode("plan-repair-1", "node-contas", 2);

        assertEquals(2, reparados);
        verify(dispatcher).despachar(argThat(req ->
                "plan-repair-1-repair-contas".equals(req.getPlanId())
                        && req.getNodes().size() == 1
                        && "contas".equals(req.getNodes().get(0).getResource())));
    }

    @Test
    @DisplayName("ExtractionPlannerService deve orquestrar automaticamente consent-to-execution ao receber ConsentGrantedEvent")
    void deveOrquestrarConsentToExecutionAutomaticamenteAoAprovarConsentimento() {
        ExecutionPlanRepository planRepo = mock(ExecutionPlanRepository.class);
        WorkflowExecutionDispatcher dispatcher = mock(WorkflowExecutionDispatcher.class);
        AuthorizedContextService contextService = mock(AuthorizedContextService.class);
        com.aurix.platform.openfinance.repository.ConsentimentoRepository consentRepo =
                mock(com.aurix.platform.openfinance.repository.ConsentimentoRepository.class);
        com.aurix.platform.openfinance.discovery.service.ResourceDiscoveryService discoveryService =
                mock(com.aurix.platform.openfinance.discovery.service.ResourceDiscoveryService.class);
        com.aurix.platform.openfinance.policy.service.PolicyEngineService policyService =
                mock(com.aurix.platform.openfinance.policy.service.PolicyEngineService.class);

        com.aurix.platform.openfinance.entity.Consentimento consent =
                new com.aurix.platform.openfinance.entity.Consentimento();
        consent.setConsentId("consent-auto-1");
        consent.setClientId("client-auto-1");
        consent.setUserId(99L);
        consent.setVersion(2);
        consent.setPermissions("accounts,loans,investments");
        consent.setStatus(com.aurix.platform.openfinance.entity.Consentimento.StatusConsentimento.AUTHORISED);
        when(consentRepo.findByConsentId("consent-auto-1")).thenReturn(Optional.of(consent));

        com.aurix.platform.openfinance.discovery.entity.ResourceGraph graph =
                new com.aurix.platform.openfinance.discovery.entity.ResourceGraph("graph-1", "consent-auto-1", 1);
        when(discoveryService.discover(eq("consent-auto-1"), anyList(), anyMap())).thenReturn(graph);

        com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse allowedDecision =
                new com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse(
                        com.aurix.platform.openfinance.policy.entity.PolicyDecisionType.ALLOWED,
                        "Todas as regras satisfeitas",
                        List.of("CONSENT-001"),
                        5,
                        "consent-auto-1",
                        "graph-1:accounts",
                        "accounts");
        when(policyService.evaluate(any())).thenReturn(allowedDecision);
        when(contextService.listActiveByConsentId("consent-auto-1")).thenReturn(List.of());
        when(planRepo.findByConsentId("consent-auto-1")).thenReturn(List.of());

        java.util.Map<String, ExecutionPlan> savedPlans = new java.util.HashMap<>();
        when(planRepo.save(any(ExecutionPlan.class))).thenAnswer(inv -> {
            ExecutionPlan p = inv.getArgument(0);
            savedPlans.put(p.getPlanId(), p);
            return p;
        });
        when(planRepo.findByPlanId(anyString())).thenAnswer(inv ->
                Optional.ofNullable(savedPlans.get(inv.getArgument(0))));
        when(dispatcher.despachar(any())).thenReturn("wf-auto-1");

        ExtractionPlannerService planner = new ExtractionPlannerService(planRepo, dispatcher, contextService);
        planner.configurarOrquestracaoConsentimento(consentRepo, discoveryService, policyService);

        planner.onConsentGranted(new com.aurix.platform.openfinance.event.ConsentGrantedEvent(
                "consent-auto-1", "client-auto-1", java.util.Map.of()));

        verify(discoveryService).discover(eq("consent-auto-1"), anyList(), anyMap());
        verify(policyService).evaluate(any());
        verify(contextService).create(eq(allowedDecision), anyString(), anyString(), anyString(),
                argThat(json -> json.contains("contas") && json.contains("emprestimos") && json.contains("investimentos")),
                eq("ES256"), anyString());
        verify(dispatcher).despachar(argThat(req ->
                "consent-auto-1".equals(req.getConsentId()) && req.getNodes().size() == 3));
    }
}
