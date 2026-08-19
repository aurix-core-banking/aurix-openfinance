package com.aurix.platform.openfinance.temporal.worker;

import com.aurix.platform.openfinance.temporal.activity.ExtractDataActivity;
import com.aurix.platform.openfinance.temporal.activity.ExtractDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.PublishDataActivity;
import com.aurix.platform.openfinance.temporal.activity.PublishDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.TransformDataActivity;
import com.aurix.platform.openfinance.temporal.activity.TransformDataActivityImpl;
import com.aurix.platform.openfinance.temporal.workflow.DataExtractionWorkflow;
import com.aurix.platform.openfinance.temporal.workflow.DataExtractionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Worker Temporal para Open Finance.
 * Registra workflows e atividades:
 * - DataExtractionWorkflow
 * - ConsentMonitoringWorkflow
 * - Activities: ExtractData, TransformData, PublishData, ValidateConsent, Reconcile
 */
@Component
public class OpenFinanceWorker {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceWorker.class);
    private static final String TASK_QUEUE = "open-finance-extraction";
    private static final String CONSENT_TASK_QUEUE = "open-finance-consent-monitoring";

    private final WorkflowClient workflowClient;
    private final ExtractDataActivityImpl extractDataActivity;
    private final TransformDataActivityImpl transformDataActivity;
    private final PublishDataActivityImpl publishDataActivity;

    private WorkerFactory workerFactory;

    public OpenFinanceWorker(WorkflowClient workflowClient,
                             ExtractDataActivityImpl extractDataActivity,
                             TransformDataActivityImpl transformDataActivity,
                             PublishDataActivityImpl publishDataActivity) {
        this.workflowClient = workflowClient;
        this.extractDataActivity = extractDataActivity;
        this.transformDataActivity = transformDataActivity;
        this.publishDataActivity = publishDataActivity;
    }

    @PostConstruct
    public void iniciar() {
        log.info("Iniciando worker Temporal para Open Finance");

        workerFactory = WorkerFactory.newInstance(workflowClient);

        Worker extractionWorker = workerFactory.newWorker(TASK_QUEUE);
        extractionWorker.registerWorkflowImplementationTypes(DataExtractionWorkflowImpl.class);
        extractionWorker.registerActivitiesImplementations(
                extractDataActivity,
                transformDataActivity,
                publishDataActivity
        );

        Worker consentWorker = workerFactory.newWorker(CONSENT_TASK_QUEUE);
        consentWorker.registerWorkflowImplementationTypes(DataExtractionWorkflowImpl.class);

        workerFactory.start();
        log.info("Worker Temporal iniciado com sucesso - task queues: {}, {}", TASK_QUEUE, CONSENT_TASK_QUEUE);
    }

    @PreDestroy
    public void parar() {
        if (workerFactory != null) {
            log.info("Parando worker Temporal");
            workerFactory.shutdown();
        }
    }
}
