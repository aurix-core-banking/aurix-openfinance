package com.aurix.platform.openfinance.policy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "policy_decisions")
public class PolicyDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String consentId;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyDecisionType decision;

    @Column(length = 2000)
    private String reason;

    @Column(length = 4000)
    private String evaluatedRules;

    @Column(nullable = false)
    private int evaluationTimeMs;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(nullable = false)
    private String evaluatedBy;

    public PolicyDecision() {
    }

    public PolicyDecision(String consentId, String resourceId, String permission,
                          PolicyDecisionType decision, String reason,
                          String evaluatedRules, int evaluationTimeMs, String evaluatedBy) {
        this.consentId = consentId;
        this.resourceId = resourceId;
        this.permission = permission;
        this.decision = decision;
        this.reason = reason;
        this.evaluatedRules = evaluatedRules;
        this.evaluationTimeMs = evaluationTimeMs;
        this.evaluatedBy = evaluatedBy;
        this.evaluatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public PolicyDecisionType getDecision() {
        return decision;
    }

    public void setDecision(PolicyDecisionType decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvaluatedRules() {
        return evaluatedRules;
    }

    public void setEvaluatedRules(String evaluatedRules) {
        this.evaluatedRules = evaluatedRules;
    }

    public int getEvaluationTimeMs() {
        return evaluationTimeMs;
    }

    public void setEvaluationTimeMs(int evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getEvaluatedBy() {
        return evaluatedBy;
    }

    public void setEvaluatedBy(String evaluatedBy) {
        this.evaluatedBy = evaluatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyDecision that = (PolicyDecision) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PolicyDecision{consentId='" + consentId + "', resourceId='" + resourceId
                + "', decision=" + decision + ", evaluatedAt=" + evaluatedAt + "}";
    }
}
