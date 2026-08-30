package com.aurix.platform.openfinance.temporal.dispatcher;

import com.aurix.platform.openfinance.temporal.workflow.DataExtractionWorkflow;
import com.aurix.platform.openfinance.temporal.workflow.dto.ExecutionPlanRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;

/**
 * Ponte Planner → Temporal (flows.yaml, fluxo {@code consent-to-execution}).
 *
 * <p>Fecha o gap em que o plano de extração (DAG) era criado e persistido, mas
 * nenhuma ação iniciava o {@link DataExtractionWorkflow} em produção — o workflow
 * só rodava em teste. Este dispatcher recebe um {@link ExecutionPlanRequest} já
 * construído pelo Planner e o dispara na task queue {@code openfinance-extraction}.
 *
 * <p>INV04: este dispatcher NÃO toma decisão de autorização — apenas entrega o
 * plano já autorizado ao motor de orquestração. Degrada graciosamente (log/warning)
 * quando o Temporal está indisponível, mantendo o fluxo utilizável em dev/local.
 *
 * <p>Orientado pelo teste arquitetural {@code testNoCircularDependencies}: o
 * dispatcher reside na slice {@code temporal} e NÃO referencia entidades da slice
 * {@code planner} (a conversão DAG → request é responsabilidade do Planner).
 */
@Component
public class WorkflowExecutionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionDispatcher.class);
    private static final String TASK_QUEUE = "openfinance-extraction";

    private final String temporalAddress;
    private final String namespace;
    private final boolean enabled;

    private WorkflowServiceStubs serviceStubs;
    private WorkflowClient workflowClient;

    public WorkflowExecutionDispatcher(
            @Value("${temporal.connection.target:localhost:7233}") String temporalAddress,
            @Value("${temporal.connection.namespace:aurix}") String namespace,
            @Value("${aurix.openfinance.workflow.dispatch-enabled:true}") boolean enabled) {
        this.temporalAddress = temporalAddress;
        this.namespace = namespace;
        this.enabled = enabled;
        connect();
    }

    private void connect() {
        if (!enabled) {
            return;
        }
        try {
            WorkflowServiceStubsOptions stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(temporalAddress)
                    .build();
            this.serviceStubs = WorkflowServiceStubs.newServiceStubs(stubsOptions);
            this.workflowClient = WorkflowClient.newInstance(serviceStubs,
                    WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
            log.info("WorkflowExecutionDispatcher conectado ao Temporal: {}", temporalAddress);
        } catch (Exception e) {
            log.warn("Falha ao conectar WorkflowExecutionDispatcher ao Temporal ({}): {}",
                    temporalAddress, e.getMessage());
        }
    }

    /**
     * Dispara o workflow de extração no Temporal para a requisição já construída.
     * Retorna o workflowId Temporal, ou null se o disparo não for possível.
     */
    public String despachar(ExecutionPlanRequest request) {
        if (!enabled || workflowClient == null) {
            log.warn("Dispatcher indisponível (enabled={}, client={}) — plano {} não disparado no Temporal",
                    enabled, workflowClient == null ? "null" : "ok", request.getPlanId());
            return null;
        }

        try {
            String workflowId = "of-extraction-" + request.getConsentId() + "-" + request.getPlanId();

            DataExtractionWorkflow workflow = workflowClient.newWorkflowStub(
                    DataExtractionWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                            .build());

            WorkflowClient.start(workflow::execute, request);

            log.info("Workflow de extração disparado: workflowId={}, planId={}, consentId={}",
                    workflowId, request.getPlanId(), request.getConsentId());
            return workflowId;
        } catch (Exception e) {
            log.warn("Falha ao disparar workflow Temporal para plano {}: {}", request.getPlanId(), e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (serviceStubs != null) {
            serviceStubs.shutdown();
        }
    }
}
