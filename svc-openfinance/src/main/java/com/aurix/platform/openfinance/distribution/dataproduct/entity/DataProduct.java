package com.aurix.platform.openfinance.distribution.dataproduct.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Produto de dado — unidade de distribuicao no Distribution Plane.
 * Contém metadados, schema e endpoint para servir dados.
 */
@Entity
@Table(name = "data_products")
public class DataProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String format;

    @Column(nullable = false, length = 8000)
    private String schema;

    @Column(nullable = false)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataProductStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public DataProduct() {
    }

    public DataProduct(String productId, String name, String description, String domain,
                       String resourceType, String format, String schema, String endpoint) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.domain = domain;
        this.resourceType = resourceType;
        this.format = format;
        this.schema = schema;
        this.endpoint = endpoint;
        this.status = DataProductStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = DataProductStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = DataProductStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == DataProductStatus.ACTIVE;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public DataProductStatus getStatus() {
        return status;
    }

    public void setStatus(DataProductStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataProduct that = (DataProduct) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "DataProduct{productId='" + productId + "', name='" + name
                + "', domain='" + domain + "', status=" + status + "}";
    }
}
