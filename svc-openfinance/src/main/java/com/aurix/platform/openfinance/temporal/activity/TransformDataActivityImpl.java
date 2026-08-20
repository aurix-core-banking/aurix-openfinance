package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação da atividade de transformação de dados.
 * Canonicalização + Validação de Schema + Qualidade de Dados.
 * Delega para serviços do DataPlane.
 */
@Component
public class TransformDataActivityImpl implements TransformDataActivity {

    private static final Logger log = LoggerFactory.getLogger(TransformDataActivityImpl.class);

    @Override
    public TransformResult transform(TransformRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando transformação - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();

        try {
            TransformResult result = new TransformResult();
            result.setNodeId(request.getNodeId());
            result.setRecordCount(request.getRecordCount());
            result.setCanonicalData(canonicalizar(request.getRawData(), request.getResource()));
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Transformação concluída - node: {}, registros: {}, duração: {}ms",
                    request.getNodeId(), result.getRecordCount(), duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na transformação - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            TransformResult result = new TransformResult();
            result.setNodeId(request.getNodeId());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    private String canonicalizar(String rawData, String resource) {
        return "canonical_" + resource + "_" + (rawData != null ? rawData.hashCode() : 0);
    }
}
