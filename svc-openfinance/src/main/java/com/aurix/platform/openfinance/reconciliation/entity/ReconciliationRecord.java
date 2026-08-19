package com.aurix.platform.openfinance.reconciliation.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de reconciliação entre dados esperados e extraídos.
 */
@Entity
@Table(name = "reconciliation_records", schema = "aurix")
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reconciliationId;

    @Column(nullable = false)
    private String planId;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private int expectedCount;

    @Column(nullable = false)
    private int actualCount;

    @Column(length = 4000)
    private String divergencesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    private LocalDateTime dataReparo;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private Integer versao = 1;

    public ReconciliationRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(String reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public void setExpectedCount(int expectedCount) {
        this.expectedCount = expectedCount;
    }

    public int getActualCount() {
        return actualCount;
    }

    public void setActualCount(int actualCount) {
        this.actualCount = actualCount;
    }

    public String getDivergencesJson() {
        return divergencesJson;
    }

    public void setDivergencesJson(String divergencesJson) {
        this.divergencesJson = divergencesJson;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public LocalDateTime getDataReparo() {
        return dataReparo;
    }

    public void setDataReparo(LocalDateTime dataReparo) {
        this.dataReparo = dataReparo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Integer getVersao() {
        return versao;
    }

    public void setVersao(Integer versao) {
        this.versao = versao;
    }
}
