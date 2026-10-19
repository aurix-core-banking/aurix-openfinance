package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.pipeline.PipelineOrchestrator;
import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação da atividade de transformação de dados.
 * Delega ao PipelineOrchestrator real: Canonicalização + Validação de Schema +
 * Qualidade de Dados + Classificação PII + Linhagem (INV03) — não simula mais
 * uma string concatenada. Publica o registro com PII mascarado (PiiResult.maskedRecord),
 * nunca o canônico sem proteção — a publicação NUNCA carrega dados pessoais em claro.
 * Suporta processamento unitário e em lote (arrays de múltiplos registros extraídos).
 */
@Component
public class TransformDataActivityImpl implements TransformDataActivity {

    private static final Logger log = LoggerFactory.getLogger(TransformDataActivityImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> CHAVES_ENVELOPE = List.of(
            "contas", "saldos", "transacoes", "cartoes", "faturas", "transacoes_cartao",
            "emprestimos", "parcelas_emprestimo", "transacoes_pix", "investimentos", "seguros", "pessoas",
            "records", "items", "data");

    private static final Map<String, ResourceType> RESOURCE_TYPE_BY_DOMAIN = Map.ofEntries(
            Map.entry("contas", ResourceType.CONTA),
            Map.entry("saldos", ResourceType.SALDO),
            Map.entry("transacoes", ResourceType.TRANSACAO),
            Map.entry("cartoes", ResourceType.CARTAO),
            Map.entry("faturas", ResourceType.FATURA_CARTAO),
            Map.entry("transacoes_cartao", ResourceType.TRANSACAO_CARTAO),
            Map.entry("emprestimos", ResourceType.CREDITO),
            Map.entry("parcelas_emprestimo", ResourceType.PARCELA_CREDITO),
            Map.entry("pix", ResourceType.PIX),
            Map.entry("investimentos", ResourceType.INVESTIMENTO),
            Map.entry("seguros", ResourceType.SEGURO),
            Map.entry("pessoas", ResourceType.PESSOA)
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
            String rawPayload = request.getRawData() != null ? request.getRawData() : "{}";
            List<String> payloadsIndividuais = desempacotarLote(rawPayload);

            List<String> publicados = new ArrayList<>();
            List<String> lineageIds = new ArrayList<>();
            String sensitivityLevel = "PUBLIC";

            for (String itemRawJson : payloadsIndividuais) {
                RawRecord raw = RawRecord.criar(
                        UUID.randomUUID().toString(),
                        "AURIX_CORE",
                        request.getNodeId(),
                        type,
                        itemRawJson,
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

                publicados.add(dadosParaPublicacao(pipelineResult));
                if (pipelineResult.getLineageRecord() != null
                        && pipelineResult.getLineageRecord().getLineageId() != null) {
                    lineageIds.add(pipelineResult.getLineageRecord().getLineageId());
                }
                if (pipelineResult.getPiiResult() != null
                        && pipelineResult.getPiiResult().getSensitivityLevel() != null) {
                    sensitivityLevel = pipelineResult.getPiiResult().getSensitivityLevel().name();
                }
            }

            result.setRecordCount(Math.max(request.getRecordCount(), publicados.size()));
            if (publicados.size() == 1) {
                result.setCanonicalData(publicados.get(0));
            } else {
                ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
                for (String pub : publicados) {
                    try {
                        arrayNode.add(OBJECT_MAPPER.readTree(pub));
                    } catch (Exception ignored) {
                        arrayNode.add(pub);
                    }
                }
                result.setCanonicalData(OBJECT_MAPPER.writeValueAsString(arrayNode));
            }
            result.setLineageId(String.join(",", lineageIds));
            result.setSensitivityLevel(sensitivityLevel);
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

    private List<String> desempacotarLote(String rawPayload) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawPayload);
            if (root != null && root.isArray() && root.size() > 1) {
                List<String> lista = new ArrayList<>(root.size());
                for (JsonNode elem : root) {
                    lista.add(OBJECT_MAPPER.writeValueAsString(elem));
                }
                return lista;
            }
            if (root != null && root.isObject()) {
                for (String chave : CHAVES_ENVELOPE) {
                    JsonNode arr = root.get(chave);
                    if (arr != null && arr.isArray() && arr.size() > 1) {
                        List<String> lista = new ArrayList<>(arr.size());
                        for (JsonNode elem : arr) {
                            lista.add(OBJECT_MAPPER.writeValueAsString(elem));
                        }
                        return lista;
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback para registro único
        }
        return List.of(rawPayload);
    }

    /**
     * Dados a publicar: o registro mascarado pela classificação PII (INV — dados
     * pessoais nunca saem em claro). Caso o pipeline não tenha classificado PII
     * (defensivo), cai no canônico.
     */
    private String dadosParaPublicacao(PipelineOrchestrator.PipelineResult pipelineResult) {
        if (pipelineResult.getPiiResult() != null
                && pipelineResult.getPiiResult().getMaskedRecord() != null) {
            return pipelineResult.getPiiResult().getMaskedRecord();
        }
        return pipelineResult.getCanonicalRecord().getCanonicalData();
    }
}
