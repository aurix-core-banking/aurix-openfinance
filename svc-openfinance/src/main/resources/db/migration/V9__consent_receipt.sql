-- V9: Trust Plane — recibo de consentimento formal (não-repúdio)
-- Gera artefato imutável e verificado criptograficamente no momento da aprovação
-- (issue #1 — Trust Plane / WP2). Fecha a cadeia de rastreabilidade do
-- architecture.yaml desde o consentimento.

CREATE TABLE IF NOT EXISTS aurix.consent_receipts (
    id               BIGSERIAL PRIMARY KEY,
    receipt_id       VARCHAR(64) NOT NULL UNIQUE,
    consent_id       VARCHAR(64) NOT NULL UNIQUE,
    content_hash     VARCHAR(128) NOT NULL,
    timestamp        TIMESTAMP NOT NULL,
    signature        VARCHAR(2000) NOT NULL,
    signing_algorithm VARCHAR(30) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consent_receipts_consent_id
    ON aurix.consent_receipts (consent_id);
