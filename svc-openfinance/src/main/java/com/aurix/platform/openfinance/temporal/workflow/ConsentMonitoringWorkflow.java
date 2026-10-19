package com.aurix.platform.openfinance.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow Temporal de monitoramento contínuo de consentimento
 * declarado em {@code architecture/services.yaml} ({@code ConsentMonitoringWorkflow}).
 */
@WorkflowInterface
public interface ConsentMonitoringWorkflow {

    @WorkflowMethod
    MonitoringResult monitorar(MonitoringInput input);

    @SignalMethod
    void notificarRevogacao(String motivo);

    @QueryMethod
    MonitoringStatus getStatus();

    class MonitoringInput {
        private String consentId;
        private String planId;
        private Integer expectedVersion;

        public MonitoringInput() {
        }

        public MonitoringInput(String consentId, String planId, Integer expectedVersion) {
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

    class MonitoringResult {
        private String consentId;
        private String planId;
        private boolean consentValid;
        private boolean revoked;
        private String reason;

        public MonitoringResult() {
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

        public boolean isConsentValid() {
            return consentValid;
        }

        public void setConsentValid(boolean consentValid) {
            this.consentValid = consentValid;
        }

        public boolean isRevoked() {
            return revoked;
        }

        public void setRevoked(boolean revoked) {
            this.revoked = revoked;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    class MonitoringStatus {
        private String consentId;
        private boolean active;
        private boolean revoked;
        private String revocationReason;

        public MonitoringStatus() {
        }

        public String getConsentId() {
            return consentId;
        }

        public void setConsentId(String consentId) {
            this.consentId = consentId;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isRevoked() {
            return revoked;
        }

        public void setRevoked(boolean revoked) {
            this.revoked = revoked;
        }

        public String getRevocationReason() {
            return revocationReason;
        }

        public void setRevocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
        }
    }
}
