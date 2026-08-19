package com.aurix.platform.openfinance.pipeline.quality.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da verificação de qualidade de dados de um registro canônico.
 * Contém a pontuação (0-100), se passou e os problemas encontrados.
 */
public class QualityResult {

    /**
     * Pontuação mínima para aprovação.
     */
    public static final int PONTUACAO_MINIMA_APROVACAO = 80;

    /**
     * Pontuação de qualidade (0-100).
     */
    private final int score;

    /**
     * Indica se o registro passou na verificação de qualidade.
     */
    private final boolean passed;

    /**
     * Lista de problemas encontrados.
     */
    private final List<QualityIssue> issues;

    /**
     * Data e hora da verificação.
     */
    private final LocalDateTime checkedAt;

    /**
     * Construtor completo.
     *
     * @param score   pontuação.
     * @param passed  se passou.
     * @param issues  problemas.
     */
    public QualityResult(final int score, final boolean passed, final List<QualityIssue> issues) {
        this.score = Math.max(0, Math.min(100, score));
        this.passed = passed;
        this.issues = issues != null
                ? Collections.unmodifiableList(new ArrayList<>(issues))
                : Collections.emptyList();
        this.checkedAt = LocalDateTime.now();
    }

    /**
     * Cria um resultado de sucesso (100 pontos).
     *
     * @return resultado de sucesso.
     */
    public static QualityResult sucesso() {
        return new QualityResult(100, true, Collections.emptyList());
    }

    /**
     * Cria um resultado com problemas.
     *
     * @param score  pontuação.
     * @param issues problemas encontrados.
     * @return resultado com problemas.
     */
    public static QualityResult comProblemas(final int score, final List<QualityIssue> issues) {
        return new QualityResult(score, score >= PONTUACAO_MINIMA_APROVACAO, issues);
    }

    /**
     * Cria um resultado de falha.
     *
     * @param issues problemas encontrados.
     * @return resultado de falha.
     */
    public static QualityResult falha(final List<QualityIssue> issues) {
        int pontuacao = calcularPontuacao(issues);
        return new QualityResult(pontuacao, false, issues);
    }

    /**
     * Calcula a pontuação com base nos problemas encontrados.
     *
     * @param issues problemas.
     * @return pontuação calculada.
     */
    private static int calcularPontuacao(final List<QualityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return 100;
        }
        int penalidade = 0;
        for (QualityIssue issue : issues) {
            switch (issue.getSeverity()) {
                case CRITICAL:
                    penalidade += 25;
                    break;
                case ERROR:
                    penalidade += 15;
                    break;
                case WARNING:
                    penalidade += 5;
                    break;
                case INFO:
                    penalidade += 1;
                    break;
                default:
                    break;
            }
        }
        return Math.max(0, 100 - penalidade);
    }

    /**
     * Retorna a pontuação.
     *
     * @return score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Retorna se passou na verificação.
     *
     * @return passed.
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * Retorna os problemas encontrados.
     *
     * @return lista imutável de issues.
     */
    public List<QualityIssue> getIssues() {
        return issues;
    }

    /**
     * Retorna a data da verificação.
     *
     * @return checkedAt.
     */
    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    /**
     * Problema individual de qualidade.
     */
    public static class QualityIssue {
        private final String ruleId;
        private final String ruleName;
        private final QualityRule.QualityRuleType ruleType;
        private final QualityRule.QualityRuleSeverity severity;
        private final String message;
        private final String fieldPath;

        /**
         * Cria um problema de qualidade.
         *
         * @param ruleId   ID da regra.
         * @param ruleName nome da regra.
         * @param ruleType tipo da regra.
         * @param severity severidade.
         * @param message  mensagem descritiva.
         * @param fieldPath caminho do campo afetado.
         */
        public QualityIssue(final String ruleId, final String ruleName,
                final QualityRule.QualityRuleType ruleType,
                final QualityRule.QualityRuleSeverity severity,
                final String message, final String fieldPath) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.ruleType = ruleType;
            this.severity = severity;
            this.message = message;
            this.fieldPath = fieldPath;
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
         * @return ruleName.
         */
        public String getRuleName() {
            return ruleName;
        }

        /**
         * Retorna o tipo da regra.
         *
         * @return ruleType.
         */
        public QualityRule.QualityRuleType getRuleType() {
            return ruleType;
        }

        /**
         * Retorna a severidade.
         *
         * @return severity.
         */
        public QualityRule.QualityRuleSeverity getSeverity() {
            return severity;
        }

        /**
         * Retorna a mensagem descritiva.
         *
         * @return message.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Retorna o caminho do campo afetado.
         *
         * @return fieldPath.
         */
        public String getFieldPath() {
            return fieldPath;
        }
    }
}
