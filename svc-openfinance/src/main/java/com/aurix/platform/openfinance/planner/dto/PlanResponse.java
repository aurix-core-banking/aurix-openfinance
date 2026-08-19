package com.aurix.platform.openfinance.planner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta contendo o plano de execução criado.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanResponse {

    @JsonProperty("plano_id")
    private String planId;

    @JsonProperty("consentimento_id")
    private String consentId;

    @JsonProperty("versao_consentimento")
    private int consentVersion;

    @JsonProperty("participante_id")
    private String participantId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("nodes")
    private List<PlanNodeInfo> nodes;

    @JsonProperty("edges")
    private List<EdgeInfo> edges;

    @JsonProperty("data_criacao")
    private LocalDateTime createdAt;

    @JsonProperty("data_execucao")
    private LocalDateTime executedAt;

    @JsonProperty("data_conclusao")
    private LocalDateTime completedAt;

    public PlanResponse() {
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public int getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(int consentVersion) {
        this.consentVersion = consentVersion;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PlanNodeInfo> getNodes() {
        return nodes;
    }

    public void setNodes(List<PlanNodeInfo> nodes) {
        this.nodes = nodes;
    }

    public List<EdgeInfo> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeInfo> edges) {
        this.edges = edges;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlanNodeInfo {

        @JsonProperty("node_id")
        private String nodeId;

        @JsonProperty("capacidade")
        private String capability;

        @JsonProperty("recurso")
        private String resource;

        @JsonProperty("dependencias")
        private List<String> dependencies;

        @JsonProperty("timeout_segundos")
        private int timeoutSeconds;

        @JsonProperty("chave_idempotencia")
        private String idempotencyKey;

        public PlanNodeInfo() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getCapability() {
            return capability;
        }

        public void setCapability(String capability) {
            this.capability = capability;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeInfo {

        @JsonProperty("origem")
        private String source;

        @JsonProperty("destino")
        private String target;

        public EdgeInfo() {
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }
}
