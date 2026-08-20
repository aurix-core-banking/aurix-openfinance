package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.pipeline.lineage.service.LineageService;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da atividade de publicação de dados.
 * Publica de verdade no tópico Kafka {@code data.published.v1} (catálogo em
 * architecture/events.yaml) e fecha a cadeia de linhagem (INV03) atualizando o
 * publicationId do registro criado na etapa de transformação — antes disso a
 * "publicação" era só um log.info sem nenhum efeito observável.
 *
 * <p>O {@code KafkaTemplate} é opcional porque {@code KafkaConfig} (aurix-shared)
 * é {@code @Profile("!test")} — em produção/dev ele sempre está presente; em
 * testes a publicação é pulada com um log, sem exigir um broker Kafka rodando.
 */
@Component
public class PublishDataActivityImpl implements PublishDataActivity {

    private static final Logger log = LoggerFactory.getLogger(PublishDataActivityImpl.class);
    private static final String TOPIC_DATA_PUBLISHED = "data.published.v1";

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final LineageService lineageService;

    public PublishDataActivityImpl(Optional<KafkaTemplate<String, Object>> kafkaTemplate,
                                    LineageService lineageService) {
        this.kafkaTemplate = kafkaTemplate;
        this.lineageService = lineageService;
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando publicação - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();
        PublishResult result = new PublishResult();
        result.setNodeId(request.getNodeId());

        try {
            String publicationId = UUID.randomUUID().toString();

            Map<String, Object> event = Map.of(
                    "publicationId", publicationId,
                    "nodeId", request.getNodeId(),
                    "resource", request.getResource(),
                    "recordCount", request.getRecordCount(),
                    "canonicalData", request.getCanonicalData() != null ? request.getCanonicalData() : "",
                    "publishedAt", LocalDateTime.now().toString()
            );
            if (kafkaTemplate.isPresent()) {
                kafkaTemplate.get().send(TOPIC_DATA_PUBLISHED, request.getNodeId(), event);
            } else {
                log.warn("KafkaTemplate indisponível (perfil sem Kafka) — publicação de {} não enviada",
                        request.getNodeId());
            }

            if (request.getLineageId() != null) {
                lineageService.atualizarPublicacao(request.getLineageId(), publicationId);
            }

            result.setPublishedCount(request.getRecordCount());
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Publicação concluída - node: {}, publicados: {}, duração: {}ms",
                    request.getNodeId(), result.getPublishedCount(), duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na publicação - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
}
