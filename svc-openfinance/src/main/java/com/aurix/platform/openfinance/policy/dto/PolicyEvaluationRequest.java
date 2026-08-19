package com.aurix.platform.openfinance.policy.dto;

import java.util.List;
import java.util.Map;

public class PolicyEvaluationRequest {

    private String consentId;
    private String resourceId;
    private String permission;
    private String purpose;
    private String subject;
    private String tokenThumbprint;
    private Map<String, String> context;

    public PolicyEvaluationRequest() {
    }

    public PolicyEvaluationRequest(String consentId, String resourceId, String permission,
                                   String purpose, String subject, String tokenThumbprint,
                                   Map<String, String> context) {
        this.consentId = consentId;
        this.resourceId = resourceId;
        this.permission = permission;
        this.purpose = purpose;
        this.subject = subject;
        this.tokenThumbprint = tokenThumbprint;
        this.context = context;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTokenThumbprint() {
        return tokenThumbprint;
    }

    public void setTokenThumbprint(String tokenThumbprint) {
        this.tokenThumbprint = tokenThumbprint;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }
}
