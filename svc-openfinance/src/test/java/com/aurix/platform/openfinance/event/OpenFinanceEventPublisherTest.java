package com.aurix.platform.openfinance.event;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Prova, sem subir Spring context, que os producers de consent.x e reconciliation.x
 * (que antes desta classe simplesmente não existiam) publicam no tópico certo com
 * consentId como chave — e que a ausência de KafkaTemplate (perfil sem Kafka) não
 * quebra a chamada, só vira um no-op logado.
 */
class OpenFinanceEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void devePublicarConsentGrantedNoTopicoCertoComConsentIdComoChave() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        OpenFinanceEventPublisher publisher = new OpenFinanceEventPublisher(Optional.of(kafkaTemplate));

        publisher.publishConsentGranted("consent-123", "client-abc");

        verify(kafkaTemplate).send(eq("consent.granted.v1"), eq("consent-123"), any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void devePublicarConsentRevokedComMotivoNoPayload() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        OpenFinanceEventPublisher publisher = new OpenFinanceEventPublisher(Optional.of(kafkaTemplate));

        publisher.publishConsentRevoked("consent-456", "Cliente solicitou");

        verify(kafkaTemplate).send(eq("consent.revoked.v1"), eq("consent-456"), any(Map.class));
    }

    @Test
    void naoDeveFalharQuandoKafkaTemplateAusente() {
        OpenFinanceEventPublisher publisher = new OpenFinanceEventPublisher(Optional.empty());

        // Não deve lançar exceção — só loga e segue.
        publisher.publishConsentExpired("consent-789");
    }

    @Test
    @SuppressWarnings("unchecked")
    void naoDevePublicarQuandoKafkaTemplateAusente() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        OpenFinanceEventPublisher publisher = new OpenFinanceEventPublisher(Optional.empty());

        publisher.publishReconciliationTriggered("plan-1", "plan-1");

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Map.class));
    }
}
