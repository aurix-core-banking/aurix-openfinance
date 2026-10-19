package com.aurix.platform.openfinance.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários das 6 métricas obrigatórias de observabilidade declaradas
 * em {@code architecture/architecture.yaml} (Issue #10).
 */
class OpenFinanceMetricsServiceTest {

    @Test
    @DisplayName("OpenFinanceMetricsService deve registrar as 6 métricas obrigatórias no MeterRegistry")
    void deveRegistrarSeisMetricasObrigatoriasDeArchitectureYaml() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OpenFinanceMetricsService metrics = new OpenFinanceMetricsService(registry);

        metrics.recordConsentApproval("AUTHORISED");
        metrics.recordExtractionResult("contas", true);
        metrics.recordPipelineLatency("canonicalization", 15L);
        metrics.recordReconciliationDivergence("emprestimos", 2);
        metrics.updateDataFreshnessSeconds(42L);
        metrics.recordPolicyDecision("ALLOWED");

        assertEquals(1.0, registry.get("consent_approval_rate").tag("status", "AUTHORISED").counter().count());
        assertEquals(1.0, registry.get("extraction_success_rate")
                .tag("domain", "contas").tag("outcome", "SUCCESS").counter().count());
        assertEquals(1L, registry.get("pipeline_latency_p99").tag("stage", "canonicalization").timer().count());
        assertEquals(2.0, registry.get("reconciliation_divergence_rate")
                .tag("domain", "emprestimos").counter().count());
        assertEquals(42.0, registry.get("data_freshness").gauge().value());
        assertEquals(1.0, registry.get("policy_decision_rate").tag("decision", "ALLOWED").counter().count());
    }
}
