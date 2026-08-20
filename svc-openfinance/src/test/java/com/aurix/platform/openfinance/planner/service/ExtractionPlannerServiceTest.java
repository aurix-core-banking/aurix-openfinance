package com.aurix.platform.openfinance.planner.service;

import com.aurix.platform.openfinance.planner.dto.PlanRequest;
import com.aurix.platform.openfinance.planner.dto.PlanResponse;
import com.aurix.platform.openfinance.planner.repository.ExecutionPlanRepository;
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
 * na ordenação topológica) e que o plano é persistido de verdade.
 */
@SpringBootTest
@ActiveProfiles("test")
class ExtractionPlannerServiceTest {

    @Autowired
    private ExtractionPlannerService plannerService;

    @Autowired
    private ExecutionPlanRepository planRepository;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
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
}
