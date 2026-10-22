package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.DisponibilidadeResponse;
import com.aurix.platform.openfinance.dto.MetricasSlaResponse;
import com.aurix.platform.openfinance.observability.MetricasRegulatoriasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller regulatório para publicação de métricas de SLA e disponibilidade (IN BACEN nº 106).
 */
@RestController
@RequestMapping("/open-finance/v1/metrics")
@Tag(name = "Métricas Regulatórias", description = "Monitoramento de disponibilidade e SLA segundo Instrução Normativa BACEN nº 106")
public class MetricasRegulatoriasController {

    private final MetricasRegulatoriasService metricasService;

    public MetricasRegulatoriasController(MetricasRegulatoriasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/availability")
    @Operation(summary = "Consultar métricas de disponibilidade e uptime regulatório (BACEN IN 106)")
    public ResponseEntity<DisponibilidadeResponse> getDisponibilidade() {
        return ResponseEntity.ok(metricasService.obterDisponibilidade());
    }

    @GetMapping("/sla")
    @Operation(summary = "Consultar métricas consolidadas de SLA e percentis de latência (p50, p95, p99)")
    public ResponseEntity<MetricasSlaResponse> getMetricasSla() {
        return ResponseEntity.ok(metricasService.obterMetricasSla());
    }
}
