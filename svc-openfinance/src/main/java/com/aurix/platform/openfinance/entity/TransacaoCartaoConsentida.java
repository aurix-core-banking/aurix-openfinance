package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacao_cartao_consentida")
public class TransacaoCartaoConsentida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String cartaoId;

    @Column(nullable = false, length = 64)
    private String transactionId;

    private BigDecimal valor;

    @Column(length = 3)
    private String moeda;

    @Column(length = 100)
    private String estabelecimento;

    @Column(length = 20)
    private String tipoTransacao;

    @Column(nullable = false)
    private LocalDateTime dataTransacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getCartaoId() { return cartaoId; }
    public void setCartaoId(String cartaoId) { this.cartaoId = cartaoId; }
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
    public void setDataTransacao(LocalDateTime dataTransacao) { this.dataTransacao = dataTransacao; }
}
