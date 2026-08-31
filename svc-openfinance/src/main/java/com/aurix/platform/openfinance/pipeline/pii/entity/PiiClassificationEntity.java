package com.aurix.platform.openfinance.pipeline.pii.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa a classificação e proteção de dados PII aplicadas a um registro canônico.
 * Persiste na tabela aurix_openfinance.pii_classifications (V6).
 */
@Entity
@Table(name = "pii_classifications", schema = "aurix_openfinance")
public class PiiClassificationEntity {

    private static final int CANONICAL_RECORD_ID_LENGTH = 64;
    private static final int SENSITIVITY_LEVEL_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_record_id", nullable = false, length = CANONICAL_RECORD_ID_LENGTH)
    private String canonicalRecordId;

    @Column(name = "sensitivity_level", nullable = false, length = SENSITIVITY_LEVEL_LENGTH)
    private String sensitivityLevel;

    @Column(name = "protected_fields", columnDefinition = "jsonb")
    private String protectedFields;

    @Column(name = "masked_record", columnDefinition = "jsonb")
    private String maskedRecord;

    @Column(name = "classified_at", nullable = false)
    private LocalDateTime classifiedAt;

    protected PiiClassificationEntity() {
    }

    public PiiClassificationEntity(final String canonicalRecordId, final String sensitivityLevel,
            final String protectedFields, final String maskedRecord, final LocalDateTime classifiedAt) {
        this.canonicalRecordId = canonicalRecordId;
        this.sensitivityLevel = sensitivityLevel;
        this.protectedFields = protectedFields;
        this.maskedRecord = maskedRecord;
        this.classifiedAt = classifiedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalRecordId() {
        return canonicalRecordId;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public String getProtectedFields() {
        return protectedFields;
    }

    public String getMaskedRecord() {
        return maskedRecord;
    }

    public LocalDateTime getClassifiedAt() {
        return classifiedAt;
    }
}
