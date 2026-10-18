package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService;
import com.aurix.platform.openfinance.distribution.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Prova que a distribuição está conectada ao event bus (Gap 7 + Gap D): ao receber um
 * evento {@code data.published.v1}, o listener encontra os produtos de dado do
 * domínio/recurso publicado, materializa o registro canônico em cada produto e
 * notifica os assinantes via webhook.
 */
class DataPublishedEventListenerTest {

    @Test
    void deveNotificarAssinantesDosProdutosDoDominioPublicado() {
        DataProductRepository repository = mock(DataProductRepository.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);

        DataProduct produto1 = new DataProduct("prod-1", "Contas", "Contas", "contas",
                "CONTA", "json", "{}", "/contas");
        DataProduct produto2 = new DataProduct("prod-2", "Transações", "Transações", "contas",
                "TRANSACAO", "json", "{}", "/transacoes");
        when(repository.findByDomain("contas")).thenReturn(List.of(produto1, produto2));

        DataPublishedEventListener listener = new DataPublishedEventListener(repository, subscriptionService);

        Map<String, Object> envelope = Map.of(
                "eventType", "data.published.v1",
                "resource", "contas",
                "consentId", "consent-123",
                "publicationId", "pub-1");

        listener.onDataPublished(envelope);

        verify(subscriptionService).notifySubscribers("prod-1", "data.published.v1", envelope);
        verify(subscriptionService).notifySubscribers("prod-2", "data.published.v1", envelope);
    }

    @Test
    void deveMaterializarRegistroCanonicoNosProdutosDoDominioQuandoPresenteNoEvento() {
        DataProductRepository repository = mock(DataProductRepository.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        DataProductService dataProductService = mock(DataProductService.class);

        DataProduct produto = new DataProduct("prod-contas", "Contas", "Contas", "contas",
                "CONTA", "json", "{}", "/contas");
        when(repository.findByDomain("contas")).thenReturn(List.of(produto));

        DataPublishedEventListener listener = new DataPublishedEventListener(
                repository, subscriptionService, dataProductService);

        String canonicalJson = "{\"accountId\":\"acc-1\",\"agency\":\"0001\"}";
        Map<String, Object> envelope = Map.of(
                "eventType", "data.published.v1",
                "resource", "contas",
                "consentId", "consent-123",
                "publicationId", "pub-999",
                "canonicalData", canonicalJson);

        listener.onDataPublished(envelope);

        verify(dataProductService).materialize(eq(produto), argThat(records ->
                records.size() == 1
                        && "pub-999".equals(records.get(0).getCanonicalId())
                        && canonicalJson.equals(records.get(0).getCanonicalData())));
        verify(subscriptionService).notifySubscribers("prod-contas", "data.published.v1", envelope);
    }

    @Test
    void deveIgnorarEventoSemRecurso() {
        DataProductRepository repository = mock(DataProductRepository.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);

        DataPublishedEventListener listener = new DataPublishedEventListener(repository, subscriptionService);

        assertDoesNotThrow(() -> listener.onDataPublished(Map.of("eventType", "data.published.v1")));

        verifyNoInteractions(subscriptionService);
    }

    @Test
    void deveIgnorarQuandoNaoHaProdutoNoDominio() {
        DataProductRepository repository = mock(DataProductRepository.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);

        when(repository.findByDomain("pix")).thenReturn(List.of());

        DataPublishedEventListener listener = new DataPublishedEventListener(repository, subscriptionService);

        listener.onDataPublished(Map.of("resource", "pix", "consentId", "c1"));

        verify(repository).findByDomain("pix");
        verifyNoInteractions(subscriptionService);
    }
}
