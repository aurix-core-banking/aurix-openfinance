package com.aurix.platform.openfinance.pipeline.quality.entity;

/**
 * Regra de qualidade de dados aplicada a registros canônicos.
 * Cada regra define um critério de validação com tipo, severidade e expressão.
 */
public class QualityRule {

    /**
     * Identificador único da regra.
     */
    private final String ruleId;

    /**
     * Nome descritivo da regra.
     */
    private final String name;

    /**
     * Tipo da regra de qualidade.
     */
    private final QualityRuleType type;

    /**
     * Severidade da regra (INFO, WARNING, ERROR, CRITICAL).
     */
    private final QualityRuleSeverity severity;

    /**
     * Expressão ou descrição da regra a ser avaliada.
     */
    private final String expression;

    /**
     * Tipo de recurso ao qual a regra se aplica (null = todos).
     */
    private final String resourceType;

    /**
     * Cria uma nova regra de qualidade.
     *
     * @param ruleId       ID da regra.
     * @param name         nome descritivo.
     * @param type         tipo da regra.
     * @param severity     severidade.
     * @param expression   expressão de validação.
     * @param resourceType tipo de recurso (null para todos).
     */
    public QualityRule(final String ruleId, final String name,
            final QualityRuleType type, final QualityRuleSeverity severity,
            final String expression, final String resourceType) {
        this.ruleId = ruleId;
        this.name = name;
        this.type = type;
        this.severity = severity;
        this.expression = expression;
        this.resourceType = resourceType;
    }

    /**
     * Retorna o ID da regra.
     *
     * @return ruleId.
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Retorna o nome da regra.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna o tipo da regra.
     *
     * @return type.
     */
    public QualityRuleType getType() {
        return type;
    }

    /**
     * Retorna a severidade da regra.
     *
     * @return severity.
     */
    public QualityRuleSeverity getSeverity() {
        return severity;
    }

    /**
     * Retorna a expressão de validação.
     *
     * @return expression.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Retorna o tipo de recurso aplicável.
     *
     * @return resourceType.
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Verifica se esta regra se aplica a um tipo de recurso específico.
     *
     * @param tipo recurso a verificar.
     * @return true se aplicável.
     */
    public boolean aplicaPara(final String tipo) {
        return resourceType == null || resourceType.equals(tipo);
    }

    /**
     * Tipos de regras de qualidade.
     */
    public enum QualityRuleType {
        /**
         * Verificação de completude — todos os campos obrigatórios presentes.
         */
        COMPLETENESS("Completude"),
        /**
         * Verificação de consistência — validações cruzadas entre campos.
         */
        CONSISTENCY("Consistência"),
        /**
         * Verificação de acurácia — valores dentro de faixas esperadas.
         */
        ACCURACY("Acurácia"),
        /**
         * Verificação de oportunidade — frescor dos dados.
         */
        TIMELINESS("Oportunidade"),
        /**
         * Verificação de unicidade — detecção de duplicatas.
         */
        UNIQUENESS("Unicidade");

        private final String descricao;

        QualityRuleType(final String desc) {
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

    /**
     * Severidade das regras de qualidade.
     */
    public enum QualityRuleSeverity {
        /**
         * Informativo — sem impacto na pontuação.
         */
        INFO,
        /**
         * Aviso — reduz pontuação levemente.
         */
        WARNING,
        /**
         * Erro — reduz pontuação significativamente.
         */
        ERROR,
        /**
         * Crítico — reprovado automaticamente.
         */
        CRITICAL
    }
}
