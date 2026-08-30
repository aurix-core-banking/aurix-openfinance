package com.aurix.platform.openfinance.trust.receipt.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Recibo de consentimento — artefato imutável e verificável que prova o que
 * exatamente foi consentido e quando, no sentido criptográfico (não-repúdio).
 *
 * <p>Gerado na aprovação do consentimento ({@code ConsentimentoService.aprovar()}),
 * assinado via {@code SigningService} e persistido de forma imutável (sem setters
 * após criação), fechando a cadeia de rastreabilidade do architecture.yaml desde
 * o primeiro elo (consentimento), não só a partir do AuthorizedContext.
 *
 * <p>Referência: issue #1 (Trust Plane / WP2) — formalização do consentimento.
 */
@Entity
@Table(name = "consent_receipts")
public class ConsentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String receiptId;

    @Column(nullable = false, unique = true, length = 64)
    private String consentId;

    @Column(nullable = false)
    private String contentHash;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 2000)
    private String signature;

    @Column(nullable = false, length = 30)
    private String signingAlgorithm;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ConsentReceipt() {
    }

    public ConsentReceipt(String receiptId, String consentId, String contentHash,
                          LocalDateTime timestamp, String signature, String signingAlgorithm) {
        this.receiptId = receiptId;
        this.consentId = consentId;
        this.contentHash = contentHash;
        this.timestamp = timestamp;
        this.signature = signature;
        this.signingAlgorithm = signingAlgorithm;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getConsentId() {
        return consentId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsentReceipt that = (ConsentReceipt) o;
        return Objects.equals(receiptId, that.receiptId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptId);
    }
}
