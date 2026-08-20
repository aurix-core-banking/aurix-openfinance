# ADR-004: Execution Plan como DAG Imutável

## Status

Accepted

## Contexto

Pipelines de extração Open Finance têm dependências complexas:
- Extrair transações de cartão requer primeiro o cartão
- Extrair saldos requer primeiro a conta
- Múltiplos domains podem ser extraídos em paralelo
- Cada extração tem suas próprias policies de retry e timeout

O plano de execução precisa ser:
- **DAG** (Directed Acyclic Graph) para representar dependências
- **Imutável** para auditoria e replay
- **Serializável** para persistência e distribuição
- **Versionável** para evolução de schemas

## Decisão

O Execution Plan é um **DAG imutável**. A serialização de transporte (entre o
Extraction Planner e o Temporal Workflow) é **JSON**, persistida em **PostgreSQL/JPA**
(`aurix.execution_plans`, `aurix.plan_nodes`, `aurix.plan_edges`) e validada contra
`architecture/contracts/execution-plan.schema.json` (JSON Schema Draft-07) — não
Protocol Buffers. A versão original desta ADR previa protobuf + Confluent Schema
Registry; a implementação real optou por JSON/JPA por simplicidade operacional no
ambiente dev/local (sem exigir um Schema Registry rodando) e porque o contrato já é
validado via JSON Schema. Migrar para protobuf fica como extensão futura caso o
volume/latência exijam serialização binária — não é uma divergência não avaliada.

### Estrutura do Plano

```
ExecutionPlan
├── planId (UUID)
├── consentId (referência ao consentimento)
├── consentVersion (versão do consentimento)
├── createdAt (timestamp)
├── validUntil (expiração do plano)
└── nodes[]
    ├── nodeId (identificador único)
    ├── capability (ex: "credit-card-transactions")
    ├── resource (recurso alvo)
    ├── dependencies[] (nós precedentes)
    ├── authorization (Authorized Context fragment)
    ├── retryPolicy (backoff, max retries)
    ├── timeout (duração máxima)
    ├── rateLimit (requisições por segundo)
    ├── idempotencyKey (chave de idempotência)
    └── schemaVersion (versão do schema de output)
```

### Serialização

- **Formato**: JSON, validado contra `architecture/contracts/execution-plan.schema.json`
- **Persistência**: PostgreSQL/JPA (`aurix.execution_plans`/`plan_nodes`/`plan_edges`)
- **Versionamento**: Cada mudança de schema cria nova versão do JSON Schema

### Imutabilidade

- Plano é criado pelo Extraction Planner
- Entregue ao Temporal como definizione imutável
- Temporal executa o plano sem modificá-lo
- Qualquer mudança requer novo plano (novo consentimento ou reautorização)

## Consequências

### Positivas

- **Traceability**: Cada execução pode ser rastreada até o plano que a gerou
- **Replay**: Planos podem ser reexecutados para debugging
- **Versionamento**: Schemas evoluem sem quebrar planos existentes
- **Validação**: Planos podem ser validados contra o grafo de recursos antes da execução
- **Observabilidade**: Cada nó do DAG é monitorado individualmente

### Negativas

- **Overhead**: JSON é mais verboso que protobuf (aceito em troca de não depender de
  Schema Registry no ambiente dev/local)
- **Complexidade**: DAGs complexos podem ser difíceis de visualizar
- **Armazenamento**: Planos imutáveis consomem espaço (mitigado com TTL)

### Mitigações

- TTL de 30 dias para planos expirados
- Dashboard de visualização de DAGs no Temporal UI
- Validação automática de DAG antes de submeter ao Temporal

## Referências

- architecture.yaml → planes.execution.components.execution-plan
- architecture.yaml → planes.execution.components.extraction-planner
- architecture.yaml → invariants.INV03, INV07
