# ADR-001: Temporal como Engine de Orquestração

## Status

Accepted

## Contexto

A plataforma Open Finance precisa executar pipelines de dados complexos com:
- Extração de múltiplos sistemas legados
- Dependências entre etapas (DAG)
- Retry automático com backoff exponencial
- Timeout configurável por atividade
- Paralelismo de extrações independentes
- Recuperação de falhas sem perda de estado
- Observabilidade completa de execuções

Sistemas legados são imprevisíveis: timeouts, erros intermitentes, inconsistências temporárias. A orquestração precisa ser **durável** — sobreviver a restarts, deployments e falhas de infraestrutura.

## Decisão

Utilizar **Temporal 1.26.0** como engine de execução durável.

O Temporal orquestra workflows que representam pipelines completos de extração. Cada etapa do pipeline é uma **Activity** do Temporal, com retry policy, timeout e idempotency key próprios.

### Workflows

- `DataExtractionWorkflow`: Pipeline completo de extração de dados
- `ConsentMonitoringWorkflow`: Monitoramento de consentimentos ativos
- `ReconciliationWorkflow`: Reconciliação esperado vs extraído

### Activities

- `ExtractDataActivity`: Executa extractor para um recurso específico
- `TransformDataActivity`: Canonicalização e validação
- `PublishDataActivity`: Publicação para consumidores
- `ValidateConsentActivity`: Verifica se consentimento continua ativo
- `ReconcileActivity`: Verifica consistência dos dados

### Restrição Crítica

O Temporal **NÃO** pode:
- Tomar decisões de autorização
- Interpretar regras regulatórias
- Acessar dados diretamente
- Modificar o Authorized Context

Essas restrições são documentadas em `INV04` e refletidas no `architecture.yaml` via `forbidden_responsibilities`.

## Consequências

### Positivas

- **Retry automático**: Falhas transitórias em sistemas legados são tratadas com backoff exponencial
- **Timeout**: Extrações que travam são canceladas automaticamente
- **Paralelismo**: Extrações independentes rodam concorrentemente
- **Recuperação**: Restart do engine não perde estado de execução
- **Versionamento**: Workflows podem ser migrados para novas versões
- **Observabilidade**: Temporal UI mostra estado de cada execução

### Negativas

- **Complexidade operacional**: Temporal requer cluster own (server + matching + history)
- **Curva de aprendizado**: Equipe precisa aprender SDK do Temporal
- **Dependência**: Vendor lock-in moderado (abandono seria trabalhoso mas possível)

### Mitigações

- Temporal é open source (MIT license)
- SDKs disponíveis para Java, Go, Python, TypeScript
- Documentação interna do time cobre padrões de uso

## Referências

- architecture.yaml → planes.execution.components.temporal
- architecture.yaml → invariants.INV04
- Temporal Documentation: https://docs.temporal.io
