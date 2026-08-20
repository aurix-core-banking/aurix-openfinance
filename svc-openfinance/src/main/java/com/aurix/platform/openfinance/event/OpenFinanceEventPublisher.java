package com.aurix.platform.openfinance.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Publica os eventos que o próprio módulo define em architecture/events.yaml e que,
 * antes desta classe, nenhum serviço emitia — só havia consumers de eventos de
 * core-banking, nunca producers dos tópicos {@code consent.*}/{@code data.published.v1}
 * que o Open Finance é dono.
 *
 * <p>Segue o padrão de metadados exigido em events.yaml (eventId, eventType,
 * timestamp, consentId) como um envelope simples em cima do payload específico.
 *
 * <p>{@code KafkaTemplate} é opcional porque {@code KafkaConfig} (aurix-shared) é
 * {@code @Profile("!test")} — publicar vira um no-op logado em vez de exigir um
 * broker Kafka rodando para os testes existentes.
 */
@Component
public class OpenFinanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceEventPublisher.class);

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;

    public OpenFinanceEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishConsentGranted(String consentId, String clientId) {
        publish("consent.granted.v1", consentId, Map.of("clientId", clientId));
    }

    public void publishConsentUpdated(String consentId, String reason) {
        publish("consent.updated.v1", consentId, Map.of("reason", reason));
    }

    public void publishConsentRevoked(String consentId, String reason) {
        publish("consent.revoked.v1", consentId, Map.of("reason", reason));
    }

    public void publishConsentExpired(String consentId) {
        publish("consent.expired.v1", consentId, Map.of());
    }

    public void publishReconciliationTriggered(String consentId, String executionPlanId) {
        publish("reconciliation.triggered.v1", consentId, Map.of("executionPlanId", executionPlanId));
    }

    public void publishReconciliationDivergenceDetected(String consentId, String executionPlanId,
                                                          String details) {
        publish("reconciliation.divergence-detected.v1", consentId,
                Map.of("executionPlanId", executionPlanId, "details", details));
    }

    public void publishReconciliationRepaired(String consentId, String executionPlanId) {
        publish("reconciliation.repaired.v1", consentId, Map.of("executionPlanId", executionPlanId));
    }

    /**
     * Publica um evento genérico no tópico dado, envelopado com os metadados
     * obrigatórios do catálogo (eventId, eventType, timestamp, consentId).
     */
    public void publish(String topic, String consentId, Map<String, Object> data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", topic);
        envelope.put("timestamp", LocalDateTime.now().toString());
        envelope.put("consentId", consentId);
        envelope.putAll(data);

        if (kafkaTemplate.isPresent()) {
            kafkaTemplate.get().send(topic, consentId, envelope);
            log.info("Evento publicado: topic={}, consentId={}", topic, consentId);
        } else {
            log.warn("KafkaTemplate indisponível (perfil sem Kafka) — evento {} de consentId={} não enviado",
                    topic, consentId);
        }
    }
}
