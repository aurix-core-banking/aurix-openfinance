package com.aurix.platform.openfinance.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Metadados obrigatórios de todo evento do catálogo (architecture/events.yaml).
 *
 * <p>O catálogo exige 8 campos em todos os eventos: {@code eventId}, {@code eventType},
 * {@code timestamp}, {@code correlationId}, {@code causationId}, {@code consentId},
 * {@code executionId} e {@code schemaVersion}. Esta classe garante que o envelope de
 * todo evento emitido contenha esses 8 campos, gerando {@code eventId}/{@code timestamp}/
 * {@code schemaVersion} internamente quando não fornecidos.
 */
public final class EventEnvelope {

    /**
     * Versão de schema default dos eventos.
     */
    public static final String SCHEMA_VERSION = "1.0";

    /**
     * Lista dos campos de metadados obrigatórios exigidos em events.yaml.
     */
    public static final java.util.List<String> REQUIRED_METADATA = java.util.List.of(
            "eventId", "eventType", "timestamp", "correlationId", "causationId",
            "consentId", "executionId", "schemaVersion");

    private EventEnvelope() {
    }

    /**
     * Constrói um envelope de evento com todos os metadados obrigatórios.
     *
     * @param eventType     tipo do evento (ex: consent.granted.v1).
     * @param consentId     ID do consentimento relacionado.
     * @param executionId   ID da execução relacionada.
     * @param correlationId ID de correlação (rastreio) — gerado se null.
     * @param causationId   ID do evento que causou este — opcional.
     * @param payload       dados específicos do evento.
     * @return mapa do envelope com os 8 metadados + payload.
     */
    public static Map<String, Object> criar(final String eventType, final String consentId,
            final String executionId, final String correlationId, final String causationId,
            final Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("timestamp", LocalDateTime.now().toString());
        envelope.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        envelope.put("causationId", causationId);
        envelope.put("consentId", consentId);
        envelope.put("executionId", executionId);
        envelope.put("schemaVersion", SCHEMA_VERSION);
        if (payload != null) {
            envelope.putAll(payload);
        }
        return envelope;
    }
}
