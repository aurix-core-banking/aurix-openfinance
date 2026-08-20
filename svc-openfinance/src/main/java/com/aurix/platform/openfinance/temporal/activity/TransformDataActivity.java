package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Interface de atividade de transformação de dados.
 * Canonicalização + Validação de Schema + Qualidade de Dados.
 */
@ActivityInterface
public interface TransformDataActivity {

    TransformResult transform(TransformRequest request);

    class TransformRequest {
        private String nodeId;
        private String resource;
        private String consentId;
        private String executionPlanId;
        private String rawData;
        private int recordCount;

        public TransformRequest() {
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

        public String getExecutionPlanId() {
            return executionPlanId;
        }

        public void setExecutionPlanId(String executionPlanId) {
            this.executionPlanId = executionPlanId;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getRawData() {
            return rawData;
        }

        public void setRawData(String rawData) {
            this.rawData = rawData;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }
    }

    class TransformResult {
        private String nodeId;
        private int recordCount;
        private String canonicalData;
        private String lineageId;
        private boolean success;
        private String errorMessage;

        public TransformResult() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getLineageId() {
            return lineageId;
        }

        public void setLineageId(String lineageId) {
            this.lineageId = lineageId;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public String getCanonicalData() {
            return canonicalData;
        }

        public void setCanonicalData(String canonicalData) {
            this.canonicalData = canonicalData;
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
