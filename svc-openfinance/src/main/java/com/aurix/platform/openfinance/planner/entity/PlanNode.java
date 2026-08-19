package com.aurix.platform.openfinance.planner.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Representa um node individual dentro do DAG de execução.
 * Cada node mapeia para uma capacidade de extração (ex: contas, transações, cartões).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanNode {

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("capacidade")
    private String capability;

    @JsonProperty("recurso")
    private String resource;

    @JsonProperty("dependencias")
    private List<String> dependencies;

    @JsonProperty("autorizacao")
    private AuthorizationInfo authorization;

    @JsonProperty("politica_tentativa")
    private RetryPolicy retryPolicy;

    @JsonProperty("timeout_segundos")
    private int timeoutSeconds;

    @JsonProperty("limite_taxa")
    private RateLimitInfo rateLimit;

    @JsonProperty("chave_idempotencia")
    private String idempotencyKey;

    @JsonProperty("versao_schema")
    private String schemaVersion;

    public PlanNode() {
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

    public AuthorizationInfo getAuthorization() {
        return authorization;
    }

    public void setAuthorization(AuthorizationInfo authorization) {
        this.authorization = authorization;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public RateLimitInfo getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitInfo rateLimit) {
        this.rateLimit = rateLimit;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * Informação de autorização vinculada ao consentimento.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthorizationInfo {

        @JsonProperty("consentimento_id")
        private String consentId;

        @JsonProperty("escopo")
        private List<String> scopes;

        @JsonProperty("data_validade")
        private String validUntil;

        public AuthorizationInfo() {
        }

        public String getConsentId() {
            return consentId;
        }

        public void setConsentId(String consentId) {
            this.consentId = consentId;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public String getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(String validUntil) {
            this.validUntil = validUntil;
        }
    }

    /**
     * Política de retry configurada para o node.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RetryPolicy {

        @JsonProperty("max_tentativas")
        private int maxAttempts;

        @JsonProperty("intervalo_ms")
        private long intervalMs;

        @JsonProperty("backoff_exponencial")
        private boolean exponentialBackoff;

        public RetryPolicy() {
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public boolean isExponentialBackoff() {
            return exponentialBackoff;
        }

        public void setExponentialBackoff(boolean exponentialBackoff) {
            this.exponentialBackoff = exponentialBackoff;
        }
    }

    /**
     * Informação de limite de taxa (rate limiting).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RateLimitInfo {

        @JsonProperty("requisicoes_por_segundo")
        private int requestsPerSecond;

        @JsonProperty("requisicoes_por_minuto")
        private int requestsPerMinute;

        public RateLimitInfo() {
        }

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
