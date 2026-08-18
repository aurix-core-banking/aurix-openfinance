package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conta_consentida")
public class ContaConsentida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Column(nullable = false, length = 20)
    private String institutionCode;

    @Column(nullable = false, length = 3)
    private String moeda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConta tipoConta;

    @Column(nullable = false, length = 20)
    private String statusConta;

    private BigDecimal saldoDisponivel;

    private BigDecimal saldoAtual;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public enum TipoConta {
        CONTA_DE_PAGAMENTO, CONTA_DE_DEPOSITO, CONTA_POUPANCA, CONTA_SALARIO
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public TipoConta getTipoConta() { return tipoConta; }
    public void setTipoConta(TipoConta tipoConta) { this.tipoConta = tipoConta; }
    public String getStatusConta() { return statusConta; }
    public void setStatusConta(String statusConta) { this.statusConta = statusConta; }
    public BigDecimal getSaldoDisponivel() { return saldoDisponivel; }
    public void setSaldoDisponivel(BigDecimal saldoDisponivel) { this.saldoDisponivel = saldoDisponivel; }
    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
