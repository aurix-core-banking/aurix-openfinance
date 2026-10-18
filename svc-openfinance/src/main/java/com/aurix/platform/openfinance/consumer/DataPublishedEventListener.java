package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService;
import com.aurix.platform.openfinance.distribution.subscription.service.SubscriptionService;
import com.aurix.platform.openfinance.event.DataPublishedEvent;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consome o evento {@code data.published.v1} do event bus e conecta a distribuição:
 * encontra os produtos de dado do domínio/recurso publicado e notifica os assinantes
 * desses produtos via webhook — fechando o laço produtor (pipeline) → consumidores
 * (data-products, subscriptions) declarado em architecture/events.yaml.
 */
@Component
public class DataPublishedEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataPublishedEventListener.class);
    private static final String TOPIC_DATA_PUBLISHED = "data.published.v1";
    private static final String EVENT_TYPE = "data.published.v1";

    private final DataProductRepository dataProductRepository;
    private final SubscriptionService subscriptionService;
    private final DataProductService dataProductService;

    public DataPublishedEventListener(DataProductRepository dataProductRepository,
                                      SubscriptionService subscriptionService) {
        this(dataProductRepository, subscriptionService, null);
    }

    @Autowired
    public DataPublishedEventListener(DataProductRepository dataProductRepository,
                                      SubscriptionService subscriptionService,
                                      DataProductService dataProductService) {
        this.dataProductRepository = dataProductRepository;
        this.subscriptionService = subscriptionService;
        this.dataProductService = dataProductService;
    }

    @EventListener
    public void onDataPublishedSpringEvent(DataPublishedEvent event) {
        if (event != null && event.envelope() != null) {
            onDataPublished(event.envelope());
        }
    }

    /**
     * Processa um evento de publicação. O envelope pode chegar como Map (Kafka JSON
     * deserializado) contendo os metadados obrigatórios e o recurso publicado.
     *
     * @param envelope mapa do evento.
     */
    @KafkaListener(topics = TOPIC_DATA_PUBLISHED, groupId = "aurix-openfinance-data-published-group")
    public void onDataPublished(Map<String, Object> envelope) {
        Object resourceObj = envelope.get("resource");
        if (resourceObj == null) {
            log.warn("Evento data.published.v1 sem recurso — ignorando");
            return;
        }
        String resource = resourceObj.toString();
        String consentId = String.valueOf(envelope.get("consentId"));

        List<DataProduct> produtos = dataProductRepository.findByDomain(resource);
        if (produtos.isEmpty()) {
            log.debug("Nenhum produto de dado no domínio {} para notificar", resource);
            return;
        }

        Object canonicalDataObj = envelope.get("canonicalData");
        String canonicalData = canonicalDataObj != null ? canonicalDataObj.toString() : "";
        Object pubIdObj = envelope.get("publicationId");
        String recordId = pubIdObj != null ? pubIdObj.toString() : UUID.randomUUID().toString();

        for (DataProduct produto : produtos) {
            if (dataProductService != null && !canonicalData.isBlank()) {
                CanonicalRecord record = new CanonicalRecord();
                record.setCanonicalId(recordId);
                record.setCanonicalData(canonicalData);
                dataProductService.materialize(produto, List.of(record));
            }
            subscriptionService.notifySubscribers(produto.getProductId(), EVENT_TYPE, envelope);
        }

        log.info("Distribuição conectada: recurso={}, consentId={}, produtos={}",
                resource, consentId, produtos.size());
    }
}
