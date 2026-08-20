package com.aurix.platform.openfinance.distribution.subscription.service;

import com.aurix.platform.openfinance.distribution.subscription.entity.Subscription;
import com.aurix.platform.openfinance.distribution.subscription.entity.SubscriptionStatus;
import com.aurix.platform.openfinance.distribution.subscription.repository.SubscriptionRepository;
import com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
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
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;

    private final SubscriptionRepository repository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public SubscriptionService(SubscriptionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
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
                LocalDateTime.now().plusDays(request.getDurationDays()),
                gerarSegredo()
        );

        Subscription saved = repository.save(subscription);
        log.info("Assinatura criada: {} para produto {}",
                subscriptionId, request.getDataProductId());
        return saved;
    }

    /**
     * Gera um novo segredo de assinatura, invalidando o anterior. O segredo só é
     * exposto no corpo desta resposta — não há como recuperá-lo depois, só rotacionar.
     */
    @Transactional
    public Subscription rotateSecret(String subscriptionId) {
        Subscription subscription = findBySubscriptionId(subscriptionId);
        subscription.rotateSecret(gerarSegredo());
        Subscription saved = repository.save(subscription);
        log.info("Segredo de webhook rotacionado: {}", subscriptionId);
        return saved;
    }

    private String gerarSegredo() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
     * Entrega webhook com retry — POST HTTP real via RestClient, corpo assinado
     * com HMAC-SHA256 (X-Webhook-Signature) sobre {@code timestamp + "." + corpo},
     * com o timestamp também no header (X-Webhook-Timestamp) para o receptor poder
     * rejeitar replay de mensagens antigas. Sem isso, qualquer um que descobrisse a
     * callbackUrl podia forjar uma notificação — o receptor não tinha como validar
     * origem nem integridade.
     */
    private void deliverWebhook(Subscription subscription, String eventType, Object payload) {
        int maxRetries = 3;
        int attempt = 0;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", eventType);
        body.put("subscriptionId", subscription.getSubscriptionId());
        body.put("dataProductId", subscription.getDataProductId());
        body.put("payload", payload);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Falha ao serializar corpo do webhook para {}: {}",
                    subscription.getCallbackUrl(), e.getMessage());
            return;
        }

        while (attempt < maxRetries) {
            try {
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String signature = assinar(subscription.getWebhookSecret(), timestamp, jsonBody);

                restClient.post()
                        .uri(subscription.getCallbackUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Signature", signature)
                        .body(jsonBody)
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

    /**
     * Assina {@code timestamp + "." + corpo} com HMAC-SHA256 usando o segredo da
     * assinatura — mesma convenção usada por Stripe/GitHub, para o receptor validar
     * que a notificação veio de nós e não foi alterada no caminho.
     */
    private String assinar(String secret, String timestamp, String jsonBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signed = mac.doFinal((timestamp + "." + jsonBody).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signed);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar webhook: " + e.getMessage(), e);
        }
    }
}
