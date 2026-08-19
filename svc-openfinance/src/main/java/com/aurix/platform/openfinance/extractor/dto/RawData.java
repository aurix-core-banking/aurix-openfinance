package com.aurix.platform.openfinance.extractor.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Dados brutos extraidos por um extractor.
 */
public class RawData {

    private ResourceType resourceType;
    private String contextId;
    private Map<String, Object> payload;
    private int recordCount;
    private LocalDateTime extractedAt;

    public RawData() {
    }

    public RawData(ResourceType resourceType, String contextId, Map<String, Object> payload,
                   int recordCount, LocalDateTime extractedAt) {
        this.resourceType = resourceType;
        this.contextId = contextId;
        this.payload = payload;
        this.recordCount = recordCount;
        this.extractedAt = extractedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }

    public long getPayloadSize() {
        if (payload == null) return 0;
        return payload.toString().length();
    }

    public static class Builder {
        private ResourceType resourceType;
        private String contextId;
        private Map<String, Object> payload;
        private int recordCount;
        private LocalDateTime extractedAt;

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public Builder recordCount(int recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        public Builder extractedAt(LocalDateTime extractedAt) {
            this.extractedAt = extractedAt;
            return this;
        }

        public RawData build() {
            return new RawData(resourceType, contextId, payload, recordCount, extractedAt);
        }
    }
}
