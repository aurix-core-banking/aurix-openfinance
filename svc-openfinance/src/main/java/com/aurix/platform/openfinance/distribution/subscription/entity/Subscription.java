package com.aurix.platform.openfinance.distribution.subscription.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Assinatura de participante para produtos de dado.
 * Permite notificacao via webhook quando dados mudam.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String subscriptionId;

    @Column(nullable = false)
    private String participantId;

    @Column(nullable = false)
    private String dataProductId;

    @Column(nullable = false)
    private String callbackUrl;

    @Column(nullable = false, length = 4000)
    private String events;

    /**
     * Segredo usado para assinar (HMAC-SHA256) o corpo de cada webhook entregue —
     * gerado uma vez na criação, nunca reexposto após isso (só no momento da
     * criação/rotação). O receptor usa o mesmo segredo para validar
     * X-Webhook-Signature e confirmar origem + integridade da notificação.
     */
    @Column(nullable = false, length = 100)
    private String webhookSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime updatedAt;

    public Subscription() {
    }

    public Subscription(String subscriptionId, String participantId, String dataProductId,
                        String callbackUrl, String events, LocalDateTime expiresAt,
                        String webhookSecret) {
        this.subscriptionId = subscriptionId;
        this.participantId = participantId;
        this.dataProductId = dataProductId;
        this.callbackUrl = callbackUrl;
        this.events = events;
        this.webhookSecret = webhookSecret;
        this.status = SubscriptionStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    /**
     * Gera um novo segredo, invalidando o anterior — usado na criação e em rotação.
     */
    public void rotateSecret(String newSecret) {
        this.webhookSecret = newSecret;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE && !isExpired();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getDataProductId() {
        return dataProductId;
    }

    public void setDataProductId(String dataProductId) {
        this.dataProductId = dataProductId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId);
    }

    @Override
    public String toString() {
        return "Subscription{subscriptionId='" + subscriptionId + "', participantId='"
                + participantId + "', dataProductId='" + dataProductId + "', status=" + status + "}";
    }
}
