package com.aurix.platform.openfinance.pipeline.quality.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA que representa um problema individual de qualidade encontrado.
 * Persiste na tabela aurix_openfinance.quality_issues (V6).
 */
@Entity
@Table(name = "quality_issues", schema = "aurix_openfinance")
public class QualityIssueEntity {

    private static final int RULE_ID_LENGTH = 50;
    private static final int RULE_NAME_LENGTH = 200;
    private static final int RULE_TYPE_LENGTH = 20;
    private static final int SEVERITY_LENGTH = 10;
    private static final int MESSAGE_LENGTH = 500;
    private static final int FIELD_PATH_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_result_id", nullable = false)
    private QualityResultEntity qualityResult;

    @Column(name = "rule_id", nullable = false, length = RULE_ID_LENGTH)
    private String ruleId;

    @Column(name = "rule_name", nullable = false, length = RULE_NAME_LENGTH)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = RULE_TYPE_LENGTH)
    private String ruleType;

    @Column(name = "severity", nullable = false, length = SEVERITY_LENGTH)
    private String severity;

    @Column(name = "message", nullable = false, length = MESSAGE_LENGTH)
    private String message;

    @Column(name = "field_path", length = FIELD_PATH_LENGTH)
    private String fieldPath;

    protected QualityIssueEntity() {
    }

    public QualityIssueEntity(final QualityResultEntity qualityResult, final String ruleId,
            final String ruleName, final String ruleType, final String severity,
            final String message, final String fieldPath) {
        this.qualityResult = qualityResult;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.severity = severity;
        this.message = message;
        this.fieldPath = fieldPath;
    }

    public Long getId() {
        return id;
    }

    public QualityResultEntity getQualityResult() {
        return qualityResult;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
