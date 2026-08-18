package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cartao_consentido")
public class CartaoConsentido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String cartaoId;

    @Column(nullable = false, length = 64)
    private String clienteId;

    @Column(length = 20)
    private String bandeira;

    @Column(length = 4)
    private String finalNumero;

    @Column(length = 20)
    private String statusCartao;

    private BigDecimal limiteCredito;

    private BigDecimal limiteDisponivel;

    private BigDecimal valorFaturaAtual;

    private LocalDate dataVencimentoFatura;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getCartaoId() { return cartaoId; }
    public void setCartaoId(String cartaoId) { this.cartaoId = cartaoId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
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
    public void setDataVencimentoFatura(LocalDate dataVencimentoFatura) { this.dataVencimentoFatura = dataVencimentoFatura; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
