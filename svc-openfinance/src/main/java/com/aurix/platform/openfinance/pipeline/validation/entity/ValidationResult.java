package com.aurix.platform.openfinance.pipeline.validation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da validação de schema de um registro canônico.
 * Contém o status da validação, erros e avisos encontrados.
 */
public class ValidationResult {

    /**
     * Versão do schema contra o qual foi validado.
     */
    private final String schemaVersion;

    /**
     * Data e hora da validação.
     */
    private final LocalDateTime validatedAt;

    /**
     * Indica se o registro é válido (sem erros).
     */
    private final boolean valid;

    /**
     * Lista de erros encontrados durante a validação.
     */
    private final List<String> errors;

    /**
     * Lista de avisos (campos opcionais ausentes, valores inexatos).
     */
    private final List<String> warnings;

    /**
     * Construtor privado para builder pattern.
     *
     * @param builder instância do builder.
     */
    private ValidationResult(final Builder builder) {
        this.schemaVersion = builder.schemaVersion;
        this.validatedAt = builder.validatedAt;
        this.valid = builder.valid;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    }

    /**
     * Construtor padrão para validação bem-sucedida.
     *
     * @param schemaVersion versão do schema.
     */
    public ValidationResult(final String schemaVersion) {
        this.schemaVersion = schemaVersion;
        this.validatedAt = LocalDateTime.now();
        this.valid = true;
        this.errors = Collections.emptyList();
        this.warnings = Collections.emptyList();
    }

    /**
     * Construtor com erros.
     *
     * @param schemaVersion versão do schema.
     * @param errors        lista de erros.
     */
    public ValidationResult(final String schemaVersion, final List<String> errors) {
        this.schemaVersion = schemaVersion;
        this.validatedAt = LocalDateTime.now();
        this.valid = errors == null || errors.isEmpty();
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<>(errors))
                : Collections.emptyList();
        this.warnings = Collections.emptyList();
    }

    /**
     * Cria uma instância de resultado válido.
     *
     * @param schemaVersion versão do schema.
     * @return resultado válido.
     */
    public static ValidationResult valido(final String schemaVersion) {
        return new ValidationResult(schemaVersion);
    }

    /**
     * Cria uma instância de resultado inválido.
     *
     * @param schemaVersion versão do schema.
     * @param errors        lista de erros.
     * @return resultado inválido.
     */
    public static ValidationResult invalido(final String schemaVersion, final List<String> errors) {
        return new ValidationResult(schemaVersion, errors);
    }

    /**
     * Retorna se o registro é válido.
     *
     * @return true se válido.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Retorna a lista de erros.
     *
     * @return lista imutável de erros.
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Retorna a lista de avisos.
     *
     * @return lista imutável de avisos.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Retorna a versão do schema.
     *
     * @return schemaVersion.
     */
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Retorna a data de validação.
     *
     * @return validatedAt.
     */
    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    /**
     * Builder para ValidationResult.
     */
    public static class Builder {
        private final String schemaVersion;
        private boolean valid = true;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final LocalDateTime validatedAt = LocalDateTime.now();

        /**
         * Cria um builder com a versão do schema.
         *
         * @param schemaVersion versão do schema.
         */
        public Builder(final String schemaVersion) {
            this.schemaVersion = schemaVersion;
        }

        /**
         * Adiciona um erro e marca como inválido.
         *
         * @param error mensagem de erro.
         * @return this.
         */
        public Builder addError(final String error) {
            this.errors.add(error);
            this.valid = false;
            return this;
        }

        /**
         * Adiciona um avio.
         *
         * @param warning mensagem de aviso.
         * @return this.
         */
        public Builder addWarning(final String warning) {
            this.warnings.add(warning);
            return this;
        }

        /**
         * Constrói o resultado.
         *
         * @return ValidationResult.
         */
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
