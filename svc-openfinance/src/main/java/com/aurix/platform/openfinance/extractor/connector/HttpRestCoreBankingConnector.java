package com.aurix.platform.openfinance.extractor.connector;

import com.aurix.platform.openfinance.extractor.adapter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Conector REST HTTP agnóstico de Core Banking.
 */
@Component
public class HttpRestCoreBankingConnector implements CoreBankingConnector,
        AccountSourceAdapter,
        CardSourceAdapter,
        CustomerSourceAdapter,
        InsuranceSourceAdapter,
        InvestmentSourceAdapter,
        LoanSourceAdapter,
        PixSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpRestCoreBankingConnector.class);

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public HttpRestCoreBankingConnector(
            @Value("${aurix.openfinance.connector.rest.base-url:http://localhost:8081/api}") String baseUrl,
            @Value("${aurix.openfinance.connector.rest.api-key:}") String apiKey,
            @Value("${aurix.openfinance.connector.rest.timeout-ms:5000}") long timeoutMs) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeoutMs);
        factory.setReadTimeout((int) timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getNome() {
        return "Conector REST HTTP Universal";
    }

    @Override
    public TipoConector getTipo() {
        return TipoConector.REST_HTTP;
    }

    @Override
    public boolean testarConectividade() {
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                    baseUrl + "/health", HttpMethod.GET, criarEntidade(null), String.class);
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Falha no teste de conectividade do conector REST: {}", e.getMessage());
            return false;
        }
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
        return chamarApi("/accounts?consentId=" + consentId, new ParameterizedTypeReference<List<Account>>() {});
    }

    @Override
    public List<Transaction> findTransactions(String consentId) {
        return chamarApi("/transactions?consentId=" + consentId, new ParameterizedTypeReference<List<Transaction>>() {});
    }

    @Override
    public List<Card> findCards(String consentId) {
        return chamarApi("/cards?consentId=" + consentId, new ParameterizedTypeReference<List<Card>>() {});
    }

    @Override
    public List<Bill> findBills(String consentId) {
        return chamarApi("/cards/bills?consentId=" + consentId, new ParameterizedTypeReference<List<Bill>>() {});
    }

    @Override
    public List<CardTransaction> findCardTransactions(String consentId) {
        return chamarApi("/cards/transactions?consentId=" + consentId, new ParameterizedTypeReference<List<CardTransaction>>() {});
    }

    @Override
    public List<CustomerRecord> findCustomers(String consentId) {
        return chamarApi("/customers?consentId=" + consentId, new ParameterizedTypeReference<List<CustomerRecord>>() {});
    }

    @Override
    public List<InsurancePolicy> findPolicies(String consentId) {
        return chamarApi("/insurances?consentId=" + consentId, new ParameterizedTypeReference<List<InsurancePolicy>>() {});
    }

    @Override
    public List<Investment> findInvestments(String consentId) {
        return chamarApi("/investments?consentId=" + consentId, new ParameterizedTypeReference<List<Investment>>() {});
    }

    @Override
    public List<Loan> findLoans(String consentId) {
        return chamarApi("/loans?consentId=" + consentId, new ParameterizedTypeReference<List<Loan>>() {});
    }

    @Override
    public List<PixTransaction> findPixTransactions(String consentId) {
        return chamarApi("/pix/transactions?consentId=" + consentId, new ParameterizedTypeReference<List<PixTransaction>>() {});
    }

    private <T> List<T> chamarApi(String path, ParameterizedTypeReference<List<T>> typeRef) {
        try {
            ResponseEntity<List<T>> resposta = restTemplate.exchange(
                    baseUrl + path,
                    HttpMethod.GET,
                    criarEntidade(null),
                    typeRef
            );
            return resposta.getBody() != null ? resposta.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Erro ao consultar Core Banking REST em {}: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }

    private <T> HttpEntity<T> criarEntidade(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-API-Key", apiKey);
        }
        return new HttpEntity<>(body, headers);
    }
}
