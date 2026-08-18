package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContaResponse {

    private String accountId;
    private String institutionCode;
    private String moeda;
    private String tipoConta;
    private String statusConta;
    private BigDecimal saldoDisponivel;
    private BigDecimal saldoAtual;
    private LocalDateTime dataAtualizacao;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getTipoConta() { return tipoConta; }
    public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }
    public String getStatusConta() { return statusConta; }
    public void setStatusConta(String statusConta) { this.statusConta = statusConta; }
    public BigDecimal getSaldoDisponivel() { return saldoDisponivel; }
    public void setSaldoDisponivel(BigDecimal saldoDisponivel) { this.saldoDisponivel = saldoDisponivel; }
    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
