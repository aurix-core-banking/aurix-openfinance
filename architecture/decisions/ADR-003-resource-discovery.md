# ADR-003: Resource Discovery como Componente Independente

## Status

Accepted

## Contexto

Consentimentos no Open Finance são declarativos: o titular autoriza "acesso a transações de conta corrente dos últimos 12 meses". Mas a execução precisa saber:
- Quais sistemas de dados contêm essas transações
- Quais tabelas/collections/armazenamentos acessar
- Quais dependências existem entre recursos (ex: precisa da conta antes das transações)
- Quais extractors são necessários para cada recurso

Mapear permissões de consentimento para recursos de dados é um problema de **resolução de grafo** — não de autorização nem de execução.

## Decisão

Criar **Resource Discovery** como componente independente no Consent Plane:

### Input
- Consentimento (com permissões e propósito)
- Catálogo de recursos disponíveis na plataforma

### Output
- **Authorized Resource Graph**: Grafo de recursos acessíveis com:
  - Caminhos de acesso (sistema → tabela → campo)
  - Dependências entre recursos
  - Extractors necessários
  - Adapters requeridos
  - Metadados de rate limit e timeout

### Separação de Responsabilidades

| Componente | Responsabilidade | NÃO faz |
|------------|-----------------|---------|
| Consent Management | Registrar consentimento | Mapear para recursos |
| Resource Discovery | Mapear permissões → recursos | Decidir se acesso é autorizado |
| Policy Engine | Decidir autorização | Mapear permissões → recursos |
| Extraction Planner | Gerar plano DAG | Mapear permissões → recursos |

## Consequências

### Positivas

- **Decoupling**: Consent semantics ≠ extraction mechanics
- **Reusabilidade**: Resource Discovery pode servir múltiplos consumers
- **Testabilidade**: Componente isolado com input/output claros
- **Evolução**: Novos domínios de dados adicionados sem mudar consent management

### Negativas

- **Manutenção do catálogo**: Resource Discovery depende de catálogo atualizado
- **Complexidade do grafo**: Grafos complexos podem ser difíceis de debugar
- **Acoplamento temporal**: Se o catálogo muda, grafos existentes podem ficar obsoletos

### Mitigações

- Catálogo de recursos versionado e imutável
- Grafos são gerados no momento da autorização (não cacheados)
- Validação de grafos contra catálogo na geração

## Referências

- architecture.yaml → planes.consent.components.resource-discovery
- architecture.yaml → relationships[consent-management → resource-discovery]
