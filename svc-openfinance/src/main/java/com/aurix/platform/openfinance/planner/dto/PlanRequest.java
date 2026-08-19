package com.aurix.platform.openfinance.planner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Requisição para criação de plano de execução.
 */
public class PlanRequest {

    @NotBlank(message = "consentId é obrigatório")
    @JsonProperty("consentimento_id")
    private String consentId;

    @NotNull(message = "consentVersion é obrigatório")
    @JsonProperty("versao_consentimento")
    private Integer consentVersion;

    @NotBlank(message = "participanteId é obrigatório")
    @JsonProperty("participante_id")
    private String participantId;

    @JsonProperty("recursos_solicitados")
    private List<String> requestedResources;

    @JsonProperty("configuracoes")
    private Map<String, Object> configuration;

    @JsonProperty("valido_ate")
    private String validUntil;

    public PlanRequest() {
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public Integer getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(Integer consentVersion) {
        this.consentVersion = consentVersion;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public List<String> getRequestedResources() {
        return requestedResources;
    }

    public void setRequestedResources(List<String> requestedResources) {
        this.requestedResources = requestedResources;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
}
