-- V4__consent_plane_policy_context.sql
-- Consent Plane: Policy Engine, Authorized Context e Resource Discovery

-- ============================================================
-- 1. Policy Engine
-- ============================================================

CREATE TABLE policy_rules (
    id              BIGSERIAL PRIMARY KEY,
    rule_code       VARCHAR(255) NOT NULL UNIQUE,
    rule_name       VARCHAR(255) NOT NULL,
    description     VARCHAR(2000) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    priority        INT NOT NULL DEFAULT 0,
    expression      VARCHAR(4000),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_policy_rules_active ON policy_rules(active, priority);
CREATE INDEX idx_policy_rules_type ON policy_rules(type);

COMMENT ON TABLE policy_rules IS 'Regras do motor de políticas de autorização';
COMMENT ON COLUMN policy_rules.type IS 'Tipos: CONSENT_VALIDATION, RESOURCE_ACCESS, PURPOSE_VALIDATION, TOKEN_VALIDATION, RATE_LIMITING, TIME_CONSTRAINT';
COMMENT ON COLUMN policy_rules.severity IS 'Severidade: CRITICAL, HIGH, MEDIUM, LOW';

CREATE TABLE policy_decisions (
    id                  BIGSERIAL PRIMARY KEY,
    consent_id          VARCHAR(255) NOT NULL,
    resource_id         VARCHAR(255) NOT NULL,
    permission          VARCHAR(255) NOT NULL,
    decision            VARCHAR(20) NOT NULL,
    reason              VARCHAR(2000),
    evaluated_rules     VARCHAR(4000),
    evaluation_time_ms  INT NOT NULL DEFAULT 0,
    evaluated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluated_by        VARCHAR(255) NOT NULL
);

CREATE INDEX idx_policy_decisions_consent ON policy_decisions(consent_id, evaluated_at DESC);
CREATE INDEX idx_policy_decisions_decision ON policy_decisions(decision, evaluated_at DESC);
CREATE INDEX idx_policy_decisions_evaluated_at ON policy_decisions(evaluated_at DESC);

COMMENT ON TABLE policy_decisions IS 'Trilha de auditoria de decisões de política';
COMMENT ON COLUMN policy_decisions.decision IS 'Decisão: ALLOWED, DENIED, CONDITIONAL';

-- ============================================================
-- 2. Authorized Context (INV05: Imutável)
-- ============================================================

CREATE TABLE authorized_contexts (
    id                  BIGSERIAL PRIMARY KEY,
    context_id          VARCHAR(255) NOT NULL UNIQUE,
    subject             VARCHAR(255) NOT NULL,
    consent_id          VARCHAR(255) NOT NULL,
    consent_version     INT NOT NULL DEFAULT 1,
    purpose             VARCHAR(255) NOT NULL,
    permissions         VARCHAR(4000) NOT NULL,
    resource_graph      VARCHAR(8000) NOT NULL,
    valid_until         TIMESTAMP NOT NULL,
    signing_algorithm   VARCHAR(20) NOT NULL,
    dpop_thumbprint     VARCHAR(255) NOT NULL,
    signature           VARCHAR(4000) NOT NULL,
    revoked             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at          TIMESTAMP
);

CREATE INDEX idx_authorized_contexts_consent ON authorized_contexts(consent_id, revoked);
CREATE INDEX idx_authorized_contexts_subject ON authorized_contexts(subject, revoked);
CREATE INDEX idx_authorized_contexts_active ON authorized_contexts(revoked, valid_until)
    WHERE revoked = FALSE;

COMMENT ON TABLE authorized_contexts IS 'Contextos autorizados imutáveis — não podem ser modificados após criação (INV05)';
COMMENT ON COLUMN authorized_contexts.signing_algorithm IS 'Algoritmo de assinatura: ES256, RS256';
COMMENT ON COLUMN authorized_contexts.revoked IS 'Contexto revogado — nunca deletado, apenas marcado';

-- ============================================================
-- 3. Resource Discovery
-- ============================================================

CREATE TABLE resource_graphs (
    id              BIGSERIAL PRIMARY KEY,
    graph_id        VARCHAR(255) NOT NULL UNIQUE,
    consent_id      VARCHAR(255) NOT NULL,
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_graphs_consent ON resource_graphs(consent_id);

COMMENT ON TABLE resource_graphs IS 'Grafo de recursos autorizados — DAG de dependências de extração';

CREATE TABLE resource_nodes (
    id              BIGSERIAL PRIMARY KEY,
    node_id         VARCHAR(255) NOT NULL UNIQUE,
    graph_id        VARCHAR(255) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    path            VARCHAR(500) NOT NULL,
    capabilities    VARCHAR(4000) NOT NULL,
    dependencies    VARCHAR(4000) NOT NULL DEFAULT '[]',
    metadata        VARCHAR(4000) NOT NULL DEFAULT '{}',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_nodes_graph ON resource_nodes(graph_id, active);
CREATE INDEX idx_resource_nodes_type ON resource_nodes(graph_id, resource_type, active);
CREATE INDEX idx_resource_nodes_path ON resource_nodes(graph_id, path);

COMMENT ON TABLE resource_nodes IS 'Nós do grafo de recursos — cada nó representa um tipo de recurso acessível';

CREATE TABLE resource_edges (
    id              BIGSERIAL PRIMARY KEY,
    graph_id        VARCHAR(255) NOT NULL,
    source_node_id  VARCHAR(255) NOT NULL,
    target_node_id  VARCHAR(255) NOT NULL,
    edge_type       VARCHAR(50) NOT NULL,
    metadata        VARCHAR(2000),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_edges_graph ON resource_edges(graph_id);
CREATE INDEX idx_resource_edges_source ON resource_edges(graph_id, source_node_id);
CREATE INDEX idx_resource_edges_target ON resource_edges(graph_id, target_node_id);

COMMENT ON TABLE resource_edges IS 'Arestas do grafo — representam dependências entre recursos';

-- ============================================================
-- Dados iniciais: Regras padrão do motor de políticas
-- ============================================================

INSERT INTO policy_rules (rule_code, rule_name, description, type, severity, active, priority, expression, created_at)
VALUES
    ('CONSENT_001', 'Validação de Consentimento Ativo',
     'Verifica se o consentimento está ativo e não foi revogado',
     'CONSENT_VALIDATION', 'CRITICAL', TRUE, 1,
     'consent.status == "ACTIVE"', CURRENT_TIMESTAMP),

    ('RESOURCE_001', 'Validação de Acesso ao Recurso',
     'Verifica se o recurso solicitado está no grafo autorizado',
     'RESOURCE_ACCESS', 'CRITICAL', TRUE, 2,
     'resource.path IN authorized_paths', CURRENT_TIMESTAMP),

    ('PURPOSE_001', 'Validação de Propósito',
     'Verifica se o propósito está dentro do escopo autorizado',
     'PURPOSE_VALIDATION', 'HIGH', TRUE, 3,
     'purpose IN consent.purposes', CURRENT_TIMESTAMP),

    ('TOKEN_001', 'Validação de Token DPoP',
     'Verifica se o DPoP thumbprint corresponde ao contexto',
     'TOKEN_VALIDATION', 'HIGH', TRUE, 4,
     'token.dpop_thumbprint == context.dpop_thumbprint', CURRENT_TIMESTAMP),

    ('RATE_001', 'Limitação de Taxa',
     'Controla taxa de requisições por consentimento',
     'RATE_LIMITING', 'MEDIUM', TRUE, 5,
     'rate_limit.consent <= 100/min', CURRENT_TIMESTAMP),

    ('TIME_001', 'Restrição Temporal',
     'Verifica se o contexto não expirou',
     'TIME_CONSTRAINT', 'CRITICAL', TRUE, 0,
     'NOW() < context.valid_until', CURRENT_TIMESTAMP);
