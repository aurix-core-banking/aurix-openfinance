CREATE TABLE IF NOT EXISTS aurix_openfinance.investimento_consentido (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    investimento_id     VARCHAR(64) NOT NULL,
    cliente_id          VARCHAR(64) NOT NULL,
    tipo_investimento   VARCHAR(30),
    produto             VARCHAR(100),
    quantidade          NUMERIC(15,4),
    valor_unitario      NUMERIC(15,4),
    valor_total         NUMERIC(15,2),
    status_investimento VARCHAR(20),
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_investimento_consent_id
    ON aurix_openfinance.investimento_consentido(consent_id);
CREATE INDEX IF NOT EXISTS idx_investimento_investimento_id
    ON aurix_openfinance.investimento_consentido(investimento_id);
