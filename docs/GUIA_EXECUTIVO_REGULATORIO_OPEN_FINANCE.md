# Guia Executivo de Arquitetura e Conformidade Regulatória — Open Finance Brasil

## Aurix Core Banking Platform

---

### 1. Visão Executiva e Fundamentação Regulatória

O ecossistema **Aurix Open Finance** implementa a camada regulatória completa do Sistema Financeiro Aberto (Open Finance Brasil), conforme instituído pela **Resolução Conjunta nº 1, de 4 de maio de 2020**, do Banco Central do Brasil (BACEN) e do Conselho Monetário Nacional (CMN), e suas subsequentes Instruções Normativas e Resoluções BCB.

A plataforma foi projetada segundo os princípios de soberania do cliente sobre seus dados, governança de autorização estrita, rastreabilidade imutável de ponta a ponta e máxima resiliência na iniciação transacional.

```
       ┌────────────────────────────────────────────────────────┐
       │              Conselho Monetário Nacional (CMN)         │
       │           Banco Central do Brasil (BACEN)              │
       └───────────────────────────┬────────────────────────────┘
                                   │ Resolução Conjunta nº 1/2020
                                   ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                      AURIX OPEN FINANCE PLATFORM                         │
│                                                                          │
│  [Consent Plane] ──► [Trust Plane] ──► [Execution] ──► [Data Plane]      │
│         │                  │               │                │            │
│     Ciclo de          mTLS + DPoP      Temporal DAG      Extractors +    │
│   Consentimento      (RFC 9449)       Workflows        Canônicos LGPD    │
│         │                  │               │                │            │
│         └──────────────────┼───────────────┴────────────────┘            │
│                            ▼                                             │
│                [Distribution & Webhooks]                                 │
└──────────────────────────────────────────────────────────────────────────┘
```

---

### 2. Os Cinco Planos da Arquitetura

O sistema é estruturado em cinco planos operacionais estritamente desacoplados, garantindo isolamento de responsabilidade e compliance contínuo:

1. **Consent Plane**: Gerencia o ciclo de vida dos consentimentos (`AWAITING_AUTHORISATION`, `AUTHORISED`, `REJECTED`, `REVOKED`), validação de escopos regulatórios, versionamento e context isolation.
2. **Execution Plane**: Orquestrado pelo Temporal Workflow Engine (`DataExtractionWorkflow`, `ConsentRevocationWorkflow`, `PaymentInitiationWorkflow`), garantindo compensação distribuída, DAGs de execução resilientes e DAG determinístico.
3. **Data Plane**: Responsável pela extração granular (`BaseExtractor`), canonicalização, avaliação de regras de qualidade de dados (`DataQualityService`) e mascaramento de dados sensíveis PII (`PiiClassificationService` conforme LGPD).
4. **Distribution Plane**: Materialização de produtos de dados (`DataProduct`), publicação no barramento de eventos (Kafka) e notificação automática de Webhooks com assinatura HMAC para receptores homologados.
5. **Trust & Governance Plane**: Fiscalização em tempo real de mTLS (certificados ICP-Brasil), validação criptográfica de prova de posse DPoP (RFC 9449), verificação de compatibilidade reversa de schemas e rastreabilidade OpenLineage.

---

### 3. Matriz de Invariantes Arquiteturais Obrigatórias

Nenhuma extração ou transação de dados pode contornar a cadeia soberana de governança:

$$\text{Consent} \rightarrow \text{Discovery} \rightarrow \text{Policy} \rightarrow \text{Context} \rightarrow \text{Plan} \rightarrow \text{Pipeline} \rightarrow \text{Lineage} \rightarrow \text{Publication}$$

| Invariante | Nome Técnico | Descrição da Regra | Mecanismo de Fiscalização |
|---|---|---|---|
| **INV-001** | `NO_CONSENT_NO_EXECUTION` | Nenhuma extração ou pagamento é executado sem consentimento em estado `AUTHORISED` e dentro do prazo de validade. | Runtime (`ConsentVerificationService`) + ArchUnit |
| **INV-002** | `NO_AUTHORIZED_RESOURCE_NO_ACCESS` | Extratores só acessam sistemas de origem mediante apresentação de `AuthorizedContext` válido e assinado. | Runtime (`BaseExtractor.extract`) + ArchUnit |
| **INV-003** | `NO_LINEAGE_NO_PUBLICATION` | Nenhum dado canônico é publicado ou materializado sem emissão prévia de registro de linhagem de dados auditável. | Runtime (`PublishDataActivityImpl`) + ArchUnit |
| **INV-004** | `TEMPORAL_MUST_NOT_AUTHORIZE` | O orquestrador Temporal jamais toma decisões de negócio ou autorização regulatória (deve invocar serviços de domínio). | Teste Arquitetural Automatizado (`ArchitectureTest`) |
| **INV-005** | `IMMUTABLE_AUTHORIZED_CONTEXT` | Uma vez emitido pelo Consent Plane, o `AuthorizedContext` é imutável até a conclusão do pipeline. | Imutabilidade em Runtime (`Java Record` / DTO selado) |
| **INV-006** | `CONSENT_VERSIONING` | Qualquer alteração de escopo, permissão ou vigência exige criação de nova versão imutável do consentimento. | Entidade JPA com versionamento estrito e log de auditoria |
| **INV-007** | `SCHEMA_BACKWARD_COMPATIBLE` | Modelos canônicos não podem quebrar contratos existentes de parceiros e reguladores. | CI Automatizado (`SchemaCompatibilityTest`) |

