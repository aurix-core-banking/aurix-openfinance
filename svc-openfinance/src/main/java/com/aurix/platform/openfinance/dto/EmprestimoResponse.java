package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmprestimoResponse {
    private String emprestimoId;
    private String tipoEmprestimo;
    private BigDecimal valorContratado;
    private BigDecimal valorSaldoDevedor;
    private BigDecimal taxaJuros;
    private Integer prazoMeses;
    private Integer parcelasPagas;
    private Integer parcelasRestantes;
    private BigDecimal valorParcela;
    private LocalDate dataContratacao;
    private LocalDate dataVencimentoPrimeiraParcela;
    private String statusEmprestimo;
    public String getEmprestimoId() { return emprestimoId; }
    public void setEmprestimoId(String emprestimoId) { this.emprestimoId = emprestimoId; }
    public String getTipoEmprestimo() { return tipoEmprestimo; }
    public void setTipoEmprestimo(String tipo) { this.tipoEmprestimo = tipo; }
    public BigDecimal getValorContratado() { return valorContratado; }
    public void setValorContratado(BigDecimal v) { this.valorContratado = v; }
    public BigDecimal getValorSaldoDevedor() { return valorSaldoDevedor; }
    public void setValorSaldoDevedor(BigDecimal v) { this.valorSaldoDevedor = v; }
    public BigDecimal getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(BigDecimal v) { this.taxaJuros = v; }
    public Integer getPrazoMeses() { return prazoMeses; }
    public void setPrazoMeses(Integer v) { this.prazoMeses = v; }
    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer v) { this.parcelasPagas = v; }
    public Integer getParcelasRestantes() { return parcelasRestantes; }
    public void setParcelasRestantes(Integer v) { this.parcelasRestantes = v; }
    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal v) { this.valorParcela = v; }
    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate v) { this.dataContratacao = v; }
    public LocalDate getDataVencimentoPrimeiraParcela() { return dataVencimentoPrimeiraParcela; }
    public void setDataVencimentoPrimeiraParcela(LocalDate v) { this.dataVencimentoPrimeiraParcela = v; }
    public String getStatusEmprestimo() { return statusEmprestimo; }
    public void setStatusEmprestimo(String v) { this.statusEmprestimo = v; }
}
