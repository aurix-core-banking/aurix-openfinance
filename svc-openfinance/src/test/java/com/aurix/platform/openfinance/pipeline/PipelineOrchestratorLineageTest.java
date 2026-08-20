package com.aurix.platform.openfinance.pipeline;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que a linhagem (INV03) é persistida de verdade em banco — não num
 * ConcurrentHashMap por instância — e que consentId/resourceId chegam
 * corretamente ao registro (bug anterior: PipelineOrchestrator sempre
 * gravava null nesses dois campos).
 */
@SpringBootTest
@ActiveProfiles("test")
class PipelineOrchestratorLineageTest {

    @Autowired
    private PipelineOrchestrator orchestrator;

    @Autowired
    private LineageRecordRepository lineageRepository;

    @Test
    void deveRegistrarLinhagemPersistidaComConsentIdEResourceIdReais() {
        String recordId = UUID.randomUUID().toString();
        RawRecord raw = RawRecord.criar(recordId, "AURIX_CORE", UUID.randomUUID().toString(),
                ResourceType.CONTA, "{\"conta\":\"1234\"}", LocalDateTime.now(), "1.0");

        PipelineOrchestrator.PipelineResult result = orchestrator.execute(
                raw, ResourceType.CONTA, "plan-lineage-test", "consent-lineage-test", "resource-accounts");

        assertTrue(result.isSucesso());
        String lineageId = result.getLineageRecord().getLineageId();

        // Busca via repositório (nova query no banco) — não reaproveita nenhum
        // estado em memória do orchestrator/service, provando persistência real.
        LineageRecord persisted = lineageRepository.findByLineageId(lineageId).orElseThrow();

        assertEquals("consent-lineage-test", persisted.getConsentId());
        assertEquals("resource-accounts", persisted.getResourceId());
        assertEquals("plan-lineage-test", persisted.getExecutionPlanId());
    }
}
