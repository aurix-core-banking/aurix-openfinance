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
