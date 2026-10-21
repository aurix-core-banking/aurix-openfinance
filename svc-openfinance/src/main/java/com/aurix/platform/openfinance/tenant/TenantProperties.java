package com.aurix.platform.openfinance.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração do Tenant para operação White-Label e Multi-Tenant.
 */
@Component
@ConfigurationProperties(prefix = "aurix.openfinance.tenant")
public class TenantProperties {

    /**
     * Identificador único do tenant (ex: "banco-ouro", "aurix").
     */
    private String id = "aurix";

    /**
     * Razão Social da instituição financeira participante perante o BACEN.
     */
    private String name = "Aurix Banco Múltiplo S.A.";

    /**
     * CNPJ oficial da instituição.
     */
    private String cnpj = "00.000.000/0001-91";

    /**
     * Código ISPB / Participante no Diretório do Open Finance Brasil.
     */
    private String participantCode = "00000000";

    /**
     * Nome comercial da marca exibida aos clientes finais.
     */
    private String brandName = "Aurix Bank";

    /**
     * Cor primária hexadecimal da marca para customização visual do frontend.
     */
    private String primaryColor = "#0052FF";

    /**
     * URL do logotipo da instituição.
     */
    private String logoUrl = "https://assets.aurix.com.br/logo.svg";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getParticipantCode() { return participantCode; }
    public void setParticipantCode(String participantCode) { this.participantCode = participantCode; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
