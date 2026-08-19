# Arquitetura — Open Finance Data Exchange Platform

## Visão Geral

Esta plataforma implementa a infraestrutura de dados Open Finance Brasil, seguindo a regulamentação **BACEN COMEX 35.145** e o **FAPI-Brazil Security Profile**. A arquitetura é organizada em **5 planos**, cada um com responsabilidades claras e separação rígida de concerns.

---

## Os 5 Planos

### 1. Trust Plane (Plano de Confiança)
Responsável por autenticação, transporte seguro e tokens. Utiliza **FAPI-Brazil 2.0** com **OAuth 2.1**, **mTLS**, e **DPoP** (sender-constrained tokens). O certificado é emitido por **ICP-Brasil**. Tokens JWT têm lifetime de 300 segundos.

### 2. Consent Plane (Plano de Consentimento)
Core da regulamentação. Gerencia o lifecycle do consentimento: criação, autorização, renovação, revogação e expiração. Três componentes principais:
- **Consent Management**: System of record para consentimentos. Estados: PENDING → AUTHORIZED → ACTIVE → REVOKED/EXPIRED.
- **Resource Discovery**: Mapeia permissões do consentimento para recursos de dados acessíveis, gerando um grafo.
- **Policy Engine**: Avalia se um acesso é autorizado. Gera o **Authorized Context** — registro imutável e criptograficamente assinado que viaja durante toda a execução.

### 3. Execution Plane (Plano de Execução)
Orquestração durável via **Temporal 1.26.0**. O planner gera um **Execution Plan** (DAG) que o Temporal executa com retry, timeout, paralelismo e recuperação. Reconciliação verifica consistência após execução.

**Invariant crítica**: O Temporal **NÃO** toma decisões de autorização. Ele apenas executa o plano. A autorização é responsabilidade do Consent Plane.

### 4. Data Plane (Plano de Dados)
Pipeline de 7 estágios: Extração → Canonicalização → Validação de Schema → Qualidade de Dados → Classificação PII → Linhagem → Publicação. Cada estágio é idempotente e auditável.

### 5. Distribution Plane (Plano de Distribuição)
Canais de entrega: Kafka (event streaming), REST API (FAPI-Brazil auth), Data Products (ClickHouse + MinIO), GraphQL (consultas ad-hoc).

---

## Por que Consent-Driven?

A regulamentação **BACEN COMEX 35.145** exige que todo acesso a dados financeiros tenha consentimento explícito do titular. Isso não é opcional — é o invariant mais fundamental da plataforma:

> **INV01**: "No consent, no execution" — Nenhum pipeline pode executar sem consentimento válido e ativo.

Isso significa que a plataforma é construída de dentro para fora: o consentimento primeiro, os dados depois.

---

## Por que Temporal?

O **Temporal** é usado como engine de execução durável, **não** como motor de autorização. Escolhemos Temporal porque:

- **Retry automático** com políticas configuráveis
- **Timeout** em nível de atividade e workflow
- **Paralelismo** natural via activities concorrentes
- **Recuperação** de falhas sem perda de estado
- **Versionamento** de workflows para migrações
- **Signal handling** para cancelamento e atualizações

O que o Temporal **NÃO** faz:
- Decidir se um acesso é autorizado
- Interpretar regras regulatórias
- Acessar dados diretamente

---

## Por que Authorized Context Imutável?

O Authorized Context é o contrato entre o Consent Plane e o Execution Plane. É um registro imutável que contém:
- Subject (quem pediu)
- ConsentId e ConsentVersion (qual consentimento)
- Purpose (para qual finalidade)
- Permissions (o que foi autorizado)
- Resources (quais recursos)
- ValidUntil (até quando)
- SigningAlgorithm + DPopThumbprint (integridade)

**Imutável** porque:
1. Qualquer mudança durante a execução compromete a auditoria
2. Permite replay idempotente
3. Garante que o contexto que o Temporal recebe é idêntico ao que foi aprovado

---

