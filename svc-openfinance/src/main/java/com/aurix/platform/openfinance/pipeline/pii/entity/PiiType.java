package com.aurix.platform.openfinance.pipeline.pii.entity;

/**
 * Tipos de dados pessoais identificáveis (PII) suportados pelo pipeline.
 * Utilizado para classificação e proteção de dados sensíveis no Open Finance.
 */
public enum PiiType {

    /**
     * Cadastro de Pessoa Física (11 dígitos).
     */
    CPF("CPF", "Cadastro de Pessoa Física", SensitivityLevel.RESTRICTED),

    /**
     * Cadastro Nacional da Pessoa Jurídica (14 dígitos).
     */
    CNPJ("CNPJ", "Cadastro Nacional de Pessoa Jurídica", SensitivityLevel.RESTRICTED),

    /**
     * Nome completo do titular.
     */
    NAME("NAME", "Nome Completo", SensitivityLevel.CONFIDENTIAL),

    /**
     * Endereço de e-mail.
     */
    EMAIL("EMAIL", "E-mail", SensitivityLevel.CONFIDENTIAL),

    /**
     * Número de telefone.
     */
    PHONE("PHONE", "Telefone", SensitivityLevel.CONFIDENTIAL),

    /**
     * Endereço residencial/corporativo.
     */
    ADDRESS("ADDRESS", "Endereço", SensitivityLevel.CONFIDENTIAL),

    /**
     * Número de conta bancária.
     */
    ACCOUNT_NUMBER("ACCOUNT_NUMBER", "Número de Conta", SensitivityLevel.RESTRICTED),

    /**
     * Chave PIX (CNPJ, CPF, email, telefone ou EVP).
     */
    PIX_KEY("PIX_KEY", "Chave PIX", SensitivityLevel.RESTRICTED),

    /**
     * Número do cartão de crédito/débito.
     */
    CARD_NUMBER("CARD_NUMBER", "Número do Cartão", SensitivityLevel.RESTRICTED),

    /**
     * Data de nascimento.
     */
    BIRTH_DATE("BIRTH_DATE", "Data de Nascimento", SensitivityLevel.CONFIDENTIAL),

    /**
     * Filiação (nome dos pais).
     */
    FILIATION("FILIATION", "Filiação", SensitivityLevel.RESTRICTED);

    /**
     * Código do tipo.
     */
    private final String code;

    /**
     * Descrição legível.
     */
    private final String description;

    /**
     * Nível de sensibilidade padrão.
     */
    private final SensitivityLevel defaultSensitivity;

    /**
     * Construtor do tipo PII.
     *
     * @param code              código do tipo.
     * @param description       descrição legível.
     * @param defaultSensitivity nível de sensibilidade padrão.
     */
    PiiType(final String code, final String description, final SensitivityLevel defaultSensitivity) {
        this.code = code;
        this.description = description;
        this.defaultSensitivity = defaultSensitivity;
    }

    /**
     * Retorna o código do tipo.
     *
     * @return code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Retorna a descrição.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna o nível de sensibilidade padrão.
     *
     * @return defaultSensitivity.
     */
    public SensitivityLevel getDefaultSensitivity() {
        return defaultSensitivity;
    }

    /**
     * Níveis de sensibilidade de dados PII.
     */
    public enum SensitivityLevel {
        /**
         * Dados públicos, sem restrição de exposição.
         */
        PUBLIC("Público"),
        /**
         * Dados internos, acesso restrito à organização.
         */
        INTERNAL("Interno"),
        /**
         * Dados confidenciais, acesso controlado.
         */
        CONFIDENTIAL("Confidencial"),
        /**
         * Dados restritos, criptografia obrigatória.
         */
        RESTRICTED("Restrito");

        private final String descricao;

        SensitivityLevel(final String desc) {
            this.descricao = desc;
        }

        /**
         * Retorna a descrição do nível.
         *
         * @return a descrição.
         */
        public String getDescricao() {
            return descricao;
        }
    }
}
