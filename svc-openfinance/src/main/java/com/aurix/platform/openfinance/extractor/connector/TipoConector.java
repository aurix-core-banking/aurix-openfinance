package com.aurix.platform.openfinance.extractor.connector;

/**
 * Tipos de conectores de core bancário suportados pela plataforma.
 */
public enum TipoConector {

    /**
     * Conector nativo do monorepo Aurix (banco e eventos compartilhados).
     */
    AURIX_NATIVO,

    /**
     * Conector agnóstico via APIs REST HTTP para qualquer banco ou core legado.
     */
    REST_HTTP,

    /**
     * Conector simulado com dados sintéticos realistas para onboarding, homologação e vendas.
     */
    SANDBOX
}
