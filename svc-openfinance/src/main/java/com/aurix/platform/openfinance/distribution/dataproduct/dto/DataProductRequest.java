package com.aurix.platform.openfinance.distribution.dataproduct.dto;

/**
 * Request para criacao de produto de dado.
 */
public class DataProductRequest {

    private String name;
    private String description;
    private String domain;
    private String resourceType;
    private String format;
    private String schema;
    private String endpoint;

    public DataProductRequest() {
    }

    public DataProductRequest(String name, String description, String domain,
                               String resourceType, String format, String schema,
                               String endpoint) {
        this.name = name;
        this.description = description;
        this.domain = domain;
        this.resourceType = resourceType;
        this.format = format;
        this.schema = schema;
        this.endpoint = endpoint;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
