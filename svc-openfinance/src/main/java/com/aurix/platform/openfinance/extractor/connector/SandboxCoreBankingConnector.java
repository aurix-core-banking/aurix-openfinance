package com.aurix.platform.openfinance.extractor.connector;

import com.aurix.platform.openfinance.extractor.adapter.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conector de Sandbox para demonstrações B2B, homologação regulatória e testes pré-produção.
 */
@Component
public class SandboxCoreBankingConnector implements CoreBankingConnector,
        AccountSourceAdapter,
        CardSourceAdapter,
        CustomerSourceAdapter,
        InsuranceSourceAdapter,
        InvestmentSourceAdapter,
        LoanSourceAdapter,
        PixSourceAdapter {

    @Override
    public String getNome() {
        return "Conector Sandbox / Homologação BACEN";
    }

    @Override
    public TipoConector getTipo() {
        return TipoConector.SANDBOX;
    }

    @Override
    public boolean testarConectividade() {
        return true;
    }

    @Override
    public AccountSourceAdapter getAccountAdapter() {
        return this;
    }

    @Override
    public CardSourceAdapter getCardAdapter() {
        return this;
    }

    @Override
    public CustomerSourceAdapter getCustomerAdapter() {
        return this;
    }

    @Override
    public InsuranceSourceAdapter getInsuranceAdapter() {
        return this;
    }

    @Override
    public InvestmentSourceAdapter getInvestmentAdapter() {
        return this;
    }

    @Override
    public LoanSourceAdapter getLoanAdapter() {
        return this;
    }

    @Override
    public PixSourceAdapter getPixAdapter() {
        return this;
    }

    @Override
    public List<Account> findAccounts(String consentId) {
        return List.of(
                new Account("acc-sbx-001", "00000000", "BRL", "CONTA_CORRENTE", "AVAILABLE",
                        new BigDecimal("12450.50"), new BigDecimal("12450.50"), LocalDateTime.now()),
                new Account("acc-sbx-002", "00000000", "BRL", "CONTA_POUPANCA", "AVAILABLE",
                        new BigDecimal("45000.00"), new BigDecimal("45000.00"), LocalDateTime.now())
        );
    }

    @Override
    public List<Transaction> findTransactions(String consentId) {
        return List.of(
                new Transaction("acc-sbx-001", "tx-sbx-001", "PIX_RECEBIDO", new BigDecimal("1500.00"),
                        "BRL", "Empresa Parceira S/A", "Recebimento comercial", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1)),
                new Transaction("acc-sbx-001", "tx-sbx-002", "PAGAMENTO_BOLETO", new BigDecimal("-250.00"),
                        "BRL", "Energia Eletrica S/A", "Conta de consumo", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2))
        );
    }

    @Override
    public List<Card> findCards(String consentId) {
        return List.of(
                new Card("card-sbx-001", "cli-sbx-1001", "MASTERCARD", "4321", "ACTIVE",
                        new BigDecimal("15000.00"), new BigDecimal("11250.00"), new BigDecimal("3750.00"),
                        LocalDate.now().plusDays(15), LocalDateTime.now())
        );
    }

    @Override
    public List<Bill> findBills(String consentId) {
        return List.of(
                new Bill("card-sbx-001", "bill-sbx-001", new BigDecimal("3750.00"), new BigDecimal("500.00"),
                        BigDecimal.ZERO, LocalDate.now().plusDays(15), null, LocalDateTime.now())
        );
    }

    @Override
    public List<CardTransaction> findCardTransactions(String consentId) {
        return List.of(
                new CardTransaction("card-sbx-001", "ctx-sbx-001", new BigDecimal("129.90"), "BRL",
                        "Restaurante Executivo", "CREDITO_A_VISTA", LocalDateTime.now().minusDays(1))
        );
    }

    @Override
    public List<CustomerRecord> findCustomers(String consentId) {
        return List.of(
                new CustomerRecord("cli-sbx-1001", "12345678901", "PESSOA_FISICA",
                        "Cliente Sandbox Open Finance", "cliente.sandbox@aurix.com.br",
                        "11987654321", "ACTIVE", LocalDateTime.now())
        );
    }

    @Override
    public List<InsurancePolicy> findPolicies(String consentId) {
        return List.of(
                new InsurancePolicy("pol-sbx-001", "12345678901", "VIDA_INDIVIDUAL",
                        new BigDecimal("250000.00"), new BigDecimal("89.90"), "ACTIVE",
                        LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6), LocalDateTime.now())
        );
    }

    @Override
    public List<Investment> findInvestments(String consentId) {
        return List.of(
                new Investment("inv-sbx-001", "cli-sbx-1001", "RENDA_FIXA", "CDB Liquidez Diaria 110% CDI",
                        new BigDecimal("1.00"), new BigDecimal("35000.00"), new BigDecimal("35000.00"), LocalDateTime.now())
        );
    }

    @Override
    public List<Loan> findLoans(String consentId) {
        return List.of(
                new Loan("loan-sbx-001", "cli-sbx-1001", "CREDITO_PESSOAL",
                        new BigDecimal("10000.00"), new BigDecimal("6500.00"), new BigDecimal("1.89"),
                        24, 8, 16, new BigDecimal("545.20"),
                        LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(7),
                        "ACTIVE", LocalDateTime.now())
        );
    }

    @Override
    public List<PixTransaction> findPixTransactions(String consentId) {
        return List.of(
                new PixTransaction("pix-sbx-001", "cli-sbx-1001", "TRANSFERENCIA_ENVIADA",
                        "pix-teste@aurix.com.br", "EMAIL", new BigDecimal("150.00"),
                        "BRL", "Transferencia Pix Sandbox", "LIQUIDADO",
                        LocalDateTime.now().minusHours(2), LocalDateTime.now())
        );
    }
}
