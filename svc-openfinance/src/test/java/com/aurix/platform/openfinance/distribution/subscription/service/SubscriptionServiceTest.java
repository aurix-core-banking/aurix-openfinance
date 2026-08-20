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
    }

    private String hmacSha256Hex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
