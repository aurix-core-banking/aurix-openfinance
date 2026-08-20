package com.aurix.platform.openfinance.distribution.dataproduct.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Materialização de um registro canônico dentro de um produto de dado —
 * substituto pragmático de ClickHouse/MinIO para o ambiente dev/local
 * (ver plano de correção / ADR pendente sobre a extensão futura).
 */
@Entity
@Table(name = "data_product_records")
public class DataProductRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "canonical_record_id", nullable = false)
    private String canonicalRecordId;

    @Column(name = "canonical_data", nullable = false, columnDefinition = "jsonb")
    private String canonicalData;

    @Column(name = "materialized_at", nullable = false)
    private LocalDateTime materializedAt;

    public DataProductRecord() {
    }

    public DataProductRecord(String productId, String canonicalRecordId, String canonicalData) {
        this.productId = productId;
        this.canonicalRecordId = canonicalRecordId;
        this.canonicalData = canonicalData;
        this.materializedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getCanonicalRecordId() {
        return canonicalRecordId;
    }

    public String getCanonicalData() {
        return canonicalData;
    }

    public LocalDateTime getMaterializedAt() {
        return materializedAt;
    }
}
