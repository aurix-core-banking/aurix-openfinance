CREATE SCHEMA IF NOT EXISTS aurix_openfinance;

-- ============================================================
-- Fase 2 — Cartões de crédito
-- ============================================================

CREATE TABLE aurix_openfinance.cartao_consentido (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    cartao_id           VARCHAR(64) NOT NULL,
    cliente_id          VARCHAR(64),
    bandeira            VARCHAR(20),
    final_numero        VARCHAR(4),
    status_cartao       VARCHAR(20),
    limite_credito      NUMERIC(15,2),
    limite_disponivel   NUMERIC(15,2),
    valor_fatura_atual  NUMERIC(15,2),
    data_vencimento_fatura DATE,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cartao_consent_id ON aurix_openfinance.cartao_consentido(consent_id);
CREATE INDEX idx_cartao_cartao_id ON aurix_openfinance.cartao_consentido(cartao_id);

CREATE TABLE aurix_openfinance.fatura_consentida (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    cartao_id           VARCHAR(64) NOT NULL,
    fatura_id           VARCHAR(64) NOT NULL,
    valor_total         NUMERIC(15,2),
    valor_minimo        NUMERIC(15,2),
    valor_pago          NUMERIC(15,2),
    data_vencimento     DATE,
    data_pagamento      DATE,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fatura_consent_id ON aurix_openfinance.fatura_consentida(consent_id);
CREATE INDEX idx_fatura_cartao_id ON aurix_openfinance.fatura_consentida(cartao_id);

CREATE TABLE aurix_openfinance.transacao_cartao_consentida (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    cartao_id           VARCHAR(64) NOT NULL,
    transaction_id      VARCHAR(64) NOT NULL,
    valor               NUMERIC(15,2),
    moeda               VARCHAR(3),
    estabelecimento     VARCHAR(100),
    tipo_transacao      VARCHAR(20),
    data_transacao      TIMESTAMP NOT NULL,
    UNIQUE(consent_id, transaction_id)
);

CREATE INDEX idx_tx_cartao_consent_id ON aurix_openfinance.transacao_cartao_consentida(consent_id);

-- ============================================================
-- Fase 3 — Empréstimos
-- ============================================================

CREATE TABLE aurix_openfinance.emprestimo_consentido (
    id                              BIGSERIAL PRIMARY KEY,
    consent_id                      VARCHAR(64) NOT NULL,
    emprestimo_id                   VARCHAR(64) NOT NULL,
    cliente_id                      VARCHAR(64),
    tipo_emprestimo                 VARCHAR(30),
    valor_contratado                NUMERIC(15,2),
    valor_saldo_devedor             NUMERIC(15,2),
    taxa_juros                      NUMERIC(7,4),
    prazo_meses                     INTEGER,
    parcelas_pagas                  INTEGER,
    parcelas_restantes              INTEGER,
    valor_parcela                   NUMERIC(15,2),
    data_contratacao                DATE,
    data_vencimento_primeira_parcela DATE,
    status_emprestimo               VARCHAR(20),
    data_atualizacao                TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_emprestimo_consent_id ON aurix_openfinance.emprestimo_consentido(consent_id);
CREATE INDEX idx_emprestimo_emprestimo_id ON aurix_openfinance.emprestimo_consentido(emprestimo_id);

-- ============================================================
-- Fase 3 — Seguros
-- ============================================================

CREATE TABLE aurix_openfinance.seguro_consentido (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    apolice_id          VARCHAR(64) NOT NULL,
    cliente_id          VARCHAR(64),
    tipo_seguro         VARCHAR(50),
    nome_seguradora     VARCHAR(100),
    premio_mensal       NUMERIC(15,2),
    premio_total        NUMERIC(15,2),
    valor_segurado      NUMERIC(15,2),
    data_inicio         DATE,
    data_fim            DATE,
    status_apolice      VARCHAR(20),
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seguro_consent_id ON aurix_openfinance.seguro_consentido(consent_id);
CREATE INDEX idx_seguro_apolice_id ON aurix_openfinance.seguro_consentido(apolice_id);

-- ============================================================
-- Fase 3 — PIX
-- ============================================================

CREATE TABLE aurix_openfinance.pix_consentido (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(64) NOT NULL,
    pix_id              VARCHAR(64) NOT NULL,
    cliente_id          VARCHAR(64),
    tipo_pix            VARCHAR(30),
    chave_pix           VARCHAR(100),
    tipo_chave          VARCHAR(30),
    valor               NUMERIC(15,2),
    moeda               VARCHAR(3),
    descricao           VARCHAR(200),
    status_pix          VARCHAR(20),
    data_pix            TIMESTAMP NOT NULL,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(consent_id, pix_id)
);

CREATE INDEX idx_pix_consent_id ON aurix_openfinance.pix_consentido(consent_id);
CREATE INDEX idx_pix_cliente_id ON aurix_openfinance.pix_consentido(cliente_id);
