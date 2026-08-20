package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.pipeline.PipelineOrchestrator;
import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação da atividade de transformação de dados.
 * Delega ao PipelineOrchestrator real: Canonicalização + Validação de Schema +
 * Qualidade de Dados + Classificação PII + Linhagem (INV03) — não simula mais
 * uma string concatenada.
 */
@Component
public class TransformDataActivityImpl implements TransformDataActivity {

    private static final Logger log = LoggerFactory.getLogger(TransformDataActivityImpl.class);

    private static final Map<String, ResourceType> RESOURCE_TYPE_BY_DOMAIN = Map.ofEntries(
            Map.entry("contas", ResourceType.CONTA),
            Map.entry("transacoes", ResourceType.TRANSACAO),
            Map.entry("cartoes", ResourceType.CARTAO),
            Map.entry("faturas", ResourceType.CARTAO),
            Map.entry("transacoes_cartao", ResourceType.CARTAO),
            Map.entry("emprestimos", ResourceType.CREDITO),
            Map.entry("pix", ResourceType.PIX),
            Map.entry("investimentos", ResourceType.INVESTIMENTO)
    );

    private final PipelineOrchestrator pipelineOrchestrator;

    public TransformDataActivityImpl(PipelineOrchestrator pipelineOrchestrator) {
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    @Override
    public TransformResult transform(TransformRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando transformação - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();
        TransformResult result = new TransformResult();
        result.setNodeId(request.getNodeId());

        try {
            ResourceType type = RESOURCE_TYPE_BY_DOMAIN.getOrDefault(request.getResource(), ResourceType.CONTA);

            RawRecord raw = RawRecord.criar(
                    UUID.randomUUID().toString(),
                    "AURIX_CORE",
                    request.getNodeId(),
                    type,
                    request.getRawData() != null ? request.getRawData() : "{}",
                    LocalDateTime.now(),
                    "1.0");

            PipelineOrchestrator.PipelineResult pipelineResult = pipelineOrchestrator.execute(
                    raw, type, request.getExecutionPlanId(), request.getConsentId(), request.getResource());

            if (!pipelineResult.isSucesso()) {
                result.setSuccess(false);
                result.setErrorMessage("Etapa " + pipelineResult.getEtapaFalha() + ": "
                        + pipelineResult.getMensagemFalha());
                return result;
            }

            result.setRecordCount(request.getRecordCount());
            result.setCanonicalData(pipelineResult.getCanonicalRecord().getCanonicalData());
            result.setLineageId(pipelineResult.getLineageRecord().getLineageId());
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Transformação concluída - node: {}, registros: {}, duração: {}ms",
                    request.getNodeId(), result.getRecordCount(), duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na transformação - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
}
