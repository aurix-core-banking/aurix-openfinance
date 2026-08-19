package com.aurix.platform.openfinance.temporal.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Resultado da extração de dados retornada pelo workflow.
 */
public class ExtractionResult implements Serializable {

    @JsonProperty("plano_id")
    private String planId;

    @JsonProperty("sucesso")
    private boolean success;

    @JsonProperty("nodes_processados")
    private int nodesProcessed;

    @JsonProperty("nodes_falha")
    private int nodesFailed;

    @JsonProperty("resultados_por_node")
    private Map<String, NodeResult> nodeResults;

    @JsonProperty("erros")
    private List<String> errors;

    @JsonProperty("duracao_ms")
    private long durationMs;

    public ExtractionResult() {
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getNodesProcessed() {
        return nodesProcessed;
    }

    public void setNodesProcessed(int nodesProcessed) {
        this.nodesProcessed = nodesProcessed;
    }

    public int getNodesFailed() {
        return nodesFailed;
    }

    public void setNodesFailed(int nodesFailed) {
        this.nodesFailed = nodesFailed;
    }

    public Map<String, NodeResult> getNodeResults() {
        return nodeResults;
    }

    public void setNodeResults(Map<String, NodeResult> nodeResults) {
        this.nodeResults = nodeResults;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Resultado individual de um node do DAG.
     */
    public static class NodeResult implements Serializable {

        @JsonProperty("node_id")
        private String nodeId;

        @JsonProperty("sucesso")
        private boolean success;

        @JsonProperty("registros_extraidos")
        private int recordsExtracted;

        @JsonProperty("registros_transformados")
        private int recordsTransformed;

        @JsonProperty("registros_publicados")
        private int recordsPublished;

        @JsonProperty("duracao_ms")
        private long durationMs;

        @JsonProperty("erro")
        private String error;

        public NodeResult() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRecordsExtracted() {
            return recordsExtracted;
        }

        public void setRecordsExtracted(int recordsExtracted) {
            this.recordsExtracted = recordsExtracted;
        }

        public int getRecordsTransformed() {
            return recordsTransformed;
        }

        public void setRecordsTransformed(int recordsTransformed) {
            this.recordsTransformed = recordsTransformed;
        }

        public int getRecordsPublished() {
            return recordsPublished;
        }

        public void setRecordsPublished(int recordsPublished) {
            this.recordsPublished = recordsPublished;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