## Estágios do Pipeline de Dados

| # | Estágio | Input | Output | Descrição |
|---|---------|-------|--------|-----------|
| 1 | Extração | Source System | Raw Record | Acessa sistemas legados via adapters |
| 2 | Canonicalization | Raw Record | Canonical Record | Padroniza para modelo Open Finance |
| 3 | Validação de Schema | Canonical Record | Validated Record | Contra schema Avro/JSON registrado |
| 4 | Qualidade de Dados | Validated Record | Quality-Checked Record | Completude, consistência, formato |
| 5 | Classificação PII | Quality-Checked Record | PII-Classified Record | Identificação e proteção de dados pessoais |
| 6 | Linhagem | PII-Classified Record | Lineage-Registered Record | Registro OpenLineage completo |
| 7 | Publicação | Lineage-Registered Record | Published Data | Entrega via Kafka, API, ou Data Products |

---

## Cadeia de Rastreabilidade

Cada dado publicado pode ser rastreado de volta ao consentimento:

```
consentId → resourceId → executionPlanId → workflowId → activityId
→ pipelineExecutionId → sourceRecordId → canonicalRecordId → publicationId
```

A cadeia é imutável e auditável. Isso permite:
- **Compliance**: Demonstração de que cada dado foi autorizado
- **Debugging**: Rastreamento de origem de dados incorretos
- **Auditoria**: Evidência para inspeções regulatórias

---

## Invariantes e Por Que Existem

| ID | Regra | Por quê |
|----|-------|---------|
| INV01 | No consent, no execution | BACEN COMEX 35.145 — consentimento é pré-requisito absoluto |
| INV02 | No authorized resource, no data access | Menor privilégio — cada recurso requer autorização explícita |
| INV03 | No lineage, no publication | Rastreabilidade obrigatória para compliance |
| INV04 | Temporal cannot decide authorization | Separação de concerns — execução ≠ autorização |
| INV05 | Immutable authorized context | Auditoria e replay seguro |
| INV06 | Consent versioning | Auditoria — nunca muta consentimento anterior |
| INV07 | Schema evolution backward-compatible | Evolução sem quebra de consumidores existentes |

---

## Como Ler architecture.yaml

O arquivo `architecture.yaml` é a **fonte de verdade** da arquitetura. Estrutura:

1. **`architecture`**: Metadados gerais, invariantes, princípios
2. **`planes`**: Os 5 planos com seus componentes, responsabilidades e APIs
3. **`domains`**: Domínios de negócio (accounts, credit-cards, loans, investments, credit)
4. **`pipeline_pattern`**: Hierarquia domain → capability → pipeline → extractor → adapter
5. **`events`**: Eventos do sistema (consent, data, system)
6. **`relationships`**: Grafo de dependências entre componentes
7. **`cross_cutting`**: Concerns transversais (policy, idempotency, observability, etc.)
8. **`traceability`**: Cadeia de rastreabilidade
9. **`flows`**: Fluxos detalhados (consent-to-execution, extraction, data-processing)
10. **`metrics`**: Métricas SRE e targets
11. **`technology`**: Stack tecnológica completa

---

## Como ADRs Relacionam ao Modelo de Arquitetura

Cada ADR (Architecture Decision Record) documenta uma decisão que afeta o modelo:

- **ADR-001 (Temporal)**: Define o execution plane. A restrição "cannot make authorization decisions" é diretamente refletida em INV04.
- **ADR-002 (Consent Plane)**: Define o consent plane. A imutabilidade e versionamento são INV05 e INV06.
- **ADR-003 (Resource Discovery)**: Conecta consent → resources → policy. A separação é refletida na relationship `provides-authorized-resources`.
- **ADR-004 (Execution Plan DAG)**: Define a estrutura do execution plan. Imutabilidade e serialização protobuf são detalhes técnicos do plano.

As ADRs são o "por quê" por trás das decisões arquiteturais. O `architecture.yaml` é o "o quê". Juntos, formam o conhecimento completo da plataforma.
