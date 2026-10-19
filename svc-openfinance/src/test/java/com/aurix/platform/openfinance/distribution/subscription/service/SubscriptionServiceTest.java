package com.aurix.platform.openfinance.distribution.subscription.service;

import com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest;
import com.aurix.platform.openfinance.distribution.subscription.entity.Subscription;
import com.aurix.platform.openfinance.distribution.subscription.repository.SubscriptionRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que a entrega de webhook faz um POST HTTP real assinado com
 * HMAC-SHA256 sobre timestamp+corpo — antes disso deliverWebhook() só logava
 * "enviado" sem requisição nem assinatura nenhuma, então qualquer um que
 * descobrisse a callbackUrl podia forjar uma notificação.
 */
@SpringBootTest
@ActiveProfiles("test")
class SubscriptionServiceTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService dataProductService;

    @Autowired
    private com.aurix.platform.openfinance.observability.OpenFinanceMetricsService metricsService;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private HttpServer server;
    private CountDownLatch received;
    private volatile String receivedBody;
    private volatile String receivedSignature;
    private volatile String receivedTimestamp;

    @BeforeEach
    void setUp() throws IOException {
        subscriptionRepository.deleteAll();
        received = new CountDownLatch(1);

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/webhook", exchange -> {
            receivedBody = new String(exchange.getRequestBody().readAllBytes());
            receivedSignature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
            receivedTimestamp = exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            received.countDown();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void deveEntregarWebhookAssinadoComHmacValidoEComTimestamp() throws Exception {
        int port = server.getAddress().getPort();
        String callbackUrl = "http://localhost:" + port + "/webhook";

        Subscription subscription = subscriptionService.subscribe(new SubscriptionRequest(
                "participant-1", "product-1", callbackUrl, "*", 30));

        assertEquals(1, subscriptionService.listAll().size(),
                "listAll() deve retornar todas as assinaturas cadastradas");

        double entregasAntes = metricsService.getMeterRegistry()
                .counter("openfinance_webhook_deliveries_total", "event_type", "data.published.v1", "outcome", "SUCCESS")
                .count();

        subscriptionService.notifySubscribers("product-1", "data.published.v1",
                java.util.Map.of("recordId", "abc-123"));

        assertTrue(received.await(5, TimeUnit.SECONDS), "webhook deveria ter sido recebido");
        assertTrue(receivedBody.contains("data.published.v1"));
        assertTrue(receivedBody.contains(subscription.getSubscriptionId()));

        assertNotEquals(null, receivedTimestamp);
        String assinaturaEsperada = hmacSha256Hex(
                subscription.getWebhookSecret(), receivedTimestamp + "." + receivedBody);
        assertEquals(assinaturaEsperada, receivedSignature,
                "a assinatura recebida deveria bater com HMAC-SHA256(secret, timestamp.body)");

        double entregasDepois = metricsService.getMeterRegistry()
                .counter("openfinance_webhook_deliveries_total", "event_type", "data.published.v1", "outcome", "SUCCESS")
                .count();
        assertEquals(entregasAntes + 1.0, entregasDepois, 0.001,
                "deve incrementar openfinance_webhook_deliveries_total com outcome=SUCCESS");
    }

    @Test
    void deveEntregarWebhookAutomaticamenteAoMaterializarDataProductOuRevogarConsentimento() throws Exception {
        int port = server.getAddress().getPort();
        String callbackUrl = "http://localhost:" + port + "/webhook";

        var product = dataProductService.create(
                new com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest(
                        "Contas Webhook", "Produto de contas", "contas-wh", "CONTA", "json", "{}", "/products/contas-wh"));

        Subscription subscription = subscriptionService.subscribe(new SubscriptionRequest(
                "participant-wh", product.getProductId(), callbackUrl, "data.published.v1,consent.revoked.v1", 30));

        var record = com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord.criar(
                "can-wh-1", "raw-wh-1", com.aurix.platform.openfinance.pipeline.ResourceType.CONTA,
                "{\"accountId\":\"acc-wh-1\"}", "1.0", "0".repeat(64));

        dataProductService.materialize(product, java.util.List.of(record));

        assertTrue(received.await(5, TimeUnit.SECONDS),
                "materialize() direto deveria disparar entrega HTTP de webhook para assinante ativo");
        assertTrue(receivedBody.contains("data.published.v1"));
        assertTrue(receivedBody.contains(product.getProductId()));

        received = new CountDownLatch(1);
        eventPublisher.publishEvent(new com.aurix.platform.openfinance.event.ConsentTerminatedEvent(
                "consent.revoked.v1", "consent-wh-99", "REVOGADO_PELO_CLIENTE",
                java.util.Map.of("eventType", "consent.revoked.v1", "consentId", "consent-wh-99")));

        assertTrue(received.await(5, TimeUnit.SECONDS),
                "ConsentTerminatedEvent (consent.revoked.v1) deveria notificar assinantes ativos via webhook HTTP");
        assertTrue(receivedBody.contains("consent.revoked.v1"));
        assertTrue(receivedBody.contains("consent-wh-99"));
        assertEquals(
                hmacSha256Hex(subscription.getWebhookSecret(), receivedTimestamp + "." + receivedBody),
                receivedSignature);
    }

    private String hmacSha256Hex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
