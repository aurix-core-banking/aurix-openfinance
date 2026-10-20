CREATE SCHEMA IF NOT EXISTS aurix;

-- Tabela de consentimentos Open Finance
CREATE TABLE aurix.consentimento (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL UNIQUE,
    client_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_AUTHORISATION',
    permissions TEXT NOT NULL DEFAULT 'accounts',
    data_criacao TIMESTAMP NOT NULL,
    data_aprovacao TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    motivo_rejeicao VARCHAR(500),
    version INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_consentimento_user_status ON aurix.consentimento(user_id, status);
CREATE INDEX idx_consentimento_client ON aurix.consentimento(client_id);
CREATE INDEX idx_consentimento_status ON aurix.consentimento(status);

-- Tabela de contas consentidas
CREATE TABLE aurix.conta_consentida (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    institution_code VARCHAR(20) NOT NULL,
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    tipo_conta VARCHAR(20) NOT NULL,
    status_conta VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    saldo_disponivel DECIMAL(18,2),
    saldo_atual DECIMAL(18,2),
    data_atualizacao TIMESTAMP NOT NULL,
    FOREIGN KEY (consent_id) REFERENCES aurix.consentimento(consent_id)
);

CREATE INDEX idx_conta_consentida_consent ON aurix.conta_consentida(consent_id);
CREATE INDEX idx_conta_consentida_account ON aurix.conta_consentida(account_id);

-- Tabela de transações consentidas
CREATE TABLE aurix.transacao_consentida (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    tipo_transacao VARCHAR(20) NOT NULL,
    valor DECIMAL(18,2) NOT NULL,
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    estabelecimento VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    data_transacao TIMESTAMP NOT NULL,
    data_processamento TIMESTAMP NOT NULL,
    FOREIGN KEY (consent_id) REFERENCES aurix.consentimento(consent_id)
);

CREATE INDEX idx_transacao_consentida_consent ON aurix.transacao_consentida(consent_id);
CREATE INDEX idx_transacao_consentida_account ON aurix.transacao_consentida(account_id);
