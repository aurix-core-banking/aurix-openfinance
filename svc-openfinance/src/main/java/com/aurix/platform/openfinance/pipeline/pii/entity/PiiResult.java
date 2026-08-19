package com.aurix.platform.openfinance.pipeline.pii.entity;

import com.aurix.platform.openfinance.pipeline.pii.entity.PiiType.SensitivityLevel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da classificação e proteção de dados PII.
 * Contém o nível de sensibilidade geral, campos protegidos e o registro mascarado.
 */
public class PiiResult {

    /**
     * Data e hora da classificação.
     */
    private final LocalDateTime classifiedAt;

    /**
     * Nível de sensibilidade mais alto encontrado no registro.
     */
    private final SensitivityLevel sensitivityLevel;

    /**
     * Lista de campos PII identificados e protegidos.
     */
    private final List<PiiField> protectedFields;

    /**
     * Registro com dados mascarados/criptografados.
     */
    private final String maskedRecord;

    /**
     * Construtor completo.
     *
     * @param classifiedAt      data da classificação.
     * @param sensitivityLevel  nível de sensibilidade.
     * @param protectedFields   campos protegidos.
     * @param maskedRecord      registro mascarado.
     */
    public PiiResult(final LocalDateTime classifiedAt, final SensitivityLevel sensitivityLevel,
            final List<PiiField> protectedFields, final String maskedRecord) {
        this.classifiedAt = classifiedAt;
        this.sensitivityLevel = sensitivityLevel;
        this.protectedFields = protectedFields != null
                ? Collections.unmodifiableList(new ArrayList<>(protectedFields))
                : Collections.emptyList();
        this.maskedRecord = maskedRecord;
    }

    /**
     * Cria um resultado de classificação PII.
     *
     * @param sensitivityLevel  nível de sensibilidade.
     * @param protectedFields   campos protegidos.
     * @param maskedRecord      registro mascarado.
     * @return novo PiiResult.
     */
    public static PiiResult criar(final SensitivityLevel sensitivityLevel,
            final List<PiiField> protectedFields, final String maskedRecord) {
        return new PiiResult(LocalDateTime.now(), sensitivityLevel, protectedFields, maskedRecord);
    }

    /**
     * Cria um resultado sem PII detectado.
     *
     * @return resultado sem PII.
     */
    public static PiiResult semPii(final String recordData) {
        return new PiiResult(LocalDateTime.now(), SensitivityLevel.PUBLIC,
                Collections.emptyList(), recordData);
    }

    /**
     * Retorna a data de classificação.
     *
     * @return classifiedAt.
     */
    public LocalDateTime getClassifiedAt() {
        return classifiedAt;
    }

    /**
     * Retorna o nível de sensibilidade.
     *
     * @return sensitivityLevel.
     */
    public SensitivityLevel getSensitivityLevel() {
        return sensitivityLevel;
    }

    /**
     * Retorna os campos protegidos.
     *
     * @return lista imutável de PiiField.
     */
    public List<PiiField> getProtectedFields() {
        return protectedFields;
    }

    /**
     * Retorna o registro mascarado.
     *
     * @return maskedRecord.
     */
    public String getMaskedRecord() {
        return maskedRecord;
    }

    /**
     * Representa um campo PII identificado e sua proteção aplicada.
     */
    public static class PiiField {
        private final String fieldName;
        private final PiiType piiType;
        private final String originalValue;
        private final String protectedValue;
        private final ProtectionStrategy strategy;

        /**
         * Cria um campo PII protegido.
         *
         * @param fieldName      nome do campo.
         * @param piiType        tipo PII.
         * @param originalValue  valor original.
         * @param protectedValue valor protegido.
         * @param strategy       estratégia de proteção.
         */
        public PiiField(final String fieldName, final PiiType piiType,
                final String originalValue, final String protectedValue,
                final ProtectionStrategy strategy) {
            this.fieldName = fieldName;
            this.piiType = piiType;
            this.originalValue = originalValue;
            this.protectedValue = protectedValue;
            this.strategy = strategy;
        }

        /**
         * Retorna o nome do campo.
         *
         * @return fieldName.
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Retorna o tipo PII.
         *
         * @return piiType.
         */
        public PiiType getPiiType() {
            return piiType;
        }

        /**
         * Retorna o valor original (para auditoria interna).
         *
         * @return originalValue.
         */
        public String getOriginalValue() {
            return originalValue;
        }

        /**
         * Retorna o valor protegido.
         *
         * @return protectedValue.
         */
        public String getProtectedValue() {
            return protectedValue;
        }

        /**
         * Retorna a estratégia de proteção aplicada.
         *
         * @return strategy.
         */
        public ProtectionStrategy getStrategy() {
            return strategy;
        }

        /**
         * Estratégias de proteção de dados PII.
         */
        public enum ProtectionStrategy {
            /**
             * Mascaramento parcial (ex: ***.***.***-**).
             */
            MASK,
            /**
             * Hash unidirecional (SHA-256).
             */
            HASH,
            /**
             * Criptografia bidirecional (AES-256).
             */
            ENCRYPT,
            /**
             * Tokenização (substituição por token).
             */
            TOKENIZE
        }
    }
}
