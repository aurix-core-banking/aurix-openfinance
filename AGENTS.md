# AGENTS.md — Aurix Open Finance Platform

## Architecture Knowledge Graph

**Antes de fazer qualquer alteração na plataforma, leia `architecture/architecture.yaml`.**

O arquivo `architecture.yaml` é a **fonte de verdade** da arquitetura. Ele contém:
- **Invariantes** (INV01–INV07): Regras que NUNCA podem ser violadas
- **Princípios** (P01–P05): Diretrizes de design
- **Planos**: Trust, Consent, Execution, Data Plane, Distribution
- **Componentes**: Cada componente com role, APIs, eventos, responsabilidades
- **Relationships**: Grafo de dependências entre componentes
- **Flows**: Fluxos detalhados de consent-to-execution, extraction, data-processing
- **Cross-cutting concerns**: Policy enforcement, idempotency, observability, etc.
- **Traceability chain**: Cadeia completa de rastreabilidade

### Regras Críticas para Agentes

1. **INV01**: "No consent, no execution" — Nunca adicione código que acesse dados sem passar pelo Consent Plane
2. **INV04**: "Temporal cannot decide authorization" — O Temporal é engine de orquestração, NÃO de autorização
3. **INV05**: "Immutable authorized context" — O Authorized Context é imutável após criação
4. **INV06**: "Consent versioning" — Alterações de consentimento criam nova versão
5. **INV07**: "Schema evolution backward-compatible" — Novos schemas devem ser compatíveis

### Estrutura de Arquivos

```
architecture/
├── architecture.yaml          # FONTE DE VERDADE
├── ARCHITECTURE.md            # Explicação humana (português)
├── diagrams/
│   ├── overview.mmd           # Visão geral Mermaid
│   ├── consent-flow.mmd       # Fluxo de consentimento
│   ├── extraction-flow.mmd    # Fluxo de extração
│   └── data-flow.mmd          # Fluxo de dados
├── decisions/
│   ├── ADR-001-temporal-orchestration.md
│   ├── ADR-002-consent-plane.md
│   ├── ADR-003-resource-discovery.md
│   └── ADR-004-execution-plan-dag.md
├── contracts/
│   ├── consent.schema.json    # Schema JSON do consentimento
│   └── execution-plan.schema.json  # Schema JSON do plano de execução
└── rules/
    └── architecture-rules.yaml  # Regras de validação de compliance
```

### Para Novas Features

1. Leia `architecture.yaml` para entender o plano afetado
2. Verifique se a feature viola algum invariante (INV01–INV07)
3. Consulte as ADRs relevantes para entender o contexto da decisão
4. Atualize `architecture.yaml` se a feature muda a arquitetura
5. Crie ADR se a feature introduz nova decisão arquitetural
6. Execute as regras de compliance em `architecture-rules.yaml`

### Para Bug Fixes

1. Identifique qual plano/componente é afetado
2. Verifique se o fix mantém os invariantes
3. Se o fix muda comportamento arquitetural, crie ADR

---

## Visão Geral da Plataforma

Aurix Open Finance é uma plataforma de dados Open Finance Brasil que implementa:
- **BACEN COMEX 35.145** — Regulamentação de dados financeiros
- **FAPI-Brazil Security Profile** — Segurança para APIs financeiras
- **Consent-driven architecture** — Consentimento como pré-requisito absoluto
- **Event-driven processing** — Pipelines duráveis via Temporal

### Stack Tecnológica

- **Backend**: Java 25, Spring Boot 4.1.0, Spring Cloud 2025.1.2
- **Build**: Maven multi-module
- **Data**: PostgreSQL 15, Kafka, ClickHouse, TimescaleDB, Redis 7, MinIO
- **Orchestration**: Temporal 1.26.0
- **Infrastructure**: Kubernetes, Helm, ArgoCD, Istio, Traefik
- **Observability**: Prometheus, Grafana, OpenTelemetry, Jaeger, ELK

### Comandos Úteis

```bash
# Build completo
./mvnw clean install -DskipTests

# Rodar serviço específico
./mvnw spring-boot:run -pl svc-openfinance

# Testes
./mvnw test -pl svc-openfinance

# Docker
docker build -f Dockerfile.svc --build-arg SVC_NAME=svc-openfinance -t svc-openfinance .

# Docker Compose (com Temporal)
docker-compose -f docker-compose-openfinance.yml up -d
```

---

## Architecture Knowledge Graph — AI Agent Instructions

A definição canônica da arquitetura está em:

`architecture/architecture.yaml`

Antes de implementar ou modificar qualquer componente arquitetural:

1. Leia `architecture/architecture.yaml`.
2. Identifique o plano, domínio e capacidade afetados.
3. Verifique os invariantes arquiteturais (INV01–INV07).
4. Consulte ADRs relevantes em `architecture/decisions/`.
5. Não introduza dependências que violem relacionamentos.
6. Atualize `architecture/architecture.yaml` ao introduzir mudanças arquiteturais.
7. Gere diagramas a partir do modelo de arquitetura; diagramas não são a fonte de verdade.

### Invariantes Críticos

- **INV01**: Sem consentimento, sem execução
- **INV02**: Sem recurso autorizado, sem acesso a dados
- **INV03**: Sem linhagem, sem publicação
- **INV04**: Temporal não decide autorização
- **INV05**: Contexto autorizado imutável
- **INV06**: Versionamento de consentimento (nova versão, nunca mutação)
- **INV07**: Evolução de schema compatível retroativamente

### Ownership de Componentes

| Plano | Componente | Serviço |
|-------|-----------|---------|
| Trust | Autenticação FAPI | OpenFinanceSecurityConfig |
| Consent | Gestão de Consentimento | ConsentimentoService |
| Consent | Descoberta de Recursos | ResourceDiscoveryService |
| Consent | Motor de Políticas | PolicyEngineService |
| Consent | Contexto Autorizado | AuthorizedContextService |
| Execution | Planejador de Extração | ExtractionPlannerService |
| Execution | Temporal | DataExtractionWorkflow |
| Execution | Reconciliação | ReconciliationService |
| Data | Canonicalização | CanonicalizationService |
| Data | Validação de Schema | SchemaValidationService |
| Data | Qualidade de Dados | DataQualityService |
| Data | Classificação PII | PiiClassificationService |
| Data | Linhagem | LineageService |
| Data | Pipeline | PipelineOrchestrator |
| Distribution | Produtos de Dado | DataProductService |
| Distribution | Assinaturas | SubscriptionService |

### Padrão Extractor

Toda extração de dados usa a interface `DataExtractor`:
- `BaseExtractor` — base abstrata com retry/circuit breaker
- `CoreAccountExtractor` — contas, saldos, transações
- `CoreCardExtractor` — cartões de crédito, faturas, transações
- `CorePixExtractor` — chaves PIX, transações PIX
- `CoreLoanExtractor` — empréstimos, parcelas
- `CoreInvestmentExtractor` — investimentos, portfolio

Extractors recebem um `AuthorizedContext` (já validado pelo Policy Engine)
e NÃO DEVEM tomar decisões de autorização (INV04).
