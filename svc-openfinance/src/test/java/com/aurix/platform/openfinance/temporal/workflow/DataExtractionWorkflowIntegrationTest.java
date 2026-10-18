package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRecordRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.EmprestimoConsentido;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.EmprestimoConsentidoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova end-to-end (via TestWorkflowEnvironment, sem precisar de um Temporal
 * server real) que DataExtractionWorkflow, com as activities Spring reais
 * (não simuladas), extrai de múltiplos adaptadores (AccountSourceAdapter,
 * LoanSourceAdapter), canonicaliza/valida/registra linhagem via PipelineOrchestrator,
 * publica e materializa automaticamente em DataProductService.
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
    private EmprestimoConsentidoRepository emprestimoRepository;

    @Autowired
    private LineageRecordRepository lineageRepository;

    @Autowired
    private DataProductService dataProductService;

    @Autowired
    private DataProductRepository dataProductRepository;

    @Autowired
    private DataProductRecordRepository dataProductRecordRepository;

    private TestWorkflowEnvironment testEnv;

    @BeforeEach
    void setUp() {
        dataProductRecordRepository.deleteAll();
        dataProductRepository.deleteAll();
        emprestimoRepository.deleteAll();
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

    @Test
    void deveExecutarFluxoMultiDominioComMaterializacaoEmDataProductsELinhagemCompleta() {
        String consentId = "consent-multi-domain-e2e";

        Consentimento consentimento = new Consentimento();
        consentimento.setConsentId(consentId);
        consentimento.setClientId("client-multi-e2e");
        consentimento.setUserId(42L);
        consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimento.setPermissions("accounts,loans");
        consentimento.setDataCriacao(LocalDateTime.now());
        consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
        consentimentoRepository.save(consentimento);

        ContaConsentida conta = new ContaConsentida();
        conta.setConsentId(consentId);
        conta.setAccountId("acc-multi-1");
        conta.setInstitutionCode("AURIX");
        conta.setMoeda("BRL");
        conta.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
        conta.setStatusConta("ACTIVE");
        conta.setDataAtualizacao(LocalDateTime.now());
        contaRepository.save(conta);

        EmprestimoConsentido emprestimo = new EmprestimoConsentido();
        emprestimo.setConsentId(consentId);
        emprestimo.setEmprestimoId("loan-multi-1");
        emprestimo.setClienteId("12345678901");
        emprestimo.setTipoEmprestimo("PESSOAL");
        emprestimo.setValorContratado(new BigDecimal("25000.00"));
        emprestimo.setValorSaldoDevedor(new BigDecimal("20000.00"));
        emprestimo.setTaxaJuros(new BigDecimal("1.99"));
        emprestimo.setPrazoMeses(36);
        emprestimo.setParcelasPagas(6);
        emprestimo.setParcelasRestantes(30);
        emprestimo.setValorParcela(new BigDecimal("950.00"));
        emprestimo.setDataContratacao(LocalDate.of(2026, 1, 15));
        emprestimo.setDataVencimentoPrimeiraParcela(LocalDate.of(2026, 2, 15));
        emprestimo.setStatusEmprestimo("ATIVO");
        emprestimo.setDataAtualizacao(LocalDateTime.now());
        emprestimoRepository.save(emprestimo);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-multi-" + UUID.randomUUID(), "subject-multi", consentId, 1,
                "accounts,loans", "[\"accounts:READ\",\"loans:READ\"]", "[\"contas\",\"emprestimos\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-multi", "sig-multi");
        authorizedContextRepository.save(context);

        DataProduct prodContas = dataProductService.create(new DataProductRequest(
                "Contas Open Finance", "Produto de contas", "contas",
                "CONTA", "json", "{}", "/api/v1/data-products/contas"));
        DataProduct prodEmprestimos = dataProductService.create(new DataProductRequest(
                "Empréstimos Open Finance", "Produto de empréstimos", "emprestimos",
                "CREDITO", "json", "{}", "/api/v1/data-products/emprestimos"));

        ExecutionPlanRequest.SerializableNode nodeContas = new ExecutionPlanRequest.SerializableNode();
        nodeContas.setNodeId("node-contas");
        nodeContas.setCapability("extract-accounts");
        nodeContas.setResource("contas");
        nodeContas.setDependencies(List.of());
        nodeContas.setIdempotencyKey(UUID.randomUUID().toString());
        nodeContas.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodeEmprestimos = new ExecutionPlanRequest.SerializableNode();
        nodeEmprestimos.setNodeId("node-emprestimos");
        nodeEmprestimos.setCapability("extract-loans");
        nodeEmprestimos.setResource("emprestimos");
        nodeEmprestimos.setDependencies(List.of());
        nodeEmprestimos.setIdempotencyKey(UUID.randomUUID().toString());
        nodeEmprestimos.setTimeoutSeconds(30);

        ExecutionPlanRequest plano = new ExecutionPlanRequest();
        plano.setPlanId("plan-multi-" + UUID.randomUUID());
        plano.setConsentId(consentId);
        plano.setConsentVersion(1);
        plano.setParticipantId("participant-multi");
        plano.setNodes(List.of(nodeContas, nodeEmprestimos));

        WorkflowClient client = testEnv.getWorkflowClient();
        DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(plano);

        assertTrue(result.isSuccess(), "Workflow multi-domínio deveria ter sucesso: " + result.getErrors());
        assertEquals(2, result.getNodesProcessed());
        assertEquals(0, result.getNodesFailed());

        List<LineageRecord> lineages = lineageRepository.findByConsentIdOrderByCreatedAtAsc(consentId);
        assertEquals(2, lineages.size(), "Deveria haver linhagem para contas e empréstimos");
        for (LineageRecord lineage : lineages) {
            assertNotNull(lineage.getPublicationId(),
                    "INV-003: publicationId deve ser preenchido na linhagem após publicação");
        }

        DataProductResponse servedContas = dataProductService.serve(prodContas.getProductId(), null);
        assertEquals(1, servedContas.getTotalCount());
        assertTrue(servedContas.getRecords().get(0).toString().contains("acc-multi-1"),
                "DataProduct de contas deveria servir o registro canônico com accountId=acc-multi-1");

        DataProductResponse servedEmprestimos = dataProductService.serve(prodEmprestimos.getProductId(), null);
        assertEquals(1, servedEmprestimos.getTotalCount());
        String loanRecordJson = servedEmprestimos.getRecords().get(0).toString();
        assertTrue(loanRecordJson.contains("loan-multi-1"),
                "DataProduct de empréstimos deveria servir o registro canônico com loanId=loan-multi-1");
        assertTrue(loanRecordJson.contains("PESSOAL"),
                "DataProduct de empréstimos deveria conter tipo canônico PESSOAL");
    }
}
