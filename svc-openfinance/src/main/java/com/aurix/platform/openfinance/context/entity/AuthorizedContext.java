package com.aurix.platform.openfinance.context.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Contexto autorizado imutável — não pode ser modificado após criação (INV05).
 * Contém: subject, consentId, consentVersion, purpose, permissions,
 * resources, validUntil, signingAlgorithm, dpopThumbprint.
 */
@Entity
@Table(name = "authorized_contexts")
public class AuthorizedContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contextId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String consentId;

    @Column(nullable = false)
    private int consentVersion;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false, length = 4000)
    private String permissions;

    @Column(nullable = false, length = 8000)
    private String resourceGraph;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private String signingAlgorithm;

    @Column(nullable = false)
    private String dpopThumbprint;

    @Column(nullable = false, length = 4000)
    private String signature;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime revokedAt;

    protected AuthorizedContext() {
    }

    public AuthorizedContext(String contextId, String subject, String consentId,
                             int consentVersion, String purpose, String permissions,
                             String resourceGraph, LocalDateTime validUntil,
                             String signingAlgorithm, String dpopThumbprint,
                             String signature) {
        this.contextId = contextId;
        this.subject = subject;
        this.consentId = consentId;
        this.consentVersion = consentVersion;
        this.purpose = purpose;
        this.permissions = permissions;
        this.resourceGraph = resourceGraph;
        this.validUntil = validUntil;
        this.signingAlgorithm = signingAlgorithm;
        this.dpopThumbprint = dpopThumbprint;
        this.signature = signature;
        this.revoked = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.validUntil);
    }

    public boolean isActive() {
        return !this.revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    // Getters — sem setters para campos imutáveis

    public Long getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public String getSubject() {
        return subject;
    }

    public String getConsentId() {
        return consentId;
    }

    public int getConsentVersion() {
        return consentVersion;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getResourceGraph() {
        return resourceGraph;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public String getDpopThumbprint() {
        return dpopThumbprint;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizedContext that = (AuthorizedContext) o;
        return Objects.equals(contextId, that.contextId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId);
    }

    @Override
    public String toString() {
        return "AuthorizedContext{contextId='" + contextId + "', subject='" + subject
                + "', consentId='" + consentId + "', purpose='" + purpose + "'}";
    }
}
