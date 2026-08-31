package com.aurix.platform.openfinance.pipeline;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import com.aurix.platform.openfinance.pipeline.pii.repository.PiiClassificationRepository;
import com.aurix.platform.openfinance.pipeline.quality.repository.QualityResultRepository;
import com.aurix.platform.openfinance.pipeline.validation.repository.ValidationResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Autowired
    private ValidationResultRepository validationResultRepository;

    @Autowired
    private QualityResultRepository qualityResultRepository;

    @Autowired
    private PiiClassificationRepository piiClassificationRepository;

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

    /**
     * INV — publicação nunca carrega dados pessoais em claro: o registro
     * mascarado (PiiResult.maskedRecord) deixa de conter o CPF original, enquanto
     * o canônico interna retém o valor para auditoria. A activity de transformação
     * deve publicar o mascarado.
     */
    @Test
    void deveProduzirRegistroMascaradoParaPublicacao() {
        String cpfOriginal = "123.456.789-01";
        RawRecord raw = RawRecord.criar(UUID.randomUUID().toString(), "AURIX_CORE",
                UUID.randomUUID().toString(), ResourceType.CONTA,
                "{\"conta\":\"1234\",\"cpf\":\"" + cpfOriginal + "\"}",
                LocalDateTime.now(), "1.0");

        PipelineOrchestrator.PipelineResult result = orchestrator.execute(
                raw, ResourceType.CONTA, "plan-mask-test", "consent-mask-test", "resource-accounts");

        assertTrue(result.isSucesso(), "pipeline deveria ter sucesso com PII classificado");

        String canonical = result.getCanonicalRecord().getCanonicalData();
        String mascara = result.getPiiResult().getMaskedRecord();

        // Canônico interno reteve o CPF (auditoria interna); o que sai para
        // publicação é o mascarado — sem o CPF em claro.
        assertTrue(canonical.contains(cpfOriginal),
                "canônico deveria reter o CPF para auditoria interna");
        assertFalse(mascara.contains(cpfOriginal),
                "registro publicado não pode conter o CPF em claro");
        assertNotEquals(canonical, mascara,
                "publicação deveria carregar o registro protegido, não o canônico em claro");
    }

    /**
     * Gap 5 — persistência do pipeline: além da linhagem, os resultados
     * intermediários (validação, qualidade e classificação PII) devem ser
     * gravados em banco, não apenas mantidos em memória.
     */
    @Test
    void devePersistirResultadosIntermediariosDoPipeline() {
        long validacoesAntes = validationResultRepository.count();
        long qualidadesAntes = qualityResultRepository.count();
        long piiAntes = piiClassificationRepository.count();

        String recordId = UUID.randomUUID().toString();
        RawRecord raw = RawRecord.criar(recordId, "AURIX_CORE", UUID.randomUUID().toString(),
                ResourceType.CONTA, "{\"conta\":\"1234\",\"cpf\":\"123.456.789-01\"}",
                LocalDateTime.now(), "1.0");

        PipelineOrchestrator.PipelineResult result = orchestrator.execute(
                raw, ResourceType.CONTA, "plan-persist-test", "consent-persist-test", "resource-accounts");

        assertTrue(result.isSucesso());

        assertEquals(validacoesAntes + 1, validationResultRepository.count(),
                "validação de schema deveria ser persistida");
        assertEquals(qualidadesAntes + 1, qualityResultRepository.count(),
                "verificação de qualidade deveria ser persistida");
        assertEquals(piiAntes + 1, piiClassificationRepository.count(),
                "classificação PII deveria ser persistida");
    }
}
