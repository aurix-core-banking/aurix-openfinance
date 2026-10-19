package com.aurix.platform.openfinance.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de métricas e observabilidade do Open Finance.
 * Registra no Micrometer {@link MeterRegistry} as 6 métricas obrigatórias declaradas
 * em {@code architecture/architecture.yaml} ({@code observability.metrics}).
 */
@Service
public class OpenFinanceMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicLong dataFreshnessSeconds = new AtomicLong(0);

    public OpenFinanceMetricsService() {
        this(new SimpleMeterRegistry());
    }

    @Autowired
    public OpenFinanceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.meterRegistry.gauge("data_freshness", dataFreshnessSeconds);
    }

    public void recordConsentApproval(String status) {
        Counter.builder("consent_approval_rate")
                .description("Consentimentos concedidos vs solicitados")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public void recordExtractionResult(String domain, boolean success) {
        Counter.builder("extraction_success_rate")
                .description("Extrações bem-sucedidas vs totais")
                .tag("domain", domain != null ? domain : "unknown")
                .tag("outcome", success ? "SUCCESS" : "FAILURE")
                .register(meterRegistry)
                .increment();
    }

    public void recordPipelineLatency(String stage, long durationMs) {
        Timer.builder("pipeline_latency_p99")
                .description("Latência por estágio do pipeline")
                .tag("stage", stage != null ? stage : "pipeline")
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(durationMs, 0)));
    }

    public void recordReconciliationDivergence(String domain, int divergentCount) {
        Counter.builder("reconciliation_divergence_rate")
                .description("Registros com divergência detectada")
                .tag("domain", domain != null ? domain : "unknown")
                .register(meterRegistry)
                .increment(Math.max(divergentCount, 0));
    }

    public void updateDataFreshnessSeconds(long ageSeconds) {
        dataFreshnessSeconds.set(Math.max(ageSeconds, 0));
    }

    public void recordPolicyDecision(String decision) {
        Counter.builder("policy_decision_rate")
                .description("Decisões de política por resultado (allow/deny)")
                .tag("decision", decision != null ? decision : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
