package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Interface de atividade de publicação de dados.
 * Publica dados transformados para o plano de distribuição.
 */
@ActivityInterface
public interface PublishDataActivity {

    PublishResult publish(PublishRequest request);

    class PublishRequest {
        private String nodeId;
        private String resource;
        private String canonicalData;
        private String lineageId;
        private int recordCount;

        public PublishRequest() {
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

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getCanonicalData() {
            return canonicalData;
        }

        public void setCanonicalData(String canonicalData) {
            this.canonicalData = canonicalData;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }
    }

    class PublishResult {
        private String nodeId;
        private int publishedCount;
        private boolean success;
        private String errorMessage;

        public PublishResult() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public int getPublishedCount() {
            return publishedCount;
        }

        public void setPublishedCount(int publishedCount) {
            this.publishedCount = publishedCount;
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
