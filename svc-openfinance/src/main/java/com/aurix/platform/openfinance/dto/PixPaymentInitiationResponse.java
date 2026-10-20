package com.aurix.platform.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta oficial para Iniciação de Pagamento Pix (Fase 3 Open Finance Brasil).
 */
public class PixPaymentInitiationResponse {

    private String paymentId;
    private String consentId;
    private String endToEndId;
    private String status; // PDNG (Pendente), ACSC (Liquidado), RJCT (Rejeitado), CANC (Cancelado)
    private BigDecimal valor;
    private String moeda;
    private String chavePix;
    private String descricao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataLiquidacao;

    public PixPaymentInitiationResponse() {}

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataLiquidacao() { return dataLiquidacao; }
    public void setDataLiquidacao(LocalDateTime dataLiquidacao) { this.dataLiquidacao = dataLiquidacao; }
}
