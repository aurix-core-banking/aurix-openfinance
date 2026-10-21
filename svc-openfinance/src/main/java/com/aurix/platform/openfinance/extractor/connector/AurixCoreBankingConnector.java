package com.aurix.platform.openfinance.extractor.connector;

import com.aurix.platform.openfinance.extractor.adapter.*;
import com.aurix.platform.openfinance.extractor.adapter.aurixcore.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Conector para o Core Banking Nativo do Aurix (banco de dados compartilhado e mensageria Kafka).
 */
@Component
@Primary
public class AurixCoreBankingConnector implements CoreBankingConnector {

    private final AurixCoreAccountSourceAdapter accountAdapter;
    private final AurixCoreCardSourceAdapter cardAdapter;
    private final AurixCoreCustomerSourceAdapter customerAdapter;
    private final AurixCoreInsuranceSourceAdapter insuranceAdapter;
    private final AurixCoreInvestmentSourceAdapter investmentAdapter;
    private final AurixCoreLoanSourceAdapter loanAdapter;
    private final AurixCorePixSourceAdapter pixAdapter;

    public AurixCoreBankingConnector(
            AurixCoreAccountSourceAdapter accountAdapter,
            AurixCoreCardSourceAdapter cardAdapter,
            AurixCoreCustomerSourceAdapter customerAdapter,
            AurixCoreInsuranceSourceAdapter insuranceAdapter,
            AurixCoreInvestmentSourceAdapter investmentAdapter,
            AurixCoreLoanSourceAdapter loanAdapter,
            AurixCorePixSourceAdapter pixAdapter) {
        this.accountAdapter = accountAdapter;
        this.cardAdapter = cardAdapter;
        this.customerAdapter = customerAdapter;
        this.insuranceAdapter = insuranceAdapter;
        this.investmentAdapter = investmentAdapter;
        this.loanAdapter = loanAdapter;
        this.pixAdapter = pixAdapter;
    }

    @Override
    public String getNome() {
        return "Conector Core Banking Aurix Nativo";
    }

    @Override
    public TipoConector getTipo() {
        return TipoConector.AURIX_NATIVO;
    }

    @Override
    public boolean testarConectividade() {
        return true;
    }

    @Override
    public AccountSourceAdapter getAccountAdapter() {
        return accountAdapter;
    }

    @Override
    public CardSourceAdapter getCardAdapter() {
        return cardAdapter;
    }

    @Override
    public CustomerSourceAdapter getCustomerAdapter() {
        return customerAdapter;
    }

    @Override
    public InsuranceSourceAdapter getInsuranceAdapter() {
        return insuranceAdapter;
    }

    @Override
    public InvestmentSourceAdapter getInvestmentAdapter() {
        return investmentAdapter;
    }

    @Override
    public LoanSourceAdapter getLoanAdapter() {
        return loanAdapter;
    }

    @Override
    public PixSourceAdapter getPixAdapter() {
        return pixAdapter;
    }
}
