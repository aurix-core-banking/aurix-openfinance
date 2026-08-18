package com.aurix.platform.openfinance.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConsentimentoResponse {

    private String consentId;
    private String clientId;
    private String status;
    private List<String> permissions;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private int version;

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
