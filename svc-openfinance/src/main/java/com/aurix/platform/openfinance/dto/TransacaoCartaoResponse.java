package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransacaoCartaoResponse {
    private String transactionId;
    private BigDecimal valor;
    private String moeda;
    private String estabelecimento;
    private String tipoTransacao;
    private LocalDateTime dataTransacao;
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }
    public String getTipoTransacao() { return tipoTransacao; }
    public void setTipoTransacao(String tipoTransacao) { this.tipoTransacao = tipoTransacao; }
    public LocalDateTime getDataTransacao() { return dataTransacao; }
    public void setDataTransacao(LocalDateTime data) { this.dataTransacao = data; }
}
