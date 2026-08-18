package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CartaoResponse {
    private String cartaoId;
    private String bandeira;
    private String finalNumero;
    private String statusCartao;
    private BigDecimal limiteCredito;
    private BigDecimal limiteDisponivel;
    private BigDecimal valorFaturaAtual;
    private LocalDate dataVencimentoFatura;
    public String getCartaoId() { return cartaoId; }
    public void setCartaoId(String cartaoId) { this.cartaoId = cartaoId; }
    public String getBandeira() { return bandeira; }
    public void setBandeira(String bandeira) { this.bandeira = bandeira; }
    public String getFinalNumero() { return finalNumero; }
    public void setFinalNumero(String finalNumero) { this.finalNumero = finalNumero; }
    public String getStatusCartao() { return statusCartao; }
    public void setStatusCartao(String statusCartao) { this.statusCartao = statusCartao; }
    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }
    public BigDecimal getLimiteDisponivel() { return limiteDisponivel; }
    public void setLimiteDisponivel(BigDecimal limiteDisponivel) { this.limiteDisponivel = limiteDisponivel; }
    public BigDecimal getValorFaturaAtual() { return valorFaturaAtual; }
    public void setValorFaturaAtual(BigDecimal valorFaturaAtual) { this.valorFaturaAtual = valorFaturaAtual; }
    public LocalDate getDataVencimentoFatura() { return dataVencimentoFatura; }
    public void setDataVencimentoFatura(LocalDate data) { this.dataVencimentoFatura = data; }
}
