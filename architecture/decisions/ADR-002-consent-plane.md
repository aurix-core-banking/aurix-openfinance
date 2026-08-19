# ADR-002: Consent Plane como Camada Independente

## Status

Accepted

## Contexto

A regulamentação **BACEN COMEX 35.145** exige consentimento explícito do titular para cada acesso a dados financeiros. Isso não é apenas uma feature — é um requisito arquitetural que afeta toda a plataforma.

O consentimento precisa ser:
- **Registrado**: System of record com lifecycle completo
- **Versão**: Alterações criam nova versão, nunca mutam a anterior
- **Imutável após autorização**: O contexto autorizado não pode ser alterado
- **Auditável**: Histórico completo de mudanças
- **Revogável**: Titular pode revogar a qualquer momento, parando todos os pipelines

## Decisão

Criar um **Consent Plane** independente com três componentes:

### 1. Consent Management (System of Record)

- Registra consentimentos com estados: PENDING → AUTHORIZED → ACTIVE → REVOKED/EXPIRED
- Emite eventos: ConsentGranted, ConsentUpdated, ConsentRenewed, ConsentRevoked, ConsentExpired
- API REST para CRUD de consentimentos
- Versionamento: cada alteração cria nova versão (INV06)

### 2. Resource Discovery (Grafo de Recursos)

- Recebe consentimento + permissões
- Gera grafo de recursos acessíveis com caminhos e dependências
- Decouple semântica de consentimento da mecânica de extração
- API: POST /resources/discover, GET /resources/graph/{consentId}

### 3. Policy Engine (Decisão de Autorização)

- Avalia se um acesso é autorizado dado: consentimento, recurso, permissão, propósito, contexto
- Regras: consent-must-be-active, permission-must-cover-resource, purpose-must-be-valid, token-must-be-valid, dpop-must-match
- Gera **Authorized Context** — registro imutável e assinado criptograficamente

### Authorized Context

O Authorized Context é o contrato entre Consent Plane e Execution Plane:
- Imutável após criação (INV05)
- Contém: subject, consentId, consentVersion, purpose, permissions, resources, validUntil, signingAlgorithm, dpopThumbprint
- Criptograficamente assinado para garantir integridade
- Inclui grafo completo de recursos autorizados

## Consequências

### Positivas

- **Compliance garantido**: Todo acesso a dados passa pelo Consent Plane
- **Separation of concerns**: Consent ≠ Authorization ≠ Execution
- **Revogação imediata**: Revogar consentimento para todos os pipelines automaticamente
- **Auditoria completa**: Histórico imutável de consentimentos e decisões
- **Replay seguro**: Contexto imutável permite reexecuções idempotentes

### Negativas

- **Latência adicional**: Uma chamada ao Policy Engine antes de cada extração
- **Complexidade**: Três componentes para gerenciar (management, discovery, policy)
- **Armazenamento**: Consentimentos e contexts precisam de persistência durável

### Mitigações

- Cache de authorized contexts com TTL curto
- Policy Engine em memória com persistência assíncrona
- Consent Management usa PostgreSQL com audit log

## Referências

- architecture.yaml → planes.consent
- architecture.yaml → invariants.INV01, INV02, INV05, INV06
- BACEN COMEX 35.145
- FAPI-Brazil Security Profile
