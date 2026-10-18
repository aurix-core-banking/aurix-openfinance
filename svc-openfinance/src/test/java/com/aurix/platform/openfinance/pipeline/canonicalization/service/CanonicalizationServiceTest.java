package com.aurix.platform.openfinance.pipeline.canonicalization.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Prova a canonicalização real dos 4 tipos de recurso conforme
 * {@code architecture/data-models.yaml} — mapeamento de campos (aliases
 * camelCase/snake_case), enums canônicos e normalização.
 */
class CanonicalizationServiceTest {

    private CanonicalizationService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CanonicalizationService();
    }

    @Test
    void canonicalizeContaMapeiaCamposDoCoreParaCanonico() throws Exception {
        String bruta = """
                {"tipoConta":"CORRENTE","numeroConta":"123456","agencia":"0001",
                 "cpfCnpj":"12345678909","statusConta":"ATIVA",
                 "dataAbertura":"2023-05-10T08:00:00","idConta":"acct-1"}
                """;
        JsonNode resultado = mapper.readTree(service.canonicalizeConta(bruta));
        assertEquals("acct-1", resultado.get("accountId").asText());
        assertEquals("CONTA_CORRENTE", resultado.get("accountType").asText());
        assertEquals("123456", resultado.get("number").asText());
        assertEquals("0001", resultado.get("agency").asText());
        assertEquals("12345678909", resultado.get("documentNumber").asText());
        assertEquals("ATIVA", resultado.get("status").asText());
        assertEquals("2023-05-10T08:00:00", resultado.get("createdAt").asText());
    }

    @Test
    void canonicalizeContaResolveEnumTipoPoupanca() throws Exception {
        String bruta = "{\"tipo\":\"POUPANCA\",\"contaId\":\"acct-2\"}";
        JsonNode resultado = mapper.readTree(service.canonicalizeConta(bruta));
        assertEquals("POUPANCA", resultado.get("accountType").asText());
    }

    @Test
    void canonicalizeTransacaoMapeiaEClassificaTipoCreditoDebito() throws Exception {
        String bruta = """
                {"idTransacao":"tx-1","contaId":"acct-1","tipoTransacao":"CREDITO",
                 "valor":"150,75","descricao":"PIX recebido",
                 "mcc":"6011","dataTransacao":"2024-01-15T12:30:00"}
                """;
        JsonNode resultado = mapper.readTree(service.canonicalizeTransacao(bruta));
        assertEquals("tx-1", resultado.get("transactionId").asText());
        assertEquals("acct-1", resultado.get("accountId").asText());
        assertEquals("CREDITO", resultado.get("type").asText());
        assertEquals("150.75", resultado.get("amount").asText());
        assertEquals("PIX recebido", resultado.get("description").asText());
        assertEquals("6011", resultado.get("merchantCategoryCode").asText());
        assertEquals("2024-01-15T12:30:00", resultado.get("timestamp").asText());
    }

    @Test
    void canonicalizeTransacaoNormalizaTipoComAcento() throws Exception {
        String bruta = "{\"tipo\":\"DÉBITO\",\"transacaoId\":\"tx-2\",\"contaId\":\"acct-1\",\"valor\":\"10\"}";
        JsonNode resultado = mapper.readTree(service.canonicalizeTransacao(bruta));
        assertEquals("DEBITO", resultado.get("type").asText());
    }

    @Test
    void canonicalizeCartaoMapeiaBandeiraEstadoEUsoLimites() throws Exception {
        String bruta = """
                {"idCartao":"card-1","bandeira":"MASTER","numero":"4111111111111111",
                 "statusCartao":"ATIVO","limiteCredito":"5000","limiteDisponivel":"3200,50",
                 "cpf":"12345678909"}
                """;
        JsonNode resultado = mapper.readTree(service.canonicalizeCartao(bruta));
        assertEquals("card-1", resultado.get("cardId").asText());
        assertEquals("MASTERCARD", resultado.get("brand").asText());
        assertEquals("4111111111111111", resultado.get("cardNumber").asText());
        assertEquals("ATIVO", resultado.get("status").asText());
        assertEquals("5000", resultado.get("creditLimit").asText());
        assertEquals("3200.50", resultado.get("availableLimit").asText());
        assertEquals("12345678909", resultado.get("documentNumber").asText());
    }

    @Test
    void canonicalizePixMapeiaFluxoPagadorRecebedorEStatus() throws Exception {
        String bruta = """
                {"id":"pix-1","e2eId":"E123456789012345678901234567890123",
                 "contaOrigem":"acct-orig","contaDestino":"acct-dest",
                 "valor":"99,99","situacao":"LIQUIDADO","dataRegistro":"2024-02-01T09:00:00"}
                """;
        JsonNode resultado = mapper.readTree(service.canonicalizePix(bruta));
        assertEquals("pix-1", resultado.get("pixId").asText());
        assertEquals("E123456789012345678901234567890123", resultado.get("endToEndId").asText());
        assertEquals("acct-orig", resultado.get("payerAccountId").asText());
        assertEquals("acct-dest", resultado.get("payeeAccountId").asText());
        assertEquals("99.99", resultado.get("amount").asText());
        assertEquals("LIQUIDADO", resultado.get("status").asText());
    }

    @Test
    void camposAusentesSaoNulosMasJsonPermaneceValido() throws Exception {
        JsonNode resultado = mapper.readTree(service.canonicalizeConta("{}"));
        assertTrue(resultado.has("accountId"));
        assertTrue(resultado.get("accountId").isNull());
        assertTrue(resultado.has("accountType"));
    }

    @Test
    void jsonInvalidoMantemConteudoOriginalECanonicalRecordGeraChecksum() throws Exception {
        String invalido = "nao-e-json";
        RawRecord raw = RawRecord.criar("rec-1", "CORE_BANKING", "ext-1",
                ResourceType.CONTA, invalido, LocalDateTime.now(), "1.0");
        CanonicalRecord record = service.canonicalize(raw, ResourceType.CONTA);
        assertEquals(invalido, record.getCanonicalData());
        assertNotNull(record.getChecksum());
        assertFalse(record.getChecksum().isBlank());
    }

    @Test
    void matrizDeArraysUtilizaPrimeiraPosicao() throws Exception {
        String bruta = "[{\"contaId\":\"acct-1\",\"tipoConta\":\"SALARIO\"},{\"contaId\":\"acct-2\"}]";
        JsonNode resultado = mapper.readTree(service.canonicalizeConta(bruta));
        assertEquals("acct-1", resultado.get("accountId").asText());
    }

    @Test
    void canonicalizeDelegadoPorTipoProduzChecksumEVersion() throws Exception {
        RawRecord raw = RawRecord.criar("rec-2", "CORE_BANKING", "ext-2",
                ResourceType.CARTAO, "{\"idCartao\":\"card-9\",\"bandeira\":\"VISA\"}",
                LocalDateTime.now(), "1.0");
        CanonicalRecord record = service.canonicalize(raw, ResourceType.CARTAO);
        assertEquals("card-9", mapper.readTree(record.getCanonicalData()).get("cardId").asText());
        assertEquals("VISA", mapper.readTree(record.getCanonicalData()).get("brand").asText());
        assertEquals("1.0", record.getVersion());
        assertNotNull(record.getChecksum());
    }

    @Test
    void canonicalizeDesempacotaEnvelopeDoExtractorDeContas() throws Exception {
        String envelopeExtractor = """
                {"contas":[{"accountId":"acc-e2e-1","institutionCode":"AURIX","currency":"BRL",
                            "type":"CONTA_DE_DEPOSITO","status":"ACTIVE","updatedAt":"2026-08-30T10:00:00"}],
                 "transacoes":[]}
                """;
        RawRecord raw = RawRecord.criar("rec-env-1", "AURIX_CORE", "node-contas",
                ResourceType.CONTA, envelopeExtractor, LocalDateTime.now(), "1.0");

        CanonicalRecord record = service.canonicalize(raw, ResourceType.CONTA);
        JsonNode resultado = mapper.readTree(record.getCanonicalData());

        assertEquals("acc-e2e-1", resultado.get("accountId").asText());
        assertEquals("CONTA_CORRENTE", resultado.get("accountType").asText());
        assertEquals("ATIVA", resultado.get("status").asText());
        assertEquals("2026-08-30T10:00:00", resultado.get("createdAt").asText());
    }

    @Test
    void canonicalizeCreditoMapeiaCamposEConverteEnums() throws Exception {
        String envelopeEmprestimo = """
                {"emprestimos":[{"loanId":"loan-1","customerId":"12345678909",
                                 "type":"CREDITO_PESSOAL","contractedAmount":"25000,00",
                                 "interestRate":"1,85","termMonths":36,"status":"ACTIVE"}]}
                """;
        RawRecord raw = RawRecord.criar("rec-loan-1", "AURIX_CORE", "node-emprestimos",
                ResourceType.CREDITO, envelopeEmprestimo, LocalDateTime.now(), "1.0");

        CanonicalRecord record = service.canonicalize(raw, ResourceType.CREDITO);
        JsonNode resultado = mapper.readTree(record.getCanonicalData());

        assertEquals("loan-1", resultado.get("loanId").asText());
        assertEquals("12345678909", resultado.get("documentNumber").asText());
        assertEquals("PESSOAL", resultado.get("type").asText());
        assertEquals("25000.00", resultado.get("principalAmount").asText());
        assertEquals("1.85", resultado.get("interestRate").asText());
        assertEquals("36", resultado.get("termMonths").asText());
        assertEquals("ATIVO", resultado.get("status").asText());
    }

    @Test
    void canonicalizeInvestimentoMapeiaCamposEConverteEnums() throws Exception {
        String envelopeInvestimento = """
                {"investimentos":[{"investmentId":"inv-1","customerId":"12345678909",
                                   "type":"TESOURO_DIRETO","totalValue":"10500,50",
                                   "unitValue":"10000,00","status":"ACTIVE"}]}
                """;
        RawRecord raw = RawRecord.criar("rec-inv-1", "AURIX_CORE", "node-investimentos",
                ResourceType.INVESTIMENTO, envelopeInvestimento, LocalDateTime.now(), "1.0");

        CanonicalRecord record = service.canonicalize(raw, ResourceType.INVESTIMENTO);
        JsonNode resultado = mapper.readTree(record.getCanonicalData());

        assertEquals("inv-1", resultado.get("investmentId").asText());
        assertEquals("12345678909", resultado.get("documentNumber").asText());
        assertEquals("TESOURO", resultado.get("type").asText());
        assertEquals("10500.50", resultado.get("currentValue").asText());
        assertEquals("10000.00", resultado.get("appliedAmount").asText());
        assertEquals("ATIVO", resultado.get("status").asText());
    }
}