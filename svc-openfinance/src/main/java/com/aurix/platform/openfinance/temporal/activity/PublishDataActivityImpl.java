package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação da atividade de publicação de dados.
 * Publica dados para o plano de distribuição:
 * - Kafka events
 * - REST API cache
 * - Data products
 */
@Component
public class PublishDataActivityImpl implements PublishDataActivity {

    private static final Logger log = LoggerFactory.getLogger(PublishDataActivityImpl.class);

    @Override
    public PublishResult publish(PublishRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando publicação - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();

        try {
            PublishResult result = new PublishResult();
            result.setNodeId(request.getNodeId());

            int publishedCount = simularPublicacao(request.getResource(), request.getRecordCount());
            result.setPublishedCount(publishedCount);
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Publicação concluída - node: {}, publicados: {}, duração: {}ms",
                    request.getNodeId(), publishedCount, duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na publicação - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            PublishResult result = new PublishResult();
            result.setNodeId(request.getNodeId());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    private int simularPublicacao(String resource, int recordCount) {
        log.info("Publicando {} registros de {} para Kafka + cache + data products", recordCount, resource);
        return recordCount;
    }
}
