package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.service.AuthorizedContextService;
import com.aurix.platform.openfinance.event.OpenFinanceEventPublisher;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.repository.ReconciliationRecordRepository;
import com.aurix.platform.openfinance.reconciliation.service.ReconciliationService;
import com.aurix.platform.openfinance.temporal.activity.*;
import com.aurix.platform.openfinance.temporal.workflow.dto.ExecutionPlanRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários e de integração Temporal para Issue #7:
 * - Propagação de falha de atividade no DataExtractionWorkflowImpl (não publicar se extração/transformação falhar)
 * - ConsentMonitoringWorkflow + ValidateConsentActivityImpl
 * - ReconciliationWorkflow + ReconcileActivityImpl + ReconciliationRepairExecutor
 */
class TemporalWorkflowsAndFailureTest {

    private static final String TASK_QUEUE = "openfinance-issue7-test";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(
                DataExtractionWorkflowImpl.class,
                ConsentMonitoringWorkflowImpl.class,
                ReconciliationWorkflowImpl.class);
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    @Test
    @DisplayName("DataExtractionWorkflow deve interromper node e marcar falha quando TransformDataActivity retorna success=false")
    void deveInterromperNodeQuandoTransformacaoFalhaSemPublicar() {
        AtomicBoolean publishCalled = new AtomicBoolean(false);

        ExtractDataActivity extractStub = request -> {
            ExtractDataActivity.ExtractResult r = new ExtractDataActivity.ExtractResult();
            r.setNodeId(request.getNodeId());
            r.setRecordCount(1);
            r.setRawData("{\"accountId\":\"acc-1\"}");
            r.setSuccess(true);
            return r;
        };

        TransformDataActivity transformStub = request -> {
            TransformDataActivity.TransformResult r = new TransformDataActivity.TransformResult();
            r.setNodeId(request.getNodeId());
            r.setSuccess(false);
            r.setErrorMessage("Etapa SCHEMA_VALIDATION: Campo obrigatório ausente");
            return r;
        };

        PublishDataActivity publishStub = request -> {
            publishCalled.set(true);
            PublishDataActivity.PublishResult r = new PublishDataActivity.PublishResult();
            r.setNodeId(request.getNodeId());
            r.setSuccess(true);
            return r;
        };

        worker.registerActivitiesImplementations(extractStub, transformStub, publishStub);
        testEnv.start();

        ExecutionPlanRequest.SerializableNode nodeContas = new ExecutionPlanRequest.SerializableNode();
        nodeContas.setNodeId("node-contas");
        nodeContas.setResource("contas");
        nodeContas.setDependencies(List.of());

        ExecutionPlanRequest.SerializableNode nodeTransacoes = new ExecutionPlanRequest.SerializableNode();
        nodeTransacoes.setNodeId("node-transacoes");
        nodeTransacoes.setResource("transacoes");
        nodeTransacoes.setDependencies(List.of("node-contas"));

        ExecutionPlanRequest planRequest = new ExecutionPlanRequest();
        planRequest.setPlanId("plan-fail-1");
        planRequest.setConsentId("consent-fail-1");
        planRequest.setNodes(List.of(nodeContas, nodeTransacoes));

        DataExtractionWorkflow workflow = client.newWorkflowStub(
                DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(planRequest);

        assertFalse(result.isSuccess(), "Workflow deve falhar quando um node falha na transformação");
        assertFalse(publishCalled.get(), "PublishDataActivity NÃO deve ser chamada quando a transformação falha (INV-003)");
        assertEquals(0, result.getNodesProcessed());
        assertEquals(2, result.getNodesFailed(), "O node que falhou e seu dependente devem constar como falhados");
    }

    @Test
    @DisplayName("ConsentMonitoringWorkflow deve validar contexto autorizado ativo via ValidateConsentActivityImpl")
    void deveMonitorarConsentimentoComContextoAtivo() {
        AuthorizedContextService contextService = mock(AuthorizedContextService.class);
        AuthorizedContext ctx = new AuthorizedContext(
                "ctx-1", "user-1", "consent-mon-1", 2, "compartilhamento",
                "ACCOUNTS_READ", "{\"resources\":[\"contas\"]}",
                LocalDateTime.now().plusHours(1), "PS256", "thumb", "sig");
        when(contextService.listActiveByConsentId("consent-mon-1")).thenReturn(List.of(ctx));

        worker.registerActivitiesImplementations(new ValidateConsentActivityImpl(contextService));
        testEnv.start();

        ConsentMonitoringWorkflow workflow = client.newWorkflowStub(
                ConsentMonitoringWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ConsentMonitoringWorkflow.MonitoringResult result = workflow.monitorar(
                new ConsentMonitoringWorkflow.MonitoringInput("consent-mon-1", "plan-mon-1", 2));

        assertTrue(result.isConsentValid());
        assertFalse(result.isRevoked());
    }

    @Test
    @DisplayName("ReconciliationWorkflow deve detectar divergência e acionar reparo automático real")
    void deveExecutarReconciliationWorkflowERepararDivergencia() {
        ReconciliationRecordRepository repo = mock(ReconciliationRecordRepository.class);
        OpenFinanceEventPublisher eventPublisher = mock(OpenFinanceEventPublisher.class);
        List<ReconciliationRecord> store = new ArrayList<>();

        when(repo.findByPlanId("plan-rec-1")).thenAnswer(inv -> new ArrayList<>(store));
        when(repo.save(any(ReconciliationRecord.class))).thenAnswer(inv -> {
            ReconciliationRecord r = inv.getArgument(0);
            store.removeIf(existing -> existing.getReconciliationId().equals(r.getReconciliationId()));
            store.add(r);
            return r;
        });
        when(repo.findByReconciliationId(any())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return store.stream().filter(r -> r.getReconciliationId().equals(id)).findFirst();
        });

        AtomicInteger repairExecutorCalls = new AtomicInteger(0);
        ReconciliationService reconciliationService = new ReconciliationService(
                repo, eventPublisher, (planId, nodeId, faltantes) -> {
                    repairExecutorCalls.incrementAndGet();
                    return faltantes;
                });

        worker.registerActivitiesImplementations(new ReconcileActivityImpl(reconciliationService));
        testEnv.start();

        ReconciliationWorkflow workflow = client.newWorkflowStub(
                ReconciliationWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ReconciliationWorkflow.ReconciliationWorkflowInput input =
                new ReconciliationWorkflow.ReconciliationWorkflowInput();
        input.setPlanId("plan-rec-1");
        input.setExtractedCounts(Map.of("node-contas", 3));
        input.setPublishedCounts(Map.of("node-contas", 1));
        input.setAutoRepair(true);

        ReconciliationWorkflow.ReconciliationWorkflowResult result = workflow.reconciliar(input);

        assertTrue(result.isHasDivergences());
        assertTrue(result.isRepaired());
        assertEquals(2, result.getTotalRecordsRepaired());
        assertEquals(1, repairExecutorCalls.get(), "Deve acionar o ReconciliationRepairExecutor durante o reparo");
    }
}
