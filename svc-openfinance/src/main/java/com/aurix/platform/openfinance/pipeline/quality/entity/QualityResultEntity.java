package com.aurix.platform.openfinance.pipeline.quality.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa o resultado da verificação de qualidade de um registro canônico.
 * Persiste na tabela aurix_openfinance.quality_results (V6).
 */
@Entity
@Table(name = "quality_results", schema = "aurix_openfinance")
public class QualityResultEntity {

    private static final int CANONICAL_RECORD_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_record_id", nullable = false, length = CANONICAL_RECORD_ID_LENGTH)
    private String canonicalRecordId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    protected QualityResultEntity() {
    }

    public QualityResultEntity(final String canonicalRecordId, final int score,
            final boolean passed, final LocalDateTime checkedAt) {
        this.canonicalRecordId = canonicalRecordId;
        this.score = score;
        this.passed = passed;
        this.checkedAt = checkedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalRecordId() {
        return canonicalRecordId;
    }

    public int getScore() {
        return score;
    }

    public boolean isPassed() {
        return passed;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
}
