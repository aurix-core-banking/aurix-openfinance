package com.aurix.platform.openfinance.temporal.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Status atual da extração, consultável via query do Temporal.
 */
public class ExtractionStatus implements Serializable {

    @JsonProperty("plano_id")
    private String planId;

    @JsonProperty("em_execucao")
    private boolean running;

    @JsonProperty("cancelado")
    private boolean cancelled;

    @JsonProperty("motivo_cancelamento")
    private String cancelReason;

    @JsonProperty("node_corrente")
    private String currentNode;

    @JsonProperty("progresso_percentual")
    private double progressPercentage;

    public ExtractionStatus() {
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
