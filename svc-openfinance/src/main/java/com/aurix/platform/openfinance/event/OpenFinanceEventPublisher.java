package com.aurix.platform.openfinance.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Publica os eventos que o próprio módulo define em architecture/events.yaml e que,
 * antes desta classe, nenhum serviço emitia — só havia consumers de eventos de
 * core-banking, nunca producers dos tópicos {@code consent.*}/{@code data.published.v1}
 * que o Open Finance é dono.
 *
 * <p>Todos os eventos são envelopados com os 8 metadados obrigatórios do catálogo
 * (eventId, eventType, timestamp, correlationId, causationId, consentId, executionId,
 * schemaVersion) via {@link EventEnvelope}.
 *
 * <p>{@code KafkaTemplate} é opcional porque {@code KafkaConfig} (aurix-shared) é
 * {@code @Profile("!test")} — publicar vira um no-op logado em vez de exigir um
 * broker Kafka rodando para os testes existentes.
 */
@Component
public class OpenFinanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceEventPublisher.class);

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OpenFinanceEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate) {
        this(kafkaTemplate, null);
    }

    @Autowired
    public OpenFinanceEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishConsentGranted(String consentId, String clientId) {
        String safeClient = clientId != null ? clientId : "unknown";
        Map<String, Object> envelope = publish("consent.granted.v1", consentId, null, null,
                Map.of("clientId", safeClient));
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new ConsentGrantedEvent(consentId, safeClient, envelope));
        }
    }

    public void publishConsentUpdated(String consentId, String reason) {
        publish("consent.updated.v1", consentId, null, null, Map.of("reason", reason));
    }

    public void publishConsentRevoked(String consentId, String reason) {
        String safeReason = reason != null ? reason : "Consentimento revogado";
        Map<String, Object> envelope = publish("consent.revoked.v1", consentId, null, null,
                Map.of("reason", safeReason));
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(
                    new ConsentTerminatedEvent("consent.revoked.v1", consentId, safeReason, envelope));
        }
    }

    public void publishConsentExpired(String consentId) {
        String reason = "Consentimento expirado";
        Map<String, Object> envelope = publish("consent.expired.v1", consentId, null, null, Map.of());
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(
                    new ConsentTerminatedEvent("consent.expired.v1", consentId, reason, envelope));
        }
    }

    public void publishReconciliationTriggered(String consentId, String executionPlanId) {
        publish("reconciliation.triggered.v1", consentId, executionPlanId, null,
                Map.of("executionPlanId", executionPlanId));
    }

    public void publishReconciliationDivergenceDetected(String consentId, String executionPlanId,
                                                          String details) {
        publish("reconciliation.divergence-detected.v1", consentId, executionPlanId, null,
                Map.of("executionPlanId", executionPlanId, "details", details));
    }

    public void publishReconciliationRepaired(String consentId, String executionPlanId) {
        publish("reconciliation.repaired.v1", consentId, executionPlanId, null,
                Map.of("executionPlanId", executionPlanId));
    }

    public Map<String, Object> publishTokenIssued(String consentId, String tokenId, String dpopThumbprint) {
        return publish("token.issued.v1", consentId, null, null, Map.of(
                "tokenId", tokenId != null ? tokenId : "unknown",
                "dpopThumbprint", dpopThumbprint != null ? dpopThumbprint : ""));
    }

    public Map<String, Object> publishTokenRevoked(String consentId, String tokenId, String reason) {
        return publish("token.revoked.v1", consentId, null, null, Map.of(
                "tokenId", tokenId != null ? tokenId : "unknown",
                "reason", reason != null ? reason : "Token revogado"));
    }

    /**
     * Publica um evento genérico no tópico dado, envelopado com os 8 metadados
     * obrigatórios do catálogo (via {@link EventEnvelope}).
     *
     * @param topic         tipo/assunto do evento.
     * @param consentId     ID do consentimento relacionado.
     * @param executionId   ID da execução relacionada (ex: executionPlanId).
     * @param causationId   ID do evento causador — opcional.
     * @param data          payload específico do evento.
     * @return envelope publicado.
     */
    public Map<String, Object> publish(String topic, String consentId, String executionId,
            String causationId, Map<String, Object> data) {
        Map<String, Object> envelope = EventEnvelope.criar(topic, consentId, executionId,
                null, causationId, data);

        if (kafkaTemplate.isPresent()) {
            kafkaTemplate.get().send(topic, consentId, envelope);
            log.info("Evento publicado: topic={}, consentId={}, executionId={}",
                    topic, consentId, executionId);
        } else {
            log.warn("KafkaTemplate indisponível (perfil sem Kafka) — evento {} de consentId={} não enviado",
                    topic, consentId);
        }
        return envelope;
    }
}
