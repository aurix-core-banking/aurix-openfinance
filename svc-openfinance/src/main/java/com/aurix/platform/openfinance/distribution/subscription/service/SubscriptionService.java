package com.aurix.platform.openfinance.distribution.subscription.service;

import com.aurix.platform.openfinance.distribution.subscription.entity.Subscription;
import com.aurix.platform.openfinance.distribution.subscription.entity.SubscriptionStatus;
import com.aurix.platform.openfinance.distribution.subscription.repository.SubscriptionRepository;
import com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servico de gestao de assinaturas.
 * Permite que participantes assinem produtos de dado
 * e recebam notificacoes via webhook.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int WEBHOOK_TIMEOUT_MS = 5000;

    private final SubscriptionRepository repository;
    private final RestClient restClient;

    public SubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(WEBHOOK_TIMEOUT_MS);
        requestFactory.setReadTimeout(WEBHOOK_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Cria uma nova assinatura.
     */
    @Transactional
    public Subscription subscribe(SubscriptionRequest request) {
        String subscriptionId = UUID.randomUUID().toString();

        Subscription subscription = new Subscription(
                subscriptionId,
                request.getParticipantId(),
                request.getDataProductId(),
                request.getCallbackUrl(),
                request.getEvents(),
                LocalDateTime.now().plusDays(request.getDurationDays())
        );

        Subscription saved = repository.save(subscription);
        log.info("Assinatura criada: {} para produto {}",
                subscriptionId, request.getDataProductId());
        return saved;
    }

    /**
     * Cancela uma assinatura.
     */
    @Transactional
    public void unsubscribe(String subscriptionId) {
        Subscription subscription = repository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assinatura nao encontrada: " + subscriptionId));

        subscription.cancel();
        repository.save(subscription);
        log.info("Assinatura cancelada: {}", subscriptionId);
    }

    /**
     * Notifica assinantes sobre mudanca em dados.
     * Implementa delivery de webhook com retry.
     */
    public void notifySubscribers(String dataProductId, String eventType, Object payload) {
        List<Subscription> activeSubscriptions = repository
                .findByDataProductIdAndStatus(dataProductId, SubscriptionStatus.ACTIVE);

        for (Subscription subscription : activeSubscriptions) {
            if (subscription.isExpired()) {
                continue;
            }

            if (!matchesEvent(subscription.getEvents(), eventType)) {
                continue;
            }

            deliverWebhook(subscription, eventType, payload);
        }
    }

    /**
     * Lista assinaturas por participante.
     */
    public List<Subscription> listByParticipant(String participantId) {
        return repository.findByParticipantId(participantId);
    }

    /**
     * Lista assinaturas por produto de dado.
     */
    public List<Subscription> listByDataProduct(String dataProductId) {
        return repository.findByDataProductId(dataProductId);
    }

    /**
     * Busca assinatura por ID.
     */
    public Subscription findBySubscriptionId(String subscriptionId) {
        return repository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assinatura nao encontrada: " + subscriptionId));
    }

    /**
     * Verifica se o tipo de evento corresponde aos eventos da assinatura.
     */
    private boolean matchesEvent(String events, String eventType) {
        if (events == null || events.isBlank()) {
            return true;
        }
        return events.contains("*") || events.contains(eventType);
    }

    /**
     * Entrega webhook com retry — POST HTTP real via RestClient. Antes disso o
     * "envio" era só um log.info sem nenhuma requisição de verdade, então o
     * retry nunca disparava (nada podia falhar).
     */
    private void deliverWebhook(Subscription subscription, String eventType, Object payload) {
        int maxRetries = 3;
        int attempt = 0;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", eventType);
        body.put("subscriptionId", subscription.getSubscriptionId());
        body.put("dataProductId", subscription.getDataProductId());
        body.put("payload", payload);

        while (attempt < maxRetries) {
            try {
                restClient.post()
                        .uri(subscription.getCallbackUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Webhook entregue para {} (tentativa {}): evento={}",
                        subscription.getCallbackUrl(), attempt + 1, eventType);
                return;
            } catch (RestClientException e) {
                attempt++;
                log.warn("Falha ao enviar webhook para {} (tentativa {}): {}",
                        subscription.getCallbackUrl(), attempt, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        log.error("Falha ao enviar webhook apos {} tentativas para {}",
                maxRetries, subscription.getCallbackUrl());
    }
}
