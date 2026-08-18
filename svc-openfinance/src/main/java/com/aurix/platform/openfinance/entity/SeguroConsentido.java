package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seguro_consentido")
public class SeguroConsentido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String apoliceId;

    @Column(nullable = false, length = 64)
    private String clienteId;

    @Column(length = 50)
    private String tipoSeguro;

    @Column(length = 100)
    private String nomeSeguradora;

    private BigDecimal premioMensal;

    private BigDecimal premioTotal;

    private BigDecimal valorSegurado;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Column(length = 20)
    private String statusApolice;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getApoliceId() { return apoliceId; }
    public void setApoliceId(String apoliceId) { this.apoliceId = apoliceId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(String tipoSeguro) { this.tipoSeguro = tipoSeguro; }
    public String getNomeSeguradora() { return nomeSeguradora; }
    public void setNomeSeguradora(String nomeSeguradora) { this.nomeSeguradora = nomeSeguradora; }
    public BigDecimal getPremioMensal() { return premioMensal; }
    public void setPremioMensal(BigDecimal premioMensal) { this.premioMensal = premioMensal; }
    public BigDecimal getPremioTotal() { return premioTotal; }
    public void setPremioTotal(BigDecimal premioTotal) { this.premioTotal = premioTotal; }
    public BigDecimal getValorSegurado() { return valorSegurado; }
    public void setValorSegurado(BigDecimal valorSegurado) { this.valorSegurado = valorSegurado; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getStatusApolice() { return statusApolice; }
    public void setStatusApolice(String statusApolice) { this.statusApolice = statusApolice; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
