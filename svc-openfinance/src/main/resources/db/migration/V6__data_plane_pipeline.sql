-- ============================================================
-- Flyway: V6 — Data Plane Pipeline
-- Tabelas para o pipeline de dados Open Finance:
-- raw_records, canonical_records, validation_results,
-- quality_results, quality_issues, pii_classifications,
-- lineage_records.
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. TABELA raw_records
-- Registros brutos extraídos de sistemas fonte antes da canonicalização.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.raw_records (
    id                      BIGSERIAL PRIMARY KEY,
    record_id               VARCHAR(128) NOT NULL UNIQUE,
    source_system           VARCHAR(100) NOT NULL,
    extraction_id           VARCHAR(64) NOT NULL,
    resource_type           VARCHAR(20) NOT NULL,
    raw_data                JSONB NOT NULL,
    extracted_at            TIMESTAMP NOT NULL,
    schema_version          VARCHAR(20) NOT NULL,
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_raw_records_source ON aurix_openfinance.raw_records (source_system);
CREATE INDEX IF NOT EXISTS idx_raw_records_extraction ON aurix_openfinance.raw_records (extraction_id);
CREATE INDEX IF NOT EXISTS idx_raw_records_resource_type ON aurix_openfinance.raw_records (resource_type);
CREATE INDEX IF NOT EXISTS idx_raw_records_extracted_at ON aurix_openfinance.raw_records (extracted_at);

-- ──────────────────────────────────────────────────────────────
-- 2. TABELA canonical_records
-- Registros canônicos no formato padrão Open Finance Brasil.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.canonical_records (
    id                      BIGSERIAL PRIMARY KEY,
    canonical_id            VARCHAR(64) NOT NULL UNIQUE,
    raw_record_id           VARCHAR(128) NOT NULL,
    resource_type           VARCHAR(20) NOT NULL,
    canonical_data          JSONB NOT NULL,
    version                 VARCHAR(20) NOT NULL,
    canonicalized_at        TIMESTAMP NOT NULL,
    checksum                VARCHAR(128) NOT NULL,
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_canonical_records_raw ON aurix_openfinance.canonical_records (raw_record_id);
CREATE INDEX IF NOT EXISTS idx_canonical_records_resource_type ON aurix_openfinance.canonical_records (resource_type);
CREATE INDEX IF NOT EXISTS idx_canonical_records_canonicalized_at ON aurix_openfinance.canonical_records (canonicalized_at);

-- ──────────────────────────────────────────────────────────────
-- 3. TABELA validation_results
-- Resultados de validação de schema dos registros canônicos.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.validation_results (
    id                      BIGSERIAL PRIMARY KEY,
    canonical_record_id     VARCHAR(64) NOT NULL,
    schema_version          VARCHAR(20) NOT NULL,
    valid                   BOOLEAN NOT NULL DEFAULT FALSE,
    errors                  JSONB DEFAULT '[]',
    warnings                JSONB DEFAULT '[]',
    validated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_validation_results_canonical ON aurix_openfinance.validation_results (canonical_record_id);
CREATE INDEX IF NOT EXISTS idx_validation_results_valid ON aurix_openfinance.validation_results (valid);

-- ──────────────────────────────────────────────────────────────
-- 4. TABELA quality_results
-- Resultados de verificação de qualidade dos registros canônicos.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.quality_results (
    id                      BIGSERIAL PRIMARY KEY,
    canonical_record_id     VARCHAR(64) NOT NULL,
    score                   INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    passed                  BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quality_results_canonical ON aurix_openfinance.quality_results (canonical_record_id);
CREATE INDEX IF NOT EXISTS idx_quality_results_score ON aurix_openfinance.quality_results (score);
CREATE INDEX IF NOT EXISTS idx_quality_results_passed ON aurix_openfinance.quality_results (passed);

-- ──────────────────────────────────────────────────────────────
-- 5. TABELA quality_issues
-- Problemas individuais de qualidade encontrados.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.quality_issues (
    id                      BIGSERIAL PRIMARY KEY,
    quality_result_id       BIGINT NOT NULL REFERENCES aurix_openfinance.quality_results(id) ON DELETE CASCADE,
    rule_id                 VARCHAR(50) NOT NULL,
    rule_name               VARCHAR(200) NOT NULL,
    rule_type               VARCHAR(20) NOT NULL,
    severity                VARCHAR(10) NOT NULL,
    message                 VARCHAR(500) NOT NULL,
    field_path              VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_quality_issues_result ON aurix_openfinance.quality_issues (quality_result_id);
CREATE INDEX IF NOT EXISTS idx_quality_issues_severity ON aurix_openfinance.quality_issues (severity);

-- ──────────────────────────────────────────────────────────────
-- 6. TABELA pii_classifications
-- Classificações e proteções de dados PII aplicadas.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.pii_classifications (
    id                      BIGSERIAL PRIMARY KEY,
    canonical_record_id     VARCHAR(64) NOT NULL,
    sensitivity_level       VARCHAR(20) NOT NULL,
    protected_fields        JSONB DEFAULT '[]',
    masked_record           JSONB,
    classified_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pii_classifications_canonical ON aurix_openfinance.pii_classifications (canonical_record_id);
CREATE INDEX IF NOT EXISTS idx_pii_classifications_sensitivity ON aurix_openfinance.pii_classifications (sensitivity_level);

-- ──────────────────────────────────────────────────────────────
-- 7. TABELA lineage_records
-- Registros de linhagem completa: consent → resource → execution → publication.
-- INV03: Sem linhagem, sem publicação.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix_openfinance.lineage_records (
    id                      BIGSERIAL PRIMARY KEY,
    lineage_id              VARCHAR(64) NOT NULL UNIQUE,
    consent_id              VARCHAR(64) NOT NULL,
    resource_id             VARCHAR(64) NOT NULL,
    execution_plan_id       VARCHAR(64) NOT NULL,
    workflow_id             VARCHAR(64),
    pipeline_execution_id   VARCHAR(64) NOT NULL,
    source_record_id        VARCHAR(64) NOT NULL,
    canonical_record_id     VARCHAR(64) NOT NULL,
    publication_id          VARCHAR(64),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lineage_records_consent ON aurix_openfinance.lineage_records (consent_id);
CREATE INDEX IF NOT EXISTS idx_lineage_records_resource ON aurix_openfinance.lineage_records (resource_id);
CREATE INDEX IF NOT EXISTS idx_lineage_records_execution ON aurix_openfinance.lineage_records (execution_plan_id);
CREATE INDEX IF NOT EXISTS idx_lineage_records_publication ON aurix_openfinance.lineage_records (publication_id);
CREATE INDEX IF NOT EXISTS idx_lineage_records_canonical ON aurix_openfinance.lineage_records (canonical_record_id);
CREATE INDEX IF NOT EXISTS idx_lineage_records_pipeline ON aurix_openfinance.lineage_records (pipeline_execution_id);
