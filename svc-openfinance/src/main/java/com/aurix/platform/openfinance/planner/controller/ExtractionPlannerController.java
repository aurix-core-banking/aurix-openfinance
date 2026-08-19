package com.aurix.platform.openfinance.planner.controller;

import com.aurix.platform.openfinance.planner.dto.PlanRequest;
import com.aurix.platform.openfinance.planner.dto.PlanResponse;
import com.aurix.platform.openfinance.planner.entity.ExecutionPlan;
import com.aurix.platform.openfinance.planner.entity.PlanStatus;
import com.aurix.platform.openfinance.planner.service.ExtractionPlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para gestão de planos de extração Open Finance.
 */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Planos de Extração", description = "API para criação e gestão de planos de extração Open Finance")
public class ExtractionPlannerController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractionPlannerController.class);
    private final ExtractionPlannerService plannerService;

    public ExtractionPlannerController(ExtractionPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping
    @Operation(summary = "Criar plano de extração", description = "Cria um novo plano de execução DAG para extração de dados")
    public ResponseEntity<PlanResponse> criarPlano(@Valid @RequestBody PlanRequest request) {
        log.info("Recebida solicitação para criar plano de extração - consentimento: {}", request.getConsentId());
        PlanResponse response = plannerService.criarPlano(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Buscar plano por ID", description = "Retorna um plano de execução pelo ID")
    public ResponseEntity<PlanResponse> buscarPlano(
            @Parameter(description = "ID do plano") @PathVariable String planId) {
        log.info("Buscando plano: {}", planId);
        ExecutionPlan plano = plannerService.buscarPorPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + planId));
        return ResponseEntity.ok(converterParaResponse(plano));
    }

    @GetMapping("/{planId}/status")
    @Operation(summary = "Consultar status do plano", description = "Retorna o status atual de um plano de execução")
    public ResponseEntity<Map<String, Object>> consultarStatus(
            @Parameter(description = "ID do plano") @PathVariable String planId) {
        log.info("Consultando status do plano: {}", planId);
        ExecutionPlan plano = plannerService.buscarPorPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + planId));

        return ResponseEntity.ok(Map.of(
                "plan_id", plano.getPlanId(),
                "status", plano.getStatus().name(),
                "data_criacao", plano.getDataCriacao() != null ? plano.getDataCriacao().toString() : null,
                "data_execucao", plano.getDataExecucao() != null ? plano.getDataExecucao().toString() : null,
                "data_conclusao", plano.getDataConclusao() != null ? plano.getDataConclusao().toString() : null
        ));
    }

    @PostMapping("/{planId}/cancel")
    @Operation(summary = "Cancelar plano", description = "Cancela um plano de execução em andamento")
    public ResponseEntity<Map<String, String>> cancelarPlano(
            @Parameter(description = "ID do plano") @PathVariable String planId,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("Cancelando plano: {}", planId);
        String motivo = body != null ? body.getOrDefault("motivo", "Cancelado pelo participante") : "Cancelado pelo participante";
        ExecutionPlan plano = plannerService.cancelarPlano(planId, motivo);
        return ResponseEntity.ok(Map.of(
                "plan_id", plano.getPlanId(),
                "status", plano.getStatus().name(),
                "motivo", motivo
        ));
    }

    private PlanResponse converterParaResponse(ExecutionPlan plano) {
        PlanResponse response = new PlanResponse();
        response.setPlanId(plano.getPlanId());
        response.setConsentId(plano.getConsentId());
        response.setConsentVersion(plano.getConsentVersion());
        response.setParticipantId(plano.getParticipanteId());
        response.setStatus(plano.getStatus().name());
        response.setCreatedAt(plano.getDataCriacao());
        response.setExecutedAt(plano.getDataExecucao());
        response.setCompletedAt(plano.getDataConclusao());
        return response;
    }
}
