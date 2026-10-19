package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Atividade Temporal para verificação de vigência de contexto autorizado de consentimento.
 * Respeita INV-004: não executa regras do Policy Engine; apenas consulta contextos
 * já autorizados e emitidos pelo Consent Plane.
 */
@ActivityInterface
public interface ValidateConsentActivity {

    ConsentValidationResult validateConsent(ConsentValidationRequest request);

    class ConsentValidationRequest {
        private String consentId;
        private String planId;
        private Integer expectedVersion;

        public ConsentValidationRequest() {
        }

        public ConsentValidationRequest(String consentId, String planId, Integer expectedVersion) {
            this.consentId = consentId;
            this.planId = planId;
            this.expectedVersion = expectedVersion;
        }

        public String getConsentId() {
            return consentId;
        }

        public void setConsentId(String consentId) {
            this.consentId = consentId;
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public Integer getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Integer expectedVersion) {
            this.expectedVersion = expectedVersion;
        }
    }

    class ConsentValidationResult {
        private String consentId;
        private boolean valid;
        private int activeContextsCount;
        private String reason;

        public ConsentValidationResult() {
        }

        public String getConsentId() {
            return consentId;
        }

        public void setConsentId(String consentId) {
            this.consentId = consentId;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public int getActiveContextsCount() {
            return activeContextsCount;
        }

        public void setActiveContextsCount(int activeContextsCount) {
            this.activeContextsCount = activeContextsCount;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
