-- ============================================================
-- Flyway: V5 — Execution Plane
-- Tabelas para planos de extração DAG, nodes, arestas,
-- registros de reconciliação e divergências.
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. TABELA execution_plans
-- Plano de execução imutável — INV05 se aplica.
-- Cada plano representa o DAG completo de extração para um consentimento.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix.execution_plans (
    id                      BIGSERIAL PRIMARY KEY,
    plan_id                 VARCHAR(64) NOT NULL UNIQUE,
    consent_id              VARCHAR(128) NOT NULL,
    consent_version         INTEGER NOT NULL DEFAULT 1,
    dag_definition          TEXT NOT NULL,
    metadata                TEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'CRIADO',
    participante_id         VARCHAR(128) NOT NULL,
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    data_execucao           TIMESTAMP,
    data_conclusao          TIMESTAMP,
    versao                  INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_execution_plan_id ON aurix.execution_plans (plan_id);
CREATE INDEX IF NOT EXISTS idx_execution_plan_consent ON aurix.execution_plans (consent_id);
CREATE INDEX IF NOT EXISTS idx_execution_plan_status ON aurix.execution_plans (status);
CREATE INDEX IF NOT EXISTS idx_execution_plan_participant ON aurix.execution_plans (participante_id);
CREATE INDEX IF NOT EXISTS idx_execution_plan_created ON aurix.execution_plans (data_criacao);

-- ──────────────────────────────────────────────────────────────
-- 2. TABELA plan_nodes
-- Nodes individuais do DAG de execução.
-- Cada node representa uma capacidade de extração (contas, transações, etc).
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix.plan_nodes (
    id                      BIGSERIAL PRIMARY KEY,
    plan_id                 VARCHAR(64) NOT NULL,
    node_id                 VARCHAR(128) NOT NULL,
    capacidade              VARCHAR(64) NOT NULL,
    recurso                 VARCHAR(64) NOT NULL,
    autorizacao_json        TEXT,
    politica_tentativa_json TEXT,
    timeout_segundos        INTEGER NOT NULL DEFAULT 30,
    limite_taxa_json        TEXT,
    chave_idempotencia      VARCHAR(128) NOT NULL,
    versao_schema           VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    versao                  INTEGER NOT NULL DEFAULT 1,
    UNIQUE(plan_id, node_id)
);

CREATE INDEX IF NOT EXISTS idx_plan_node_plan ON aurix.plan_nodes (plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_node_resource ON aurix.plan_nodes (recurso);
CREATE INDEX IF NOT EXISTS idx_plan_node_idempotency ON aurix.plan_nodes (chave_idempotencia);

-- ──────────────────────────────────────────────────────────────
-- 3. TABELA plan_edges
-- Arestas do DAG representando dependências entre nodes.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix.plan_edges (
    id                      BIGSERIAL PRIMARY KEY,
    plan_id                 VARCHAR(64) NOT NULL,
    origem                  VARCHAR(128) NOT NULL,
    destino                 VARCHAR(128) NOT NULL,
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(plan_id, origem, destino)
);

CREATE INDEX IF NOT EXISTS idx_plan_edge_plan ON aurix.plan_edges (plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_edge_origem ON aurix.plan_edges (origem);
CREATE INDEX IF NOT EXISTS idx_plan_edge_destino ON aurix.plan_edges (destino);

-- ──────────────────────────────────────────────────────────────
-- 4. TABELA reconciliation_records
-- Registros de reconciliação entre dados esperados e extraídos.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix.reconciliation_records (
    id                      BIGSERIAL PRIMARY KEY,
    reconciliation_id       VARCHAR(64) NOT NULL UNIQUE,
    plan_id                 VARCHAR(64) NOT NULL,
    node_id                 VARCHAR(128) NOT NULL,
    expected_count          INTEGER NOT NULL DEFAULT 0,
    actual_count            INTEGER NOT NULL DEFAULT 0,
    divergences_json        TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    data_reparo             TIMESTAMP,
    data_criacao            TIMESTAMP NOT NULL DEFAULT NOW(),
    versao                  INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_plan ON aurix.reconciliation_records (plan_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_node ON aurix.reconciliation_records (node_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_status ON aurix.reconciliation_records (status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_id ON aurix.reconciliation_records (reconciliation_id);

-- ──────────────────────────────────────────────────────────────
-- 5. TABELA reconciliation_divergences
-- Detalhes das divergências encontradas na reconciliação.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aurix.reconciliation_divergences (
    id                      BIGSERIAL PRIMARY KEY,
    reconciliation_id       VARCHAR(64) NOT NULL,
    plan_id                 VARCHAR(64) NOT NULL,
    node_id                 VARCHAR(128) NOT NULL,
    tipo_divergencia        VARCHAR(50) NOT NULL,
    quantidade              INTEGER NOT NULL DEFAULT 0,
    detalhes                TEXT,
    reparado                BOOLEAN NOT NULL DEFAULT FALSE,
    data_deteccao           TIMESTAMP NOT NULL DEFAULT NOW(),
    data_reparo             TIMESTAMP,
    versao                  INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_divergence_reconciliation ON aurix.reconciliation_divergences (reconciliation_id);
CREATE INDEX IF NOT EXISTS idx_divergence_plan ON aurix.reconciliation_divergences (plan_id);
CREATE INDEX IF NOT EXISTS idx_divergence_node ON aurix.reconciliation_divergences (node_id);
CREATE INDEX IF NOT EXISTS idx_divergence_tipo ON aurix.reconciliation_divergences (tipo_divergencia);
CREATE INDEX IF NOT EXISTS idx_divergence_reparado ON aurix.reconciliation_divergences (reparado);

-- ──────────────────────────────────────────────────────────────
-- Constraints de integridade referencial
-- ──────────────────────────────────────────────────────────────
ALTER TABLE aurix.plan_nodes
    ADD CONSTRAINT fk_plan_node_plan FOREIGN KEY (plan_id)
    REFERENCES aurix.execution_plans(plan_id) ON DELETE CASCADE;

ALTER TABLE aurix.plan_edges
    ADD CONSTRAINT fk_plan_edge_plan FOREIGN KEY (plan_id)
    REFERENCES aurix.execution_plans(plan_id) ON DELETE CASCADE;

ALTER TABLE aurix.reconciliation_records
    ADD CONSTRAINT fk_reconciliation_plan FOREIGN KEY (plan_id)
    REFERENCES aurix.execution_plans(plan_id) ON DELETE CASCADE;

ALTER TABLE aurix.reconciliation_divergences
    ADD CONSTRAINT fk_divergence_reconciliation FOREIGN KEY (reconciliation_id)
    REFERENCES aurix.reconciliation_records(reconciliation_id) ON DELETE CASCADE;

ALTER TABLE aurix.reconciliation_divergences
    ADD CONSTRAINT fk_divergence_plan FOREIGN KEY (plan_id)
    REFERENCES aurix.execution_plans(plan_id) ON DELETE CASCADE;