---

### 4. Cobertura das Fases do Open Finance Brasil

A plataforma cobre integralmente o roadmap regulatório do Banco Central do Brasil:

#### Fase 1 — Dados Abertos Institucionais
- Catálogo de canais de atendimento, produtos e serviços oferecidos.
- Catálogo de instituições participantes ativas e certificadas no Diretório de Participantes.

#### Fase 2 — Compartilhamento de Dados Cadastrais e Financeiros
- Dados cadastrais de Pessoas Físicas e Jurídicas (identificação, contatos, filiação, representantes).
- Informações de Contas de Depósito à Vista, Poupança e Pagamento (saldos, limites e extratos detalhados).
- Operações de Crédito (empréstimos, financiamentos, adiantamentos e parcelas).
- Cartões de Crédito (limites, faturas fechadas, parcelamentos e transações de fatura).
- Investimentos (Renda Fixa, CDB, Renda Variável, Fundos de Investimento e Previdência Aberta).

#### Fase 3 — Iniciação de Transações de Pagamento (ITP)
- Iniciação de transferências via **Pix** pelo Iniciador de Transação de Pagamentos (ITP).
- Suporte a pagamentos imediatos e agendados com conciliação síncrona/assíncrona.
- Validação de idempotência via `Idempotency-Key` e assinatura de payload FAPI.

#### Fase 4 — Serviços Avançados e Câmbio
- Cotações e operações de Câmbio.
- Apólices de Seguros, Títulos de Capitalização e Previdência Complementar.

---

### 5. Padrões de Segurança Criptográfica & Governança

1. **FAPI 1.0 Advanced**: Perfil de segurança OpenID Foundation exigido pelo BACEN.
2. **mTLS (Mutual TLS)**: Comunicação entre instituições e participantes autenticada por certificados digitais no padrão ICP-Brasil (certificados de assinatura e criptografia).
3. **DPoP (RFC 9449 — Demonstrating Proof-of-Possession)**:
   - Todo request autenticado nas rotas transacionais exige cabeçalho HTTP `DPoP` com JWT assinado pela chave privada efêmera do cliente.
   - O filtro [`DpopFilter`](file:///C:/Users/wende/Projects/aurix-platform/aurix-openfinance/svc-openfinance/src/main/java/com/aurix/platform/openfinance/trust/filter/DpopFilter.java) valida a correspondência de método HTTP, URI e nonce temporal, repelindo ataques de replay e roubo de tokens.
4. **Criptografia em Repouso**: Dados sensíveis armazenados com algoritmo `AES-256-GCM` com vetor de inicialização randômico e chaves geridas via KMS/Vault.
5. **Classificação e Mascaramento PII (LGPD)**: O [`PiiClassificationService`](file:///C:/Users/wende/Projects/aurix-platform/aurix-openfinance/svc-openfinance/src/main/java/com/aurix/platform/openfinance/pipeline/quality/PiiClassificationService.java) garante ofuscação de CPFs (`***.456.789-**`), contas bancárias e dados protegidos antes da materialização nos Data Products.

---

### 6. Roteamento de APIs & Documentação Swagger

As rotas são expostas unificadamente através do API Gateway Traefik com prefixo `/open-finance`:

| Método | Endpoint | Descrição Funcional |
|---|---|---|
| `GET` | `/open-finance/v1/consents` | Listar consentimentos do cliente autenticado |
| `POST` | `/open-finance/v1/consents` | Criar solicitação de novo consentimento regulatório |
| `GET` | `/open-finance/v1/consents/{id}` | Consultar detalhes e status de um consentimento |
| `POST` | `/open-finance/v1/consents/{id}/authorise` | Autorizar consentimento pelo titular da conta |
| `DELETE` | `/open-finance/v1/consents/{id}` | Revogar consentimento com justificativa formal |
| `POST` | `/open-finance/v1/pix/payments` | Iniciar pagamento Pix via Open Finance (Fase 3) |
| `GET` | `/open-finance/v1/pix/payments/{id}` | Consultar status de liquidação do Pix iniciado |
| `GET` | `/open-finance/v1/products/accounts` | Consultar contas bancárias externas agregadas |
| `GET` | `/open-finance/v1/products/credit-cards` | Consultar cartões de crédito externos agregados |
| `GET` | `/open-finance/v1/products/investments` | Consultar carteira de investimentos consolidada |
| `GET` | `/open-finance/v1/participants` | Catálogo de instituições autorizadas pelo BACEN |
| `GET` | `/swagger-ui/index.html` | Interface Interativa Swagger UI |
| `GET` | `/v3/api-docs` | Especificação OpenAPI 3.0 em JSON |

---

### 7. Checklist para Homologação junto ao Diretório do BACEN

- [x] Conformidade total de contratos com schemas JSON oficiais do Open Finance Brasil.
- [x] Testes de Arquitetura ArchUnit automatizados cobrindo todas as invariantes (`INV-001` a `INV-007`).
- [x] Testes de Contrato Pact para garantia de retrocompatibilidade de payload.
- [x] Suporte obrigatório a DPoP (RFC 9449) e autorização com token vinculada à chave criptográfica.
- [x] Resiliência de orquestração via Temporal para tolerância a falhas e recuperação automática de workflows.
- [x] Rastreabilidade e auditoria via OpenLineage e métricas exportadas via OpenTelemetry / Actuator.
