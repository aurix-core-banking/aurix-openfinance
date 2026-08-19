package com.aurix.platform.openfinance.planner.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Plano de execução imutável — INV05 se aplica.
 * Representa o DAG completo de extração de dados para um consentimento.
 */
@Entity
@Table(name = "execution_plans", schema = "aurix")
public class ExecutionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String planId;

    @Column(nullable = false)
    private String consentId;

    @Column(nullable = false)
    private int consentVersion;

    @Column(nullable = false, length = 16000)
    private String dagDefinition;

    @Column(nullable = false, length = 4000)
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataExecucao;
    private LocalDateTime dataConclusao;

    @Column(nullable = false)
    private String participanteId;

    @Column(nullable = false)
    private Integer versao = 1;

    public ExecutionPlan() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public int getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(int consentVersion) {
        this.consentVersion = consentVersion;
    }

    public String getDagDefinition() {
        return dagDefinition;
    }

    public void setDagDefinition(String dagDefinition) {
        this.dagDefinition = dagDefinition;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataExecucao() {
        return dataExecucao;
    }

    public void setDataExecucao(LocalDateTime dataExecucao) {
        this.dataExecucao = dataExecucao;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public String getParticipanteId() {
        return participanteId;
    }

    public void setParticipanteId(String participanteId) {
        this.participanteId = participanteId;
    }

    public Integer getVersao() {
        return versao;
    }

    public void setVersao(Integer versao) {
        this.versao = versao;
    }
}
