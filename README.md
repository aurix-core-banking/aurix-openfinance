# Aurix Open Finance

Serviço de Open Finance Brasil (BACEN) da plataforma Aurix. Implementa o fluxo completo
consentimento → policy engine → extração agnóstica de core → canonicalização/qualidade/PII/linhagem →
publicação (Kafka + Data Products + webhooks assinados), orquestrado via Temporal.

Arquitetura completa, ADRs e catálogo de eventos em [`architecture/`](architecture/ARCHITECTURE.md).

## Stack

- Java 25 / Spring Boot 4.1.0
- PostgreSQL + Flyway (8 migrations)
- Temporal 1.26.0 (orquestração durável do pipeline de extração)
- Apache Kafka (event bus + publicação de dados)
- Spring Security + OAuth2/JWT (resource server contra Keycloak)
- Springdoc OpenAPI 3.0
- ArchUnit (regras arquiteturais automatizadas em CI)

## Porta

**8096**

## Arquitetura em 5 planos

| Plano | O que faz | Pacotes principais |
|---|---|---|
| Consent Plane | Consentimento, descoberta de recursos, policy engine (avaliação real, não simulada) | `service` (Consentimento), `discovery`, `policy` |
| Execution Plane | Planner gera DAG, Temporal executa, reconciliação | `planner`, `temporal`, `reconciliation` |
| Pipelines por domínio | Extractors **agnósticos de core** via portas (`*SourceAdapter`) | `extractor`, `extractor/adapter` |
| Data Plane | Canonicalização, validação de schema, qualidade, classificação PII (AES/GCM real), linhagem persistida | `pipeline/*` |
| Distribution Plane | Event bus (Kafka), Data Products, assinaturas com webhook assinado (HMAC-SHA256) | `event`, `distribution` |

### Extractors agnósticos de core

`CoreAccountExtractor` (e os demais) não dependem de nenhuma tabela ou repositório específico do
core "aurix" — dependem só de uma porta (`AccountSourceAdapter`, `CardSourceAdapter`, etc., em
`extractor/adapter/`). A implementação atual (`extractor/adapter/aurixcore/`) é **um** plugin
concreto; plugar outro core é implementar a mesma porta e registrar o bean, sem tocar no extractor.

## Build & Test

```bash
# Pré-requisito: aurix-shared precisa estar instalado no repositório Maven local
cd ../aurix-backend && ./mvnw -pl aurix-shared -am install -DskipTests

# Build + testes (45 testes, incluindo um end-to-end do workflow Temporal via
# TestWorkflowEnvironment, sem precisar de Temporal server real)
./mvnw clean test

# Run local (perfil dev)
./mvnw spring-boot:run -pl svc-openfinance
```

## Rodar a stack completa (Postgres + Kafka + Temporal + serviço)

```bash
docker compose -f docker-compose-openfinance.yml up
```

Sobe Postgres, Kafka/Zookeeper, Temporal + Temporal UI (`localhost:8098`) e o serviço em `8096`.

## Endpoints principais

### Consentimento e dados (API Open Finance Brasil)

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/open-finance/v1/consents` | Criar consentimento |
| `GET` | `/open-finance/v1/consents/{id}` | Buscar consentimento |
| `POST` | `/open-finance/v1/consents/{id}/authorise` | Aprovar consentimento (dispara `consent.granted.v1`) |
| `POST` | `/open-finance/v1/consents/{id}/reject` | Rejeitar consentimento |
| `POST` | `/open-finance/v1/consents/{id}/revoke` | Revogar consentimento (dispara `consent.revoked.v1`) |
| `GET` | `/open-finance/v1/accounts` | Listar contas autorizadas |
| `GET` | `/open-finance/v1/accounts/{id}/balances` | Saldo da conta |
| `GET` | `/open-finance/v1/accounts/{id}/transactions` | Transações da conta |
| `GET` | `/open-finance/v1/credit-cards` | Cartões (Fase 2) |

Spec completa em [`openapi/aurix-openfinance.yaml`](openapi/aurix-openfinance.yaml) e Swagger UI em
`/swagger-ui.html`.

### Administração (Distribution Plane)

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/subscriptions` | Assinar um Data Product (retorna `webhookSecret` — só aparece aqui) |
| `POST` | `/api/v1/subscriptions/{id}/rotate-secret` | Rotaciona o segredo de assinatura, invalidando o anterior |
| `DELETE` | `/api/v1/subscriptions/{id}` | Cancela assinatura |
| `POST` | `/api/v1/products` | Cria Data Product |
| `GET` | `/api/v1/policy/decisions?consentId=` | Trilha de auditoria do Policy Engine |
| `POST` | `/api/v1/resources/discover` | Descobre grafo de recursos autorizados |

## Verificando webhooks recebidos

Cada notificação enviada por uma assinatura carrega dois headers:

- `X-Webhook-Timestamp` — epoch seconds do envio
- `X-Webhook-Signature` — `HMAC-SHA256(webhookSecret, "{timestamp}.{corpo}")` em hex

O receptor deve recalcular o HMAC com o `webhookSecret` recebido na criação da assinatura e comparar
com o header (idealmente em tempo constante), além de rejeitar timestamps fora de uma janela de
tolerância (ex.: 5 minutos) para se proteger contra replay.

## Event Bus (Kafka)

Producers ativos hoje (catálogo completo em [`architecture/events.yaml`](architecture/events.yaml)):

- `consent.granted.v1` / `consent.revoked.v1` / `consent.expired.v1`
- `reconciliation.triggered.v1` / `reconciliation.divergence-detected.v1` / `reconciliation.repaired.v1`
- `data.published.v1`

## Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `SERVER_PORT` | `8096` | Porta HTTP |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/aurix_db` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `aurix_user` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `aurix_dev_password` | Senha do banco |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka |
| `TEMPORAL_ADDRESS` | `localhost:7233` | Endereço do Temporal server |
| `TEMPORAL_NAMESPACE` | `aurix` | Namespace do Temporal |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8443/realms/aurix` | Issuer do resource server OAuth2/JWT |
| `AURIX_PII_ENCRYPTION_KEY_BASE64` | chave dev embutida | Chave AES-256 (base64) para criptografia real de campos PII — **trocar em produção** |
| `AURIX_VAULT_ENABLED` | `false` | Habilita Spring Cloud Vault para secrets |
| `AURIX_BACEN_URL` | `http://localhost:8095` | URL do mock BACEN |
| `AURIX_OF_CONSENT_MAX_DAYS` | `365` | Máximo de dias para consentimento |
| `AURIX_OF_RATE_LIMIT` | `10` | Rate limit por segundo |

## Status / Roadmap

Build funcional de ponta a ponta para ambiente dev/local, 45 testes automatizados (incluindo um
workflow Temporal completo via `TestWorkflowEnvironment` e verificação de assinatura HMAC de webhook).
Pendente: mTLS/DPoP de produção (FAPI-Brasil completo) e certificado ICP-Brasil real — desenho em
[issue #1](https://github.com/aurix-core-banking/aurix-openfinance/issues/1).
