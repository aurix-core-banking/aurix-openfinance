package com.aurix.platform.openfinance.event;

import java.util.Map;

/**
 * Evento Spring publicado quando {@code data.published.v1} é emitido pela atividade
 * de publicação, permitindo que {@code DataPublishedEventListener} materialize
 * registros em {@code DataProductService} mesmo em perfis sem broker Kafka ativo.
 */
public record DataPublishedEvent(Map<String, Object> envelope) {
}
