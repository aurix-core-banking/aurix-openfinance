package com.aurix.platform.openfinance.pipeline.quality.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityResult;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityResult.QualityIssue;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityRule;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityRule.QualityRuleSeverity;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityRule.QualityRuleType;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de verificação de qualidade de dados do pipeline Open Finance.
 * Aplica regras de completude, consistência, acurácia, oportunidade e unicidade
 * a registros canônicos.
 */
@Service
public class DataQualityService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataQualityService.class);
    private static final int MAX_TIMELINESS_HOURS = 24;
    private static final Map<String, LocalDateTime> recentRecords = new ConcurrentHashMap<>();

    /**
     * Verifica a qualidade de um registro canônico.
     *
     * @param record registro canônico.
     * @param type   tipo de recurso.
     * @return resultado da verificação de qualidade.
     */
    public QualityResult checkQuality(final CanonicalRecord record, final ResourceType type) {
        log.info("Verificando qualidade do registro {} tipo {}", record.getCanonicalId(), type);

        List<QualityRule> rules = getRules(type);
        List<QualityIssue> issues = new ArrayList<>();

        for (QualityRule rule : rules) {
            List<QualityIssue> ruleIssues = avaliarRegra(record, rule);
            issues.addAll(ruleIssues);
        }

        int pontuacao = calcularPontuacao(issues);
        boolean passed = pontuacao >= QualityResult.PONTUACAO_MINIMA_APROVACAO;

        log.info("Qualidade verificada: pontuação={}, aprovado={}, problemas={}",
                pontuacao, passed, issues.size());
        return QualityResult.comProblemas(pontuacao, issues);
    }

    /**
     * Retorna as regras de qualidade aplicáveis para um tipo de recurso.
     *
     * @param type tipo de recurso.
     * @return lista de regras.
     */
    private List<QualityRule> getRules(final ResourceType type) {
        List<QualityRule> rules = new ArrayList<>();

        // Regras de completude (aplicam a todos os tipos)
        rules.add(new QualityRule("COMP001", "ID Canônico obrigatório",
                QualityRuleType.COMPLETENESS, QualityRuleSeverity.CRITICAL,
                "canonicalId IS NOT NULL", null));
        rules.add(new QualityRule("COMP002", "ID Registro Bruto obrigatório",
                QualityRuleType.COMPLETENESS, QualityRuleSeverity.CRITICAL,
                "rawRecordId IS NOT NULL", null));
        rules.add(new QualityRule("COMP003", "Dados Canônicos obrigatórios",
                QualityRuleType.COMPLETENESS, QualityRuleSeverity.CRITICAL,
                "canonicalData IS NOT NULL", null));
        rules.add(new QualityRule("COMP004", "Checksum obrigatório",
                QualityRuleType.COMPLETENESS, QualityRuleSeverity.ERROR,
                "checksum IS NOT NULL", null));
        rules.add(new QualityRule("COMP005", "Versão do modelo obrigatória",
                QualityRuleType.COMPLETENESS, QualityRuleSeverity.ERROR,
                "version IS NOT NULL", null));

        // Regras de consistência
        rules.add(new QualityRule("CONS001", "Versão formato válido",
                QualityRuleType.CONSISTENCY, QualityRuleSeverity.WARNING,
                "version MATCHES '^\\d+\\.\\d+$'", null));
        rules.add(new QualityRule("CONS002", "Checksum formato SHA-256",
                QualityRuleType.CONSISTENCY, QualityRuleSeverity.WARNING,
                "checksum LENGTH 64", null));

        // Regras de acurácia
        rules.add(new QualityRule("ACUR001", "Dados canônicos não vazio",
                QualityRuleType.ACCURACY, QualityRuleSeverity.CRITICAL,
                "canonicalData NOT EMPTY", null));
        rules.add(new QualityRule("ACUR002", "Checksum correspondente",
                QualityRuleType.ACCURACY, QualityRuleSeverity.ERROR,
                "checksum MATCHES calculated", null));

        // Regras de oportunidade
        rules.add(new QualityRule("OPOR001", "Dados extraídos há menos de 24h",
                QualityRuleType.TIMELINESS, QualityRuleSeverity.WARNING,
                "extractedAt WITHIN 24h", null));

        // Regras específicas por tipo
        if (type == ResourceType.CONTA) {
            rules.add(new QualityRule("CONTA001", "Tipo de conta válido",
                    QualityRuleType.ACCURACY, QualityRuleSeverity.ERROR,
                    "tipoConta IN (CORRENTE, POUPANCA, SALARIO)", "CONTA"));
        } else if (type == ResourceType.TRANSACAO) {
            rules.add(new QualityRule("TRAN001", "Tipo de transação válido",
                    QualityRuleType.ACCURACY, QualityRuleSeverity.ERROR,
                    "tipoTransacao IN (CREDIT, DEBIT)", "TRANSACAO"));
        } else if (type == ResourceType.CARTAO) {
            rules.add(new QualityRule("CART001", "Bandeira do cartão válida",
                    QualityRuleType.ACCURACY, QualityRuleSeverity.WARNING,
                    "bandeira NOT EMPTY", "CARTAO"));
        } else if (type == ResourceType.PIX) {
            rules.add(new QualityRule("PIX001", "Chave PIX formato válido",
                    QualityRuleType.ACCURACY, QualityRuleSeverity.ERROR,
                    "chavePix FORMAT VALID", "PIX"));
        }

        return rules;
    }

    /**
     * Avalia uma regra específica contra o registro.
     *
     * @param record registro canônico.
     * @param rule   regra a avaliar.
     * @return lista de problemas encontrados.
     */
    private List<QualityIssue> avaliarRegra(final CanonicalRecord record, final QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();

        switch (rule.getRuleId()) {
            case "COMP001":
                if (record.getCanonicalId() == null || record.getCanonicalId().isBlank()) {
                    issues.add(criarIssue(rule, "ID canônico ausente", "canonicalId"));
                }
                break;
            case "COMP002":
                if (record.getRawRecordId() == null || record.getRawRecordId().isBlank()) {
                    issues.add(criarIssue(rule, "ID do registro bruto ausente", "rawRecordId"));
                }
                break;
            case "COMP003":
                if (record.getCanonicalData() == null || record.getCanonicalData().isBlank()) {
                    issues.add(criarIssue(rule, "Dados canônicos ausentes", "canonicalData"));
                }
                break;
            case "COMP004":
                if (record.getChecksum() == null || record.getChecksum().isBlank()) {
                    issues.add(criarIssue(rule, "Checksum ausente", "checksum"));
                }
                break;
            case "COMP005":
                if (record.getVersion() == null || record.getVersion().isBlank()) {
                    issues.add(criarIssue(rule, "Versão ausente", "version"));
                }
                break;
            case "CONS001":
                if (record.getVersion() != null && !record.getVersion().matches("^\\d+\\.\\d+$")) {
                    issues.add(criarIssue(rule, "Formato de versão inválido: " + record.getVersion(), "version"));
                }
                break;
            case "CONS002":
                if (record.getChecksum() != null && record.getChecksum().length() != 64) {
                    issues.add(criarIssue(rule, "Checksum não é SHA-256 (esperado 64 hex)", "checksum"));
                }
                break;
            case "ACUR001":
                if (record.getCanonicalData() != null && record.getCanonicalData().trim().isEmpty()) {
                    issues.add(criarIssue(rule, "Dados canônicos vazios", "canonicalData"));
                }
                break;
            case "OPOR001":
                if (record.getCanonicalizedAt() != null) {
                    long horas = ChronoUnit.HOURS.between(record.getCanonicalizedAt(), LocalDateTime.now());
                    if (horas > MAX_TIMELINESS_HOURS) {
                        issues.add(criarIssue(rule,
                                "Dados canonicalizados há " + horas + " horas (limite: " + MAX_TIMELINESS_HOURS + "h)",
                                "canonicalizedAt"));
                    }
                }
                break;
            default:
                break;
        }

        return issues;
    }

    /**
     * Cria um problema de qualidade a partir de uma regra.
     *
     * @param rule    regra que falhou.
     * @param message mensagem descritiva.
     * @param field   campo afetado.
     * @return problema de qualidade.
     */
    private QualityIssue criarIssue(final QualityRule rule, final String message, final String field) {
        return new QualityIssue(
                rule.getRuleId(), rule.getName(), rule.getType(),
                rule.getSeverity(), message, field);
    }

    /**
     * Calcula a pontuação final com base nos problemas.
     *
     * @param issues problemas encontrados.
     * @return pontuação (0-100).
     */
    private int calcularPontuacao(final List<QualityIssue> issues) {
        if (issues.isEmpty()) {
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
}
