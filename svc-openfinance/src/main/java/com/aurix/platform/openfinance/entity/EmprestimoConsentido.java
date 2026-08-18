package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emprestimo_consentido")
public class EmprestimoConsentido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String emprestimoId;

    @Column(nullable = false, length = 64)
    private String clienteId;

    @Column(length = 30)
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

    @Column(length = 20)
    private String statusEmprestimo;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getEmprestimoId() { return emprestimoId; }
    public void setEmprestimoId(String emprestimoId) { this.emprestimoId = emprestimoId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getTipoEmprestimo() { return tipoEmprestimo; }
    public void setTipoEmprestimo(String tipoEmprestimo) { this.tipoEmprestimo = tipoEmprestimo; }
    public BigDecimal getValorContratado() { return valorContratado; }
    public void setValorContratado(BigDecimal valorContratado) { this.valorContratado = valorContratado; }
    public BigDecimal getValorSaldoDevedor() { return valorSaldoDevedor; }
    public void setValorSaldoDevedor(BigDecimal valorSaldoDevedor) { this.valorSaldoDevedor = valorSaldoDevedor; }
    public BigDecimal getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(BigDecimal taxaJuros) { this.taxaJuros = taxaJuros; }
    public Integer getPrazoMeses() { return prazoMeses; }
    public void setPrazoMeses(Integer prazoMeses) { this.prazoMeses = prazoMeses; }
    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer parcelasPagas) { this.parcelasPagas = parcelasPagas; }
    public Integer getParcelasRestantes() { return parcelasRestantes; }
    public void setParcelasRestantes(Integer parcelasRestantes) { this.parcelasRestantes = parcelasRestantes; }
    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }
    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }
    public LocalDate getDataVencimentoPrimeiraParcela() { return dataVencimentoPrimeiraParcela; }
    public void setDataVencimentoPrimeiraParcela(LocalDate data) { this.dataVencimentoPrimeiraParcela = data; }
    public String getStatusEmprestimo() { return statusEmprestimo; }
    public void setStatusEmprestimo(String statusEmprestimo) { this.statusEmprestimo = statusEmprestimo; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
