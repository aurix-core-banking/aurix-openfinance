package com.aurix.platform.openfinance.pipeline.validation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa o resultado da validação de schema de um registro canônico.
 * Persiste na tabela aurix_openfinance.validation_results (V6).
 */
@Entity
@Table(name = "validation_results", schema = "aurix_openfinance")
public class ValidationResultEntity {

    private static final int CANONICAL_RECORD_ID_LENGTH = 64;
    private static final int SCHEMA_VERSION_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_record_id", nullable = false, length = CANONICAL_RECORD_ID_LENGTH)
    private String canonicalRecordId;

    @Column(name = "schema_version", nullable = false, length = SCHEMA_VERSION_LENGTH)
    private String schemaVersion;

    @Column(name = "valid", nullable = false)
    private boolean valid;

    @Column(name = "errors", columnDefinition = "jsonb")
    private String errors;

    @Column(name = "warnings", columnDefinition = "jsonb")
    private String warnings;

    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt;

    protected ValidationResultEntity() {
    }

    public ValidationResultEntity(final String canonicalRecordId, final String schemaVersion,
            final boolean valid, final String errors, final String warnings,
            final LocalDateTime validatedAt) {
        this.canonicalRecordId = canonicalRecordId;
        this.schemaVersion = schemaVersion;
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
        this.validatedAt = validatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalRecordId() {
        return canonicalRecordId;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrors() {
        return errors;
    }

    public String getWarnings() {
        return warnings;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }
}
