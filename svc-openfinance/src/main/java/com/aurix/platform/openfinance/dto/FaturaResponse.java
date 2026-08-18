package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FaturaResponse {
    private String faturaId;
    private BigDecimal valorTotal;
    private BigDecimal valorMinimo;
    private BigDecimal valorPago;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    public String getFaturaId() { return faturaId; }
    public void setFaturaId(String faturaId) { this.faturaId = faturaId; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public BigDecimal getValorMinimo() { return valorMinimo; }
    public void setValorMinimo(BigDecimal valorMinimo) { this.valorMinimo = valorMinimo; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate data) { this.dataVencimento = data; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate data) { this.dataPagamento = data; }
}
