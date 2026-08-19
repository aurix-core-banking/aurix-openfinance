package com.aurix.platform.openfinance.policy.dto;

import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import java.util.List;

public class PolicyDecisionResponse {

    private PolicyDecisionType decision;
    private String reason;
    private List<String> evaluatedRules;
    private int evaluationTimeMs;
    private String consentId;
    private String resourceId;
    private String permission;

    public PolicyDecisionResponse() {
    }

    public PolicyDecisionResponse(PolicyDecisionType decision, String reason,
                                  List<String> evaluatedRules, int evaluationTimeMs,
                                  String consentId, String resourceId, String permission) {
        this.decision = decision;
        this.reason = reason;
        this.evaluatedRules = evaluatedRules;
        this.evaluationTimeMs = evaluationTimeMs;
        this.consentId = consentId;
        this.resourceId = resourceId;
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

    public List<String> getEvaluatedRules() {
        return evaluatedRules;
    }

    public void setEvaluatedRules(List<String> evaluatedRules) {
        this.evaluatedRules = evaluatedRules;
    }

    public int getEvaluationTimeMs() {
        return evaluationTimeMs;
    }

    public void setEvaluationTimeMs(int evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
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
}
