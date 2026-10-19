package com.aurix.platform.openfinance.pipeline;

/**
 * Enumeração dos tipos de recurso suportados pelo Open Finance Brasil.
 */
public enum ResourceType {

    /**
     * Conta de depósito (corrente, poupança).
     */
    CONTA("Conta"),

    /**
     * Transação/lançamento em conta.
     */
    TRANSACAO("Transação"),

    /**
     * Cartão de crédito ou débito.
     */
    CARTAO("Cartão"),

    /**
     * Dados PIX (chaves, transferências).
     */
    PIX("PIX"),

    /**
     * Produto de crédito (empréstimo, financiamento).
     */
    CREDITO("Crédito"),

    /**
     * Investimento (aplicação financeira).
     */
    INVESTIMENTO("Investimento"),

    /**
     * Fatura de cartão de crédito.
     */
    FATURA_CARTAO("Fatura de Cartão"),

    /**
     * Transação de cartão de crédito.
     */
    TRANSACAO_CARTAO("Transação de Cartão"),

    /**
     * Apólice de seguro.
     */
    SEGURO("Seguro"),

    /**
     * Dados cadastrais de pessoa física ou jurídica.
     */
    PESSOA("Pessoa");

    /**
     * Descrição do tipo.
     */
    private final String descricao;

    ResourceType(final String desc) {
        this.descricao = desc;
    }

    /**
     * Retorna a descrição do tipo.
     *
     * @return a descrição.
     */
    public String getDescricao() {
        return descricao;
    }
}
