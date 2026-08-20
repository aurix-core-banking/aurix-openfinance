package com.aurix.platform.openfinance.temporal.worker;

import com.aurix.platform.openfinance.temporal.activity.ExtractDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.PublishDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.TransformDataActivityImpl;
import com.aurix.platform.openfinance.temporal.workflow.DataExtractionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Worker Temporal para Open Finance.
 * Registra o {@link DataExtractionWorkflowImpl} e as activities de extração/transformação/publicação
 * na task queue documentada em architecture/infrastructure.yaml (openfinance-extraction).
 *
 * <p>Conecta ao Temporal server via wiring manual (mesmo padrão usado em
 * aurix-backend/svc-customer/.../OnboardingTemporalWorker) em vez de depender de autoconfiguração do
 * temporal-spring-boot-starter, para poder degradar graciosamente (log de warning, app continua no ar)
 * quando o Temporal não está acessível em dev/local.
 *
 * <p>TODO (fora do escopo dev/local atual): implementar e registrar {@code ConsentMonitoringWorkflow}
 * numa task queue própria quando esse workflow existir — hoje não há implementação real dele.
 */
@Component
public class OpenFinanceWorker {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceWorker.class);
    private static final String TASK_QUEUE = "openfinance-extraction";

    @Value("${temporal.connection.target:localhost:7233}")
    private String temporalAddress;

    @Value("${temporal.connection.namespace:aurix}")
    private String namespace;

    private final ExtractDataActivityImpl extractDataActivity;
    private final TransformDataActivityImpl transformDataActivity;
    private final PublishDataActivityImpl publishDataActivity;

    private WorkerFactory workerFactory;

    public OpenFinanceWorker(ExtractDataActivityImpl extractDataActivity,
                              TransformDataActivityImpl transformDataActivity,
                              PublishDataActivityImpl publishDataActivity) {
        this.extractDataActivity = extractDataActivity;
        this.transformDataActivity = transformDataActivity;
        this.publishDataActivity = publishDataActivity;
    }

    @PostConstruct
    public void iniciar() {
        try {
            WorkflowServiceStubsOptions stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(temporalAddress)
                    .build();
            WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newServiceStubs(stubsOptions);

            WorkflowClient workflowClient = WorkflowClient.newInstance(serviceStubs,
                    WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

            workerFactory = WorkerFactory.newInstance(workflowClient);

            Worker extractionWorker = workerFactory.newWorker(TASK_QUEUE);
            extractionWorker.registerWorkflowImplementationTypes(DataExtractionWorkflowImpl.class);
            extractionWorker.registerActivitiesImplementations(
                    extractDataActivity,
                    transformDataActivity,
                    publishDataActivity
            );

            workerFactory.start();
            log.info("Worker Temporal iniciado - task queue: {}", TASK_QUEUE);
        } catch (Exception e) {
            log.warn("Falha ao conectar com Temporal ({}): worker de extração não iniciado", e.getMessage());
        }
    }

    @PreDestroy
    public void parar() {
        if (workerFactory != null) {
            log.info("Parando worker Temporal");
            workerFactory.shutdown();
        }
    }
}
