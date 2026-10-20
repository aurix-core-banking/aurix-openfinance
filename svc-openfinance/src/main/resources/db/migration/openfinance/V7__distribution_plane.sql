-- ============================================================
-- Flyway: V7 — Distribution Plane
-- Tabelas para data_products, subscriptions e a materialização
-- pragmática de produtos de dado usada em dev/local (substituto de
-- ClickHouse/MinIO — ver ADR/plano de correção).
-- Sem schema explícito, seguindo o mesmo padrão já em produção de
-- V4__consent_plane_policy_context.sql (DataProduct/Subscription
-- também não declaram @Table(schema=...)).
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. TABELA data_products
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS data_products (
    id                      BIGSERIAL PRIMARY KEY,
    product_id              VARCHAR(64) NOT NULL UNIQUE,
    name                    VARCHAR(200) NOT NULL,
    description             VARCHAR(2000),
    domain                  VARCHAR(50) NOT NULL,
    resource_type           VARCHAR(50) NOT NULL,
    format                  VARCHAR(20) NOT NULL,
    schema                  VARCHAR(8000) NOT NULL,
    endpoint                VARCHAR(500) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_data_products_domain ON data_products (domain);
CREATE INDEX IF NOT EXISTS idx_data_products_status ON data_products (status);

-- ──────────────────────────────────────────────────────────────
-- 2. TABELA data_product_records
-- Materialização dos registros canônicos publicados em cada produto
-- de dado — substituto pragmático de ClickHouse/MinIO para dev/local.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS data_product_records (
    id                      BIGSERIAL PRIMARY KEY,
    product_id              VARCHAR(64) NOT NULL REFERENCES data_products(product_id) ON DELETE CASCADE,
    canonical_record_id     VARCHAR(64) NOT NULL,
    canonical_data          JSONB NOT NULL,
    materialized_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_data_product_records_product ON data_product_records (product_id);
CREATE INDEX IF NOT EXISTS idx_data_product_records_canonical ON data_product_records (canonical_record_id);

-- ──────────────────────────────────────────────────────────────
-- 3. TABELA subscriptions
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    id                      BIGSERIAL PRIMARY KEY,
    subscription_id         VARCHAR(64) NOT NULL UNIQUE,
    participant_id          VARCHAR(64) NOT NULL,
    data_product_id         VARCHAR(64) NOT NULL,
    callback_url            VARCHAR(500) NOT NULL,
    events                  VARCHAR(4000) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_participant ON subscriptions (participant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_product ON subscriptions (data_product_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions (status);
