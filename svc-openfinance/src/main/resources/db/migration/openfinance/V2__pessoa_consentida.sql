-- V2: Tabela de pessoas consentidas (dados pessoais Open Finance Fase 1)

CREATE TABLE aurix.pessoa_consentida (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    tipo_pessoa VARCHAR(10) NOT NULL DEFAULT 'FISICA',
    cpf_cnpj VARCHAR(20),
    nome_completo VARCHAR(200),
    data_nascimento DATE,
    sexo VARCHAR(10),
    nome_mae VARCHAR(200),
    email VARCHAR(100),
    telefone VARCHAR(20),
    logradouro VARCHAR(200),
    cidade VARCHAR(100),
    estado VARCHAR(5),
    cep VARCHAR(10),
    pais VARCHAR(50) DEFAULT 'BRASIL',
    data_atualizacao TIMESTAMP NOT NULL,
    FOREIGN KEY (consent_id) REFERENCES aurix.consentimento(consent_id)
);

CREATE INDEX idx_pessoa_consentida_consent ON aurix.pessoa_consentida(consent_id);
CREATE INDEX idx_pessoa_consentida_customer ON aurix.pessoa_consentida(customer_id);
