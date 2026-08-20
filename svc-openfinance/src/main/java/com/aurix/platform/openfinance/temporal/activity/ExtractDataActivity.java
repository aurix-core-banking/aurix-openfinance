package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Interface de atividade de extração de dados.
 * Extrai dados brutos da fonte (sistema legado, API externa, etc).
 */
@ActivityInterface
public interface ExtractDataActivity {

    ExtractResult extract(ExtractRequest request);

    class ExtractRequest {
        private String nodeId;
        private String resource;
        private String consentId;
        private String idempotencyKey;
        private int timeoutSeconds;

        public ExtractRequest() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getConsentId() {
            return consentId;
        }

        public void setConsentId(String consentId) {
            this.consentId = consentId;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    class ExtractResult {
        private String nodeId;
        private int recordCount;
        private String rawData;
        private boolean success;
        private String errorMessage;

        public ExtractResult() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public String getRawData() {
            return rawData;
        }

        public void setRawData(String rawData) {
            this.rawData = rawData;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
