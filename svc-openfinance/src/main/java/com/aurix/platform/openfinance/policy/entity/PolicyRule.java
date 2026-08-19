package com.aurix.platform.openfinance.policy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "policy_rules")
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleCode;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyRuleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyRuleSeverity severity;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int priority;

    @Column(length = 4000)
    private String expression;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public PolicyRule() {
    }

    public PolicyRule(String ruleCode, String ruleName, PolicyRuleType type,
                      PolicyRuleSeverity severity, String expression, int priority) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.type = type;
        this.severity = severity;
        this.expression = expression;
        this.priority = priority;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PolicyRuleType getType() {
        return type;
    }

    public void setType(PolicyRuleType type) {
        this.type = type;
    }

    public PolicyRuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(PolicyRuleSeverity severity) {
        this.severity = severity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyRule that = (PolicyRule) o;
        return Objects.equals(ruleCode, that.ruleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleCode);
    }

    @Override
    public String toString() {
        return "PolicyRule{ruleCode='" + ruleCode + "', type=" + type
                + ", severity=" + severity + ", priority=" + priority + "}";
    }
}
