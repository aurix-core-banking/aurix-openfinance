package com.aurix.platform.openfinance.extractor.dto;

import java.util.List;

/**
 * Capacidades de um extractor.
 */
public class ExtractorCapabilities {

    private String name;
    private String description;
    private List<ResourceType> supportedResourceTypes;
    private int maxBatchSize;

    public ExtractorCapabilities() {
    }

    public ExtractorCapabilities(String name, String description,
                                  List<ResourceType> supportedResourceTypes, int maxBatchSize) {
        this.name = name;
        this.description = description;
        this.supportedResourceTypes = supportedResourceTypes;
        this.maxBatchSize = maxBatchSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ResourceType> getSupportedResourceTypes() {
        return supportedResourceTypes;
    }

    public void setSupportedResourceTypes(List<ResourceType> supportedResourceTypes) {
        this.supportedResourceTypes = supportedResourceTypes;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public static class Builder {
        private String name;
        private String description;
        private List<ResourceType> supportedResourceTypes;
        private int maxBatchSize;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder supportedResourceTypes(List<ResourceType> supportedResourceTypes) {
            this.supportedResourceTypes = supportedResourceTypes;
            return this;
        }

        public Builder maxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        public ExtractorCapabilities build() {
            return new ExtractorCapabilities(name, description, supportedResourceTypes, maxBatchSize);
        }
    }
}
