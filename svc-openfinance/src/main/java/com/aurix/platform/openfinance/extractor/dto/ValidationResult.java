package com.aurix.platform.openfinance.extractor.dto;

import java.util.List;

/**
 * Resultado de validacao de dados extraidos.
 */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
