-- V10: Consent Versioning — INV-006 (CONSENT_VERSIONING)
-- Histórico append-only do ciclo de vida do consentimento. Cada transição de
-- estado (criação, aprovação, rejeição, revogação, expiração) é uma linha nova;
-- a versão anterior nunca é mutada. Consentimento mantém apenas o apontador do
-- estado corrente; esta tabela é a fonte de verdade auditável.

CREATE TABLE IF NOT EXISTS aurix.consentimento_versao (
    id            BIGSERIAL PRIMARY KEY,
    consent_id    VARCHAR(64) NOT NULL,
    versao        INT NOT NULL,
    status        VARCHAR(30) NOT NULL,
    data_evento   TIMESTAMP NOT NULL,
    data_aprovacao TIMESTAMP,
    motivo        VARCHAR(500),
    CONSTRAINT uk_consent_versao UNIQUE (consent_id, versao),
    CONSTRAINT fk_consent_versao_consent FOREIGN KEY (consent_id)
        REFERENCES aurix.consentimento(consent_id)
);

CREATE INDEX IF NOT EXISTS idx_consentimento_versao_consent
    ON aurix.consentimento_versao (consent_id);