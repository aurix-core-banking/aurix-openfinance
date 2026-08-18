package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pix_consentido")
public class PixConsentido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String pixId;

    @Column(nullable = false, length = 64)
    private String clienteId;

    @Column(length = 30)
    private String tipoPix;

    @Column(length = 100)
    private String chavedePix;

    @Column(length = 30)
    private String tipoChave;

    private BigDecimal valor;

    @Column(length = 3)
    private String moeda;

    @Column(length = 200)
    private String descricao;

    @Column(length = 50)
    private String statusPix;

    @Column(nullable = false)
    private LocalDateTime dataPix;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getPixId() { return pixId; }
    public void setPixId(String pixId) { this.pixId = pixId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getTipoPix() { return tipoPix; }
    public void setTipoPix(String tipoPix) { this.tipoPix = tipoPix; }
    public String getChavedePix() { return chavedePix; }
    public void setChavedePix(String chavedePix) { this.chavedePix = chavedePix; }
    public String getTipoChave() { return tipoChave; }
    public void setTipoChave(String tipoChave) { this.tipoChave = tipoChave; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatusPix() { return statusPix; }
    public void setStatusPix(String statusPix) { this.statusPix = statusPix; }
    public LocalDateTime getDataPix() { return dataPix; }
    public void setDataPix(LocalDateTime dataPix) { this.dataPix = dataPix; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
