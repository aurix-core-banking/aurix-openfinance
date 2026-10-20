package com.aurix.platform.openfinance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload para solicitação de Iniciação de Pagamento Pix (Fase 3 Open Finance Brasil).
 */
public class PixPaymentInitiationRequest {

    @NotBlank(message = "O ID do consentimento é obrigatório (INV-001)")
    private String consentId;

    private String endToEndId;

    @NotNull(message = "O valor do pagamento é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor mínimo para iniciação Pix é R$ 0,01")
    private BigDecimal valor;

    private String moeda = "BRL";

    @NotBlank(message = "A chave Pix de destino é obrigatória")
    private String chavePix;

    private String tipoChave = "ALEATORIA";

    private String descricao;

    private String clienteId;

    public PixPaymentInitiationRequest() {}

    public PixPaymentInitiationRequest(String consentId, BigDecimal valor, String chavePix, String tipoChave, String descricao) {
        this.consentId = consentId;
        this.valor = valor;
        this.chavePix = chavePix;
        this.tipoChave = tipoChave;
        this.descricao = descricao;
    }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public String getTipoChave() { return tipoChave; }
    public void setTipoChave(String tipoChave) { this.tipoChave = tipoChave; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
}
