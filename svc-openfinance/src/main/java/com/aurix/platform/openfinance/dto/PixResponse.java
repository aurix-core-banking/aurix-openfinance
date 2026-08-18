package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PixResponse {
    private String pixId;
    private String tipoPix;
    private String chavedePix;
    private String tipoChave;
    private BigDecimal valor;
    private String moeda;
    private String descricao;
    private String statusPix;
    private LocalDateTime dataPix;
    public String getPixId() { return pixId; }
    public void setPixId(String pixId) { this.pixId = pixId; }
    public String getTipoPix() { return tipoPix; }
    public void setTipoPix(String v) { this.tipoPix = v; }
    public String getChavedePix() { return chavedePix; }
    public void setChavedePix(String v) { this.chavedePix = v; }
    public String getTipoChave() { return tipoChave; }
    public void setTipoChave(String v) { this.tipoChave = v; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal v) { this.valor = v; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String v) { this.moeda = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public String getStatusPix() { return statusPix; }
    public void setStatusPix(String v) { this.statusPix = v; }
    public LocalDateTime getDataPix() { return dataPix; }
    public void setDataPix(LocalDateTime v) { this.dataPix = v; }
}
