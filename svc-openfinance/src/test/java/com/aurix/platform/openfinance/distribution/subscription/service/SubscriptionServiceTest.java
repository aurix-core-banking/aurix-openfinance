package com.aurix.platform.openfinance.distribution.subscription.service;

import com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest;
import com.aurix.platform.openfinance.distribution.subscription.repository.SubscriptionRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que a entrega de webhook faz um POST HTTP real contra o callbackUrl —
 * antes disso deliverWebhook() só logava "enviado" sem nenhuma requisição de
 * verdade, então o retry nunca podia disparar (nada falhava).
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

    @BeforeEach
    void setUp() throws IOException {
        subscriptionRepository.deleteAll();
        received = new CountDownLatch(1);

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/webhook", exchange -> {
            receivedBody = new String(exchange.getRequestBody().readAllBytes());
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
    void deveEntregarWebhookViaPostHttpReal() throws InterruptedException {
        int port = server.getAddress().getPort();
        String callbackUrl = "http://localhost:" + port + "/webhook";

        var subscription = subscriptionService.subscribe(new SubscriptionRequest(
                "participant-1", "product-1", callbackUrl, "*", 30));

        subscriptionService.notifySubscribers("product-1", "data.published.v1",
                java.util.Map.of("recordId", "abc-123"));

        assertTrue(received.await(5, TimeUnit.SECONDS), "webhook deveria ter sido recebido");
        assertTrue(receivedBody.contains("data.published.v1"));
        assertTrue(receivedBody.contains(subscription.getSubscriptionId()));
    }
}
