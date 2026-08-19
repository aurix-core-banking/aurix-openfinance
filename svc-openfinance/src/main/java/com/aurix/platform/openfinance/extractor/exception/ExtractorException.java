package com.aurix.platform.openfinance.extractor.exception;

/**
 * Excecao lancada por extractors em caso de falha.
 */
public class ExtractorException extends RuntimeException {

    public ExtractorException(String message) {
        super(message);
    }

    public ExtractorException(String message, Throwable cause) {
        super(message, cause);
    }
}
