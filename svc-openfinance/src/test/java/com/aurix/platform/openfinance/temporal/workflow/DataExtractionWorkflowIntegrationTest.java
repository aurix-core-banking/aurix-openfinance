package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRecordRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService;
import com.aurix.platform.openfinance.entity.CartaoConsentido;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.EmprestimoConsentido;
import com.aurix.platform.openfinance.entity.FaturaConsentida;
import com.aurix.platform.openfinance.entity.InvestimentoConsentido;
import com.aurix.platform.openfinance.entity.PessoaConsentida;
import com.aurix.platform.openfinance.entity.SeguroConsentido;
import com.aurix.platform.openfinance.entity.TransacaoCartaoConsentida;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.repository.LineageRecordRepository;
import com.aurix.platform.openfinance.repository.CartaoConsentidoRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.EmprestimoConsentidoRepository;
import com.aurix.platform.openfinance.repository.FaturaConsentidaRepository;
import com.aurix.platform.openfinance.repository.InvestimentoConsentidoRepository;
import com.aurix.platform.openfinance.repository.PessoaConsentidaRepository;
import com.aurix.platform.openfinance.repository.SeguroConsentidoRepository;
import com.aurix.platform.openfinance.repository.TransacaoCartaoConsentidaRepository;
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
 * LoanSourceAdapter, InvestmentSourceAdapter, CardSourceAdapter,
 * InsuranceSourceAdapter, CustomerSourceAdapter), canonicaliza/valida/registra
 * linhagem via PipelineOrchestrator, publica e materializa automaticamente
 * em DataProductService.
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
    private InvestimentoConsentidoRepository investimentoRepository;

    @Autowired
    private CartaoConsentidoRepository cartaoRepository;

    @Autowired
    private FaturaConsentidaRepository faturaRepository;

    @Autowired
    private TransacaoCartaoConsentidaRepository transacaoCartaoRepository;

    @Autowired
    private SeguroConsentidoRepository seguroRepository;

    @Autowired
    private PessoaConsentidaRepository pessoaRepository;

    @Autowired
    private LineageRecordRepository lineageRepository;

    @Autowired
    private DataProductService dataProductService;

    @Autowired
    private DataProductRepository dataProductRepository;

    @Autowired
    private DataProductRecordRepository dataProductRecordRepository;

    @Autowired
    private com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository transacaoContaRepository;

    @Autowired
    private com.aurix.platform.openfinance.distribution.subscription.service.SubscriptionService subscriptionService;

    @Autowired
    private com.aurix.platform.openfinance.distribution.subscription.repository.SubscriptionRepository subscriptionRepository;

    private TestWorkflowEnvironment testEnv;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        dataProductRecordRepository.deleteAll();
        dataProductRepository.deleteAll();
        transacaoCartaoRepository.deleteAll();
        transacaoContaRepository.deleteAll();
        faturaRepository.deleteAll();
        cartaoRepository.deleteAll();
        seguroRepository.deleteAll();
        pessoaRepository.deleteAll();
        investimentoRepository.deleteAll();
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
        consentimento.setPermissions("accounts,loans,investments");
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

        ContaConsentida conta2 = new ContaConsentida();
        conta2.setConsentId(consentId);
        conta2.setAccountId("acc-multi-2");
        conta2.setInstitutionCode("AURIX");
        conta2.setMoeda("BRL");
        conta2.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
        conta2.setStatusConta("ACTIVE");
        conta2.setDataAtualizacao(LocalDateTime.now());
        contaRepository.save(conta2);

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

        InvestimentoConsentido investimento = new InvestimentoConsentido();
        investimento.setConsentId(consentId);
        investimento.setInvestimentoId("inv-multi-1");
        investimento.setClienteId("12345678901");
        investimento.setTipoInvestimento("CDB");
        investimento.setProduto("CDB Aurix 120% CDI");
        investimento.setQuantidade(new BigDecimal("10.0000"));
        investimento.setValorUnitario(new BigDecimal("1050.0000"));
        investimento.setValorTotal(new BigDecimal("10500.00"));
        investimento.setStatusInvestimento("ATIVO");
        investimento.setDataAtualizacao(LocalDateTime.now());
        investimentoRepository.save(investimento);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-multi-" + UUID.randomUUID(), "subject-multi", consentId, 1,
                "accounts,loans,investments",
                "[\"accounts:READ\",\"loans:READ\",\"investments:READ\"]",
                "[\"contas\",\"emprestimos\",\"investimentos\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-multi", "sig-multi");
        authorizedContextRepository.save(context);

        DataProduct prodContas = dataProductService.create(new DataProductRequest(
                "Contas Open Finance", "Produto de contas", "contas",
                "CONTA", "json", "{}", "/api/v1/data-products/contas"));
        DataProduct prodEmprestimos = dataProductService.create(new DataProductRequest(
                "Empréstimos Open Finance", "Produto de empréstimos", "emprestimos",
                "CREDITO", "json", "{}", "/api/v1/data-products/emprestimos"));
        DataProduct prodInvestimentos = dataProductService.create(new DataProductRequest(
                "Investimentos Open Finance", "Produto de investimentos", "investimentos",
                "INVESTIMENTO", "json", "{}", "/api/v1/data-products/investimentos"));

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

        ExecutionPlanRequest.SerializableNode nodeInvestimentos = new ExecutionPlanRequest.SerializableNode();
        nodeInvestimentos.setNodeId("node-investimentos");
        nodeInvestimentos.setCapability("extract-investments");
        nodeInvestimentos.setResource("investimentos");
        nodeInvestimentos.setDependencies(List.of());
        nodeInvestimentos.setIdempotencyKey(UUID.randomUUID().toString());
        nodeInvestimentos.setTimeoutSeconds(30);

        ExecutionPlanRequest plano = new ExecutionPlanRequest();
        plano.setPlanId("plan-multi-" + UUID.randomUUID());
        plano.setConsentId(consentId);
        plano.setConsentVersion(1);
        plano.setParticipantId("participant-multi");
        plano.setNodes(List.of(nodeContas, nodeEmprestimos, nodeInvestimentos));

        WorkflowClient client = testEnv.getWorkflowClient();
        DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(plano);

        assertTrue(result.isSuccess(), "Workflow multi-domínio deveria ter sucesso: " + result.getErrors());
        assertEquals(3, result.getNodesProcessed());
        assertEquals(0, result.getNodesFailed());

        List<LineageRecord> lineages = lineageRepository.findByConsentIdOrderByCreatedAtAsc(consentId);
        assertEquals(4, lineages.size(),
                "Deveria haver 4 registros de linhagem (2 contas em lote + 1 empréstimo + 1 investimento)");
        for (LineageRecord lineage : lineages) {
            assertNotNull(lineage.getPublicationId(),
                    "INV-003: publicationId deve ser preenchido na linhagem após publicação");
        }

        DataProductResponse servedContas = dataProductService.serve(prodContas.getProductId(), null);
        assertEquals(2, servedContas.getTotalCount(),
                "Ambos os registros do lote de contas (acc-multi-1 e acc-multi-2) devem ser materializados");
        String allContasJson = servedContas.getRecords().toString();
        assertTrue(allContasJson.contains("acc-multi-1"),
                "DataProduct de contas deveria servir o registro canônico com accountId=acc-multi-1");
        assertTrue(allContasJson.contains("acc-multi-2"),
                "DataProduct de contas deveria servir o segundo registro do lote accountId=acc-multi-2");

        DataProductResponse servedEmprestimos = dataProductService.serve(prodEmprestimos.getProductId(), null);
        assertEquals(1, servedEmprestimos.getTotalCount());
        String loanRecordJson = servedEmprestimos.getRecords().get(0).toString();
        assertTrue(loanRecordJson.contains("loan-multi-1"),
                "DataProduct de empréstimos deveria servir o registro canônico com loanId=loan-multi-1");
        assertTrue(loanRecordJson.contains("PESSOAL"),
                "DataProduct de empréstimos deveria conter tipo canônico PESSOAL");

        DataProductResponse servedInvestimentos = dataProductService.serve(prodInvestimentos.getProductId(), null);
        assertEquals(1, servedInvestimentos.getTotalCount());
        String invRecordJson = servedInvestimentos.getRecords().get(0).toString();
        assertTrue(invRecordJson.contains("inv-multi-1"),
                "DataProduct de investimentos deveria servir o registro canônico com investmentId=inv-multi-1");
        assertTrue(invRecordJson.contains("CDB"),
                "DataProduct de investimentos deveria conter tipo canônico CDB");
    }

    @Test
    void deveExecutarFluxoDeFaturasTransacoesCartaoSegurosEPessoasPontaAPonta() {
        String consentId = "consent-domains-13-e2e";

        Consentimento consentimento = new Consentimento();
        consentimento.setConsentId(consentId);
        consentimento.setClientId("client-domains-13");
        consentimento.setUserId(99L);
        consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimento.setPermissions("cards,card_bills,card_transactions,insurance,customers");
        consentimento.setDataCriacao(LocalDateTime.now());
        consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
        consentimentoRepository.save(consentimento);

        CartaoConsentido cartao = new CartaoConsentido();
        cartao.setConsentId(consentId);
        cartao.setCartaoId("card-13-1");
        cartao.setClienteId("12345678901");
        cartao.setBandeira("VISA");
        cartao.setFinalNumero("4321");
        cartao.setStatusCartao("ATIVO");
        cartao.setLimiteCredito(new BigDecimal("15000.00"));
        cartao.setLimiteDisponivel(new BigDecimal("12000.00"));
        cartao.setValorFaturaAtual(new BigDecimal("1850.00"));
        cartao.setDataVencimentoFatura(LocalDate.of(2026, 11, 10));
        cartao.setDataAtualizacao(LocalDateTime.now());
        cartaoRepository.save(cartao);

        FaturaConsentida fatura = new FaturaConsentida();
        fatura.setConsentId(consentId);
        fatura.setCartaoId("card-13-1");
        fatura.setFaturaId("bill-13-1");
        fatura.setValorTotal(new BigDecimal("1850.00"));
        fatura.setValorMinimo(new BigDecimal("185.00"));
        fatura.setValorPago(BigDecimal.ZERO);
        fatura.setDataVencimento(LocalDate.of(2026, 11, 10));
        fatura.setDataAtualizacao(LocalDateTime.now());
        faturaRepository.save(fatura);

        TransacaoCartaoConsentida txCartao = new TransacaoCartaoConsentida();
        txCartao.setConsentId(consentId);
        txCartao.setCartaoId("card-13-1");
        txCartao.setTransactionId("txc-13-1");
        txCartao.setValor(new BigDecimal("249.90"));
        txCartao.setMoeda("BRL");
        txCartao.setEstabelecimento("Aurix Tech Store");
        txCartao.setTipoTransacao("COMPRA_A_VISTA");
        txCartao.setDataTransacao(LocalDateTime.of(2026, 10, 18, 15, 30));
        transacaoCartaoRepository.save(txCartao);

        SeguroConsentido seguro = new SeguroConsentido();
        seguro.setConsentId(consentId);
        seguro.setApoliceId("pol-13-1");
        seguro.setClienteId("12345678901");
        seguro.setTipoSeguro("VIDA");
        seguro.setNomeSeguradora("Aurix Seguradora S.A.");
        seguro.setPremioMensal(new BigDecimal("120.00"));
        seguro.setPremioTotal(new BigDecimal("1440.00"));
        seguro.setValorSegurado(new BigDecimal("500000.00"));
        seguro.setDataInicio(LocalDate.of(2026, 1, 1));
        seguro.setDataFim(LocalDate.of(2027, 1, 1));
        seguro.setStatusApolice("VIGENTE");
        seguro.setDataAtualizacao(LocalDateTime.now());
        seguroRepository.save(seguro);

        PessoaConsentida pessoa = new PessoaConsentida();
        pessoa.setConsentId(consentId);
        pessoa.setCustomerId("cust-13-1");
        pessoa.setTipoPessoa(PessoaConsentida.TipoPessoa.FISICA);
        pessoa.setCpfCnpj("12345678901");
        pessoa.setNomeCompleto("Ana Clara Aurix");
        pessoa.setEmail("ana.clara@aurix.com");
        pessoa.setTelefone("11988887777");
        pessoa.setDataAtualizacao(LocalDateTime.now());
        pessoaRepository.save(pessoa);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-13-" + UUID.randomUUID(), "subject-13", consentId, 1,
                "cards,card_bills,card_transactions,insurance,customers",
                "[\"cards:READ\",\"card_bills:READ\",\"card_transactions:READ\",\"insurance:READ\",\"customers:READ\"]",
                "[\"cartoes\",\"faturas\",\"transacoes_cartao\",\"seguros\",\"pessoas\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-13", "sig-13");
        authorizedContextRepository.save(context);

        DataProduct prodFaturas = dataProductService.create(new DataProductRequest(
                "Faturas Open Finance", "Produto de faturas", "faturas",
                "FATURA_CARTAO", "json", "{}", "/api/v1/data-products/faturas"));
        DataProduct prodTxCartao = dataProductService.create(new DataProductRequest(
                "Transações Cartão Open Finance", "Produto de transações de cartão", "transacoes_cartao",
                "TRANSACAO_CARTAO", "json", "{}", "/api/v1/data-products/transacoes-cartao"));
        DataProduct prodSeguros = dataProductService.create(new DataProductRequest(
                "Seguros Open Finance", "Produto de seguros", "seguros",
                "SEGURO", "json", "{}", "/api/v1/data-products/seguros"));
        DataProduct prodPessoas = dataProductService.create(new DataProductRequest(
                "Pessoas Open Finance", "Produto cadastral de clientes", "pessoas",
                "PESSOA", "json", "{}", "/api/v1/data-products/pessoas"));

        ExecutionPlanRequest.SerializableNode nodeFaturas = new ExecutionPlanRequest.SerializableNode();
        nodeFaturas.setNodeId("node-faturas");
        nodeFaturas.setCapability("extract-faturas");
        nodeFaturas.setResource("faturas");
        nodeFaturas.setDependencies(List.of());
        nodeFaturas.setIdempotencyKey(UUID.randomUUID().toString());
        nodeFaturas.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodeTxCartao = new ExecutionPlanRequest.SerializableNode();
        nodeTxCartao.setNodeId("node-transacoes_cartao");
        nodeTxCartao.setCapability("extract-transacoes_cartao");
        nodeTxCartao.setResource("transacoes_cartao");
        nodeTxCartao.setDependencies(List.of());
        nodeTxCartao.setIdempotencyKey(UUID.randomUUID().toString());
        nodeTxCartao.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodeSeguros = new ExecutionPlanRequest.SerializableNode();
        nodeSeguros.setNodeId("node-seguros");
        nodeSeguros.setCapability("extract-seguros");
        nodeSeguros.setResource("seguros");
        nodeSeguros.setDependencies(List.of());
        nodeSeguros.setIdempotencyKey(UUID.randomUUID().toString());
        nodeSeguros.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodePessoas = new ExecutionPlanRequest.SerializableNode();
        nodePessoas.setNodeId("node-pessoas");
        nodePessoas.setCapability("extract-pessoas");
        nodePessoas.setResource("pessoas");
        nodePessoas.setDependencies(List.of());
        nodePessoas.setIdempotencyKey(UUID.randomUUID().toString());
        nodePessoas.setTimeoutSeconds(30);

        ExecutionPlanRequest plano = new ExecutionPlanRequest();
        plano.setPlanId("plan-13-" + UUID.randomUUID());
        plano.setConsentId(consentId);
        plano.setConsentVersion(1);
        plano.setParticipantId("participant-13");
        plano.setNodes(List.of(nodeFaturas, nodeTxCartao, nodeSeguros, nodePessoas));

        WorkflowClient client = testEnv.getWorkflowClient();
        DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(plano);

        assertTrue(result.isSuccess(), "Workflow dos 4 novos domínios deveria ter sucesso: " + result.getErrors());
        assertEquals(4, result.getNodesProcessed());
        assertEquals(0, result.getNodesFailed());

        assertEquals(1, dataProductService.serve(prodFaturas.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodFaturas.getProductId(), null).getRecords().get(0).toString().contains("bill-13-1"));

        assertEquals(1, dataProductService.serve(prodTxCartao.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodTxCartao.getProductId(), null).getRecords().get(0).toString().contains("txc-13-1"));

        assertEquals(1, dataProductService.serve(prodSeguros.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodSeguros.getProductId(), null).getRecords().get(0).toString().contains("pol-13-1"));

        assertEquals(1, dataProductService.serve(prodPessoas.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodPessoas.getProductId(), null).getRecords().get(0).toString().contains("cust-13-1"));
    }

    @Test
    void deveEntregarWebhookHttpAssinadoAoFinalDoWorkflowTemporalDeExtracao() throws Exception {
        com.sun.net.httpserver.HttpServer webhookServer =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> bodyRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> sigRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> tsRef = new java.util.concurrent.atomic.AtomicReference<>();

        webhookServer.createContext("/webhook-e2e", exchange -> {
            bodyRef.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            sigRef.set(exchange.getRequestHeaders().getFirst("X-Webhook-Signature"));
            tsRef.set(exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            latch.countDown();
        });
        webhookServer.start();

        try {
            String consentId = "consent-webhook-temporal-e2e";
            Consentimento consentimento = new Consentimento();
            consentimento.setConsentId(consentId);
            consentimento.setClientId("client-wh-e2e");
            consentimento.setUserId(77L);
            consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
            consentimento.setPermissions("accounts");
            consentimento.setDataCriacao(LocalDateTime.now());
            consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
            consentimentoRepository.save(consentimento);

            ContaConsentida conta = new ContaConsentida();
            conta.setConsentId(consentId);
            conta.setAccountId("acc-wh-e2e-1");
            conta.setInstitutionCode("AURIX");
            conta.setMoeda("BRL");
            conta.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
            conta.setStatusConta("ACTIVE");
            conta.setDataAtualizacao(LocalDateTime.now());
            contaRepository.save(conta);

            AuthorizedContext context = new AuthorizedContext(
                    "ctx-wh-" + UUID.randomUUID(), "subject-wh", consentId, 1,
                    "accounts", "[\"accounts:READ\"]", "[\"contas\"]",
                    LocalDateTime.now().plusHours(1), "ES256", "thumb-wh", "sig-wh");
            authorizedContextRepository.save(context);

            DataProduct prodContas = dataProductService.create(new DataProductRequest(
                    "Contas Webhook E2E", "Produto de contas com webhook", "contas",
                    "CONTA", "json", "{}", "/api/v1/data-products/contas-wh"));

            String callbackUrl = "http://localhost:" + webhookServer.getAddress().getPort() + "/webhook-e2e";
            var subscription = subscriptionService.subscribe(
                    new com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest(
                            "participant-wh-e2e", prodContas.getProductId(), callbackUrl, "data.published.v1", 30));

            ExecutionPlanRequest.SerializableNode nodeContas = new ExecutionPlanRequest.SerializableNode();
            nodeContas.setNodeId("node-contas");
            nodeContas.setCapability("extract-accounts");
            nodeContas.setResource("contas");
            nodeContas.setDependencies(List.of());
            nodeContas.setIdempotencyKey(UUID.randomUUID().toString());
            nodeContas.setTimeoutSeconds(30);

            ExecutionPlanRequest plano = new ExecutionPlanRequest();
            plano.setPlanId("plan-wh-" + UUID.randomUUID());
            plano.setConsentId(consentId);
            plano.setConsentVersion(1);
            plano.setParticipantId("participant-wh-e2e");
            plano.setNodes(List.of(nodeContas));

            WorkflowClient client = testEnv.getWorkflowClient();
            DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

            ExtractionResult result = workflow.execute(plano);
            assertTrue(result.isSuccess());

            assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                    "O assinante deveria receber o POST HTTP do webhook ao final do workflow Temporal");
            assertNotNull(bodyRef.get());
            assertTrue(bodyRef.get().contains("acc-wh-e2e-1"));
            assertTrue(bodyRef.get().contains(subscription.getSubscriptionId()));
            assertNotNull(sigRef.get());
            assertNotNull(tsRef.get());
        } finally {
            webhookServer.stop(0);
        }
    }

    @Test
    void deveExecutarFluxoGranularDeTransacoesSaldosEParcelasEmprestimo() {
        String consentId = "consent-subresources-17-e2e";

        Consentimento consentimento = new Consentimento();
        consentimento.setConsentId(consentId);
        consentimento.setClientId("client-subresources-17");
        consentimento.setUserId(101L);
        consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimento.setPermissions("accounts,balances,transactions,loans,loan_installments");
        consentimento.setDataCriacao(LocalDateTime.now());
        consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
        consentimentoRepository.save(consentimento);

        ContaConsentida conta = new ContaConsentida();
        conta.setConsentId(consentId);
        conta.setAccountId("acc-sub-1");
        conta.setInstitutionCode("AURIX");
        conta.setMoeda("BRL");
        conta.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
        conta.setStatusConta("ACTIVE");
        conta.setSaldoDisponivel(new BigDecimal("4200.75"));
        conta.setSaldoAtual(new BigDecimal("4200.75"));
        conta.setDataAtualizacao(LocalDateTime.now());
        contaRepository.save(conta);

        com.aurix.platform.openfinance.entity.TransacaoConsentida tx =
                new com.aurix.platform.openfinance.entity.TransacaoConsentida();
        tx.setConsentId(consentId);
        tx.setAccountId("acc-sub-1");
        tx.setTransactionId("tx-sub-1");
        tx.setTipoTransacao(com.aurix.platform.openfinance.entity.TransacaoConsentida.TipoTransacao.CREDITO);
        tx.setValor(new BigDecimal("750.00"));
        tx.setMoeda("BRL");
        tx.setEstabelecimento("Aurix Pagamentos");
        tx.setDescricao("TED Recebida");
        tx.setDataTransacao(LocalDateTime.of(2026, 10, 18, 12, 0));
        tx.setDataProcessamento(LocalDateTime.of(2026, 10, 18, 12, 5));
        transacaoContaRepository.save(tx);

        EmprestimoConsentido emprestimo = new EmprestimoConsentido();
        emprestimo.setConsentId(consentId);
        emprestimo.setEmprestimoId("loan-sub-1");
        emprestimo.setClienteId("12345678901");
        emprestimo.setTipoEmprestimo("CONSIGNADO");
        emprestimo.setValorContratado(new BigDecimal("18000.00"));
        emprestimo.setValorSaldoDevedor(new BigDecimal("15000.00"));
        emprestimo.setTaxaJuros(new BigDecimal("1.45"));
        emprestimo.setPrazoMeses(24);
        emprestimo.setParcelasPagas(4);
        emprestimo.setParcelasRestantes(20);
        emprestimo.setValorParcela(new BigDecimal("850.00"));
        emprestimo.setDataContratacao(LocalDate.of(2026, 6, 10));
        emprestimo.setDataVencimentoPrimeiraParcela(LocalDate.of(2026, 11, 10));
        emprestimo.setStatusEmprestimo("ATIVO");
        emprestimo.setDataAtualizacao(LocalDateTime.now());
        emprestimoRepository.save(emprestimo);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-sub-" + UUID.randomUUID(), "subject-sub", consentId, 1,
                "accounts,balances,transactions,loans,loan_installments",
                "[\"accounts:READ\",\"balances:READ\",\"transactions:READ\",\"loans:READ\",\"loan_installments:READ\"]",
                "[\"contas\",\"saldos\",\"transacoes\",\"emprestimos\",\"parcelas_emprestimo\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-sub", "sig-sub");
        authorizedContextRepository.save(context);

        DataProduct prodTransacoes = dataProductService.create(new DataProductRequest(
                "Transações Conta Open Finance", "Produto de transações em conta", "transacoes",
                "TRANSACAO", "json", "{}", "/api/v1/data-products/transacoes"));
        DataProduct prodSaldos = dataProductService.create(new DataProductRequest(
                "Saldos Open Finance", "Produto de saldos bancários", "saldos",
                "SALDO", "json", "{}", "/api/v1/data-products/saldos"));
        DataProduct prodParcelas = dataProductService.create(new DataProductRequest(
                "Parcelas Empréstimo Open Finance", "Produto de parcelas de crédito", "parcelas_emprestimo",
                "PARCELA_CREDITO", "json", "{}", "/api/v1/data-products/parcelas-emprestimo"));

        ExecutionPlanRequest.SerializableNode nodeTransacoes = new ExecutionPlanRequest.SerializableNode();
        nodeTransacoes.setNodeId("node-transacoes");
        nodeTransacoes.setCapability("extract-transacoes");
        nodeTransacoes.setResource("transacoes");
        nodeTransacoes.setDependencies(List.of());
        nodeTransacoes.setIdempotencyKey(UUID.randomUUID().toString());
        nodeTransacoes.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodeSaldos = new ExecutionPlanRequest.SerializableNode();
        nodeSaldos.setNodeId("node-saldos");
        nodeSaldos.setCapability("extract-saldos");
        nodeSaldos.setResource("saldos");
        nodeSaldos.setDependencies(List.of());
        nodeSaldos.setIdempotencyKey(UUID.randomUUID().toString());
        nodeSaldos.setTimeoutSeconds(30);

        ExecutionPlanRequest.SerializableNode nodeParcelas = new ExecutionPlanRequest.SerializableNode();
        nodeParcelas.setNodeId("node-parcelas_emprestimo");
        nodeParcelas.setCapability("extract-parcelas_emprestimo");
        nodeParcelas.setResource("parcelas_emprestimo");
        nodeParcelas.setDependencies(List.of());
        nodeParcelas.setIdempotencyKey(UUID.randomUUID().toString());
        nodeParcelas.setTimeoutSeconds(30);

        ExecutionPlanRequest plano = new ExecutionPlanRequest();
        plano.setPlanId("plan-sub-" + UUID.randomUUID());
        plano.setConsentId(consentId);
        plano.setConsentVersion(1);
        plano.setParticipantId("participant-sub");
        plano.setNodes(List.of(nodeTransacoes, nodeSaldos, nodeParcelas));

        WorkflowClient client = testEnv.getWorkflowClient();
        DataExtractionWorkflow workflow = client.newWorkflowStub(DataExtractionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ExtractionResult result = workflow.execute(plano);

        assertTrue(result.isSuccess(), "Workflow granular de transacoes, saldos e parcelas deveria ter sucesso: " + result.getErrors());
        assertEquals(3, result.getNodesProcessed());
        assertEquals(0, result.getNodesFailed());

        assertEquals(1, dataProductService.serve(prodTransacoes.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodTransacoes.getProductId(), null).getRecords().get(0).toString().contains("tx-sub-1"));

        assertEquals(1, dataProductService.serve(prodSaldos.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodSaldos.getProductId(), null).getRecords().get(0).toString().contains("bal-acc-sub-1"));

        assertEquals(1, dataProductService.serve(prodParcelas.getProductId(), null).getTotalCount());
        assertTrue(dataProductService.serve(prodParcelas.getProductId(), null).getRecords().get(0).toString().contains("inst-loan-sub-1-1"));
    }
}
