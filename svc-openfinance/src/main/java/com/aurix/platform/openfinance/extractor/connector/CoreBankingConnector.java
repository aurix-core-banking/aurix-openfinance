package com.aurix.platform.openfinance.extractor.connector;

import com.aurix.platform.openfinance.extractor.adapter.*;

/**
 * Interface unificadora de conectores de core bancário (SPI).
 *
 * <p>Permite que a solução Open Finance seja acoplada a qualquer sistema bancário
 * de mercado (Matera, Totvs, Sinqia, Mambu, APIs proprietárias ou ambiente de Sandbox),
 * sem que nenhum componente interno do pipeline regulatório precise ser alterado.
 */
public interface CoreBankingConnector {

    /**
     * Nome identificador do conector (ex: "REST Connector", "Sandbox Provider", "Aurix Core").
     */
    String getNome();

    /**
     * Tipo do conector em execução.
     */
    TipoConector getTipo();

    /**
     * Testa a conectividade com o sistema bancário de origem.
     *
     * @return true se o core estiver respondendo normalmente
     */
    boolean testarConectividade();

    /**
     * Adaptador para contas e transações correntes/poupança.
     */
    AccountSourceAdapter getAccountAdapter();

    /**
     * Adaptador para cartões de crédito e faturas.
     */
    CardSourceAdapter getCardAdapter();

    /**
     * Adaptador para dados cadastrais de clientes PF/PJ.
     */
    CustomerSourceAdapter getCustomerAdapter();

    /**
     * Adaptador para apólices de seguro.
     */
    InsuranceSourceAdapter getInsuranceAdapter();

    /**
     * Adaptador para posições de investimento.
     */
    InvestmentSourceAdapter getInvestmentAdapter();

    /**
     * Adaptador para contratos de empréstimo e financiamento.
     */
    LoanSourceAdapter getLoanAdapter();

    /**
     * Adaptador para pagamentos e transações Pix.
     */
    PixSourceAdapter getPixAdapter();
}
