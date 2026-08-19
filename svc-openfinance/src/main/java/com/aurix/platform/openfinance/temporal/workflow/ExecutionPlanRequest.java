package com.aurix.platform.openfinance.temporal.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Requisição de execução serializável para o workflow Temporal.
 */
public class ExecutionPlanRequest implements Serializable {

    @JsonProperty("plano_id")
    private String planId;

    @JsonProperty("consentimento_id")
    private String consentId;

    @JsonProperty("versao_consentimento")
    private int consentVersion;

    @JsonProperty("participante_id")
    private String participantId;

    @JsonProperty("dag_json")
    private String dagJson;

    @JsonProperty("nodes")
    private List<SerializableNode> nodes;

    public ExecutionPlanRequest() {
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

    public String getDagJson() {
        return dagJson;
    }

    public void setDagJson(String dagJson) {
        this.dagJson = dagJson;
    }

    public List<SerializableNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<SerializableNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Node serializável para comunicação Temporal.
     */
    public static class SerializableNode implements Serializable {

        @JsonProperty("node_id")
        private String nodeId;

        @JsonProperty("capacidade")
        private String capability;

        @JsonProperty("recurso")
        private String resource;

        @JsonProperty("dependencias")
        private List<String> dependencies;

        @JsonProperty("chave_idempotencia")
        private String idempotencyKey;

        @JsonProperty("timeout_segundos")
        private int timeoutSeconds;

        @JsonProperty("max_tentativas")
        private int maxAttempts;

        public SerializableNode() {
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

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
