package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.temporal.activity.ExtractDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.PublishDataActivityImpl;
import com.aurix.platform.openfinance.temporal.activity.TransformDataActivityImpl;
import com.aurix.platform.openfinance.temporal.workflow.dto.ExecutionPlanRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova end-to-end (via TestWorkflowEnvironment, sem precisar de um Temporal
 * server real) que DataExtractionWorkflow, com as activities Spring reais
 * (não simuladas), extrai do AccountSourceAdapter, canonicaliza/registra
 * linhagem via PipelineOrchestrator e conclui com sucesso.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataExtractionWorkflowIntegrationTest {

    private static final String TASK_QUEUE = "openfinance-extraction-test";

    @Autowired
    private ExtractDataActivityImpl extractDataActivity;

    @Autowired
    private TransformDataActivityImpl transformDataActivity;

    @Autowired
    private PublishDataActivityImpl publishDataActivity;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @Autowired
    private AuthorizedContextRepository authorizedContextRepository;

    @Autowired
    private ContaConsentidaRepository contaRepository;

    @Autowired
    private LineageRecordRepository lineageRepository;

    private TestWorkflowEnvironment testEnv;

    @BeforeEach
    void setUp() {
        contaRepository.deleteAll();
        authorizedContextRepository.deleteAll();
        consentimentoRepository.deleteAll();

        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(DataExtractionWorkflowImpl.class);
        worker.registerActivitiesImplementations(extractDataActivity, transformDataActivity, publishDataActivity);
        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    @Test
    void deveExecutarWorkflowCompletoComExtractorsECanonicalizacaoReais() {
        String consentId = "consent-workflow-e2e";

        Consentimento consentimento = new Consentimento();
        consentimento.setConsentId(consentId);
        consentimento.setClientId("client-e2e");
        consentimento.setUserId(1L);
        consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimento.setPermissions("accounts");
        consentimento.setDataCriacao(LocalDateTime.now());
        consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
        consentimentoRepository.save(consentimento);

        ContaConsentida conta = new ContaConsentida();
        conta.setConsentId(consentId);
        conta.setAccountId("acc-e2e-1");
        conta.setInstitutionCode("AURIX");
        conta.setMoeda("BRL");
        conta.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
        conta.setStatusConta("ACTIVE");
        conta.setDataAtualizacao(LocalDateTime.now());
        contaRepository.save(conta);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-e2e-" + UUID.randomUUID(), "subject-e2e", consentId, 1,
                "accounts", "[\"accounts:READ\"]", "[\"contas\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-e2e", "sig-e2e");
        authorizedContextRepository.save(context);

        ExecutionPlanRequest.SerializableNode node = new ExecutionPlanRequest.SerializableNode();
        node.setNodeId("node-contas");
        node.setCapability("extract-accounts");
        node.setResource("contas");
        node.setDependencies(List.of());
        node.setIdempotencyKey(UUID.randomUUID().toString());
        node.setTimeoutSeconds(30);

        ExecutionPlanRequest plano = new ExecutionPlanRequest();
        plano.setPlanId("plan-e2e-" + UUID.randomUUID());
        plano.setConsentId(consentId);
        plano.setConsentVersion(1);
        plano.setParticipantId("participant-e2e");
        plano.setNodes(List.of(node));

        WorkflowClient client = testEnv.getWorkflowClient();
        DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(plano);

        assertTrue(result.isSuccess(), "workflow deveria ter sucesso: " + result.getErrors());
        assertEquals(1, result.getNodesProcessed());
        assertEquals(0, result.getNodesFailed());

        ExtractionResult.NodeResult nodeResult = result.getNodeResults().get("node-contas");
        assertTrue(nodeResult.isSuccess());
        assertEquals(1, nodeResult.getRecordsExtracted());

        // A linhagem gerada pelo PipelineOrchestrator dentro do TransformDataActivity
        // precisa estar persistida de verdade, associada ao consentId real.
        assertTrue(lineageRepository.findByConsentIdOrderByCreatedAtAsc(consentId).size() >= 1);
    }
}
