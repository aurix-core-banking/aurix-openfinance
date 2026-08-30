package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Versão imutável (append-only) do ciclo de vida de um consentimento — INV-006.
 *
 * <p>Registra cada transição de estado como uma linha nova: status anterior nunca
 * é mutado, apenas emanado. {@code Consentimento} mantém o apontador do estado
 * corrente (projeção); esta tabela é a fonte de verdade do histórico auditável:
 * criação (versão 1), aprovação, rejeição, revogação e expiração.
 */
@Entity
@Table(name = "consentimento_versao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_consent_versao",
                columnNames = {"consent_id", "versao"}))
public class ConsentimentoVersao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false)
    private int versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Consentimento.StatusConsentimento status;

    @Column(nullable = false)
    private LocalDateTime dataEvento;

    private LocalDateTime dataAprovacao;

    private String motivo;

    public ConsentimentoVersao() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public int getVersao() {
        return versao;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public Consentimento.StatusConsentimento getStatus() {
        return status;
    }

    public void setStatus(Consentimento.StatusConsentimento status) {
        this.status = status;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDateTime dataEvento) {
        this.dataEvento = dataEvento;
    }

    public LocalDateTime getDataAprovacao() {
        return dataAprovacao;
    }

    public void setDataAprovacao(LocalDateTime dataAprovacao) {
        this.dataAprovacao = dataAprovacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}