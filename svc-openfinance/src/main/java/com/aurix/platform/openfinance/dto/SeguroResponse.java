package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SeguroResponse {
    private String apoliceId;
    private String tipoSeguro;
    private String nomeSeguradora;
    private BigDecimal premioMensal;
    private BigDecimal premioTotal;
    private BigDecimal valorSegurado;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String statusApolice;
    public String getApoliceId() { return apoliceId; }
    public void setApoliceId(String apoliceId) { this.apoliceId = apoliceId; }
    public String getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(String v) { this.tipoSeguro = v; }
    public String getNomeSeguradora() { return nomeSeguradora; }
    public void setNomeSeguradora(String v) { this.nomeSeguradora = v; }
    public BigDecimal getPremioMensal() { return premioMensal; }
    public void setPremioMensal(BigDecimal v) { this.premioMensal = v; }
    public BigDecimal getPremioTotal() { return premioTotal; }
    public void setPremioTotal(BigDecimal v) { this.premioTotal = v; }
    public BigDecimal getValorSegurado() { return valorSegurado; }
    public void setValorSegurado(BigDecimal v) { this.valorSegurado = v; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate v) { this.dataInicio = v; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate v) { this.dataFim = v; }
    public String getStatusApolice() { return statusApolice; }
    public void setStatusApolice(String v) { this.statusApolice = v; }
}
