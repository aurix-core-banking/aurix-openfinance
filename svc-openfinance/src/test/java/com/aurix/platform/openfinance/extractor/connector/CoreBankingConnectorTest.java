package com.aurix.platform.openfinance.extractor.connector;

import com.aurix.platform.openfinance.extractor.adapter.AccountSourceAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoreBankingConnectorTest {

    @Test
    @DisplayName("SandboxConnector deve fornecer dados sintéticos realistas para todos os adaptadores")
    void deveFornecerDadosNoSandbox() {
        SandboxCoreBankingConnector sandbox = new SandboxCoreBankingConnector();

        assertThat(sandbox.getNome()).contains("Sandbox");
        assertThat(sandbox.getTipo()).isEqualTo(TipoConector.SANDBOX);
        assertThat(sandbox.testarConectividade()).isTrue();

        // Contas e Transações
        List<AccountSourceAdapter.Account> contas = sandbox.findAccounts("consent-123");
        assertThat(contas).isNotEmpty();
        assertThat(contas.get(0).accountId()).isEqualTo("acc-sbx-001");

        assertThat(sandbox.findTransactions("consent-123")).isNotEmpty();
        assertThat(sandbox.findCards("consent-123")).isNotEmpty();
        assertThat(sandbox.findBills("consent-123")).isNotEmpty();
        assertThat(sandbox.findCardTransactions("consent-123")).isNotEmpty();
        assertThat(sandbox.findCustomers("consent-123")).isNotEmpty();
        assertThat(sandbox.findPolicies("consent-123")).isNotEmpty();
        assertThat(sandbox.findInvestments("consent-123")).isNotEmpty();
        assertThat(sandbox.findLoans("consent-123")).isNotEmpty();
        assertThat(sandbox.findPixTransactions("consent-123")).isNotEmpty();
    }

    @Test
    @DisplayName("HttpRestConnector deve inicializar com propriedades e fornecer tipo REST_HTTP")
    void deveInicializarRestConnector() {
        HttpRestCoreBankingConnector restConnector = new HttpRestCoreBankingConnector(
                "http://core-externo.exemplo.com/api",
                "secret-api-key",
                3000
        );

        assertThat(restConnector.getTipo()).isEqualTo(TipoConector.REST_HTTP);
        assertThat(restConnector.getNome()).contains("REST HTTP");
        assertThat(restConnector.getAccountAdapter()).isNotNull();
        assertThat(restConnector.getCardAdapter()).isNotNull();
        assertThat(restConnector.getCustomerAdapter()).isNotNull();
        assertThat(restConnector.getInsuranceAdapter()).isNotNull();
        assertThat(restConnector.getInvestmentAdapter()).isNotNull();
        assertThat(restConnector.getLoanAdapter()).isNotNull();
        assertThat(restConnector.getPixAdapter()).isNotNull();
    }

    @Test
    @DisplayName("Registry deve resolver conector com base na propriedade configurada")
    void deveResolverConectorNoRegistry() {
        SandboxCoreBankingConnector sandbox = new SandboxCoreBankingConnector();
        HttpRestCoreBankingConnector restConnector = new HttpRestCoreBankingConnector(
                "http://localhost:8081/api", null, 1000);

        CoreBankingConnectorRegistry registrySandbox = new CoreBankingConnectorRegistry(
                List.of(sandbox, restConnector), "sandbox");
        assertThat(registrySandbox.getConectorAtivo().getTipo()).isEqualTo(TipoConector.SANDBOX);

        CoreBankingConnectorRegistry registryRest = new CoreBankingConnectorRegistry(
                List.of(sandbox, restConnector), "rest");
        assertThat(registryRest.getConectorAtivo().getTipo()).isEqualTo(TipoConector.REST_HTTP);
    }
}
