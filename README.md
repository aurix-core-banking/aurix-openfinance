# Aurix Open Finance

Microserviço de Open Finance para a plataforma Aurix. Implementa as APIs do BACEN Fase 1 (contas, transações, consentimento).

## Stack

- Java 25 / Spring Boot 4.1.0
- PostgreSQL + Flyway
- Spring Security + OAuth2 (FAPI-Brazil)
- Springdoc OpenAPI 3.0
- Apache Kafka

## Porta

**8096**

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/open-finance/v1/consents` | Criar consentimento |
| `GET` | `/open-finance/v1/consents/{id}` | Buscar consentimento |
| `POST` | `/open-finance/v1/consents/{id}/authorise` | Aprovar consentimento |
| `POST` | `/open-finance/v1/consents/{id}/reject` | Rejeitar consentimento |
| `POST` | `/open-finance/v1/consents/{id}/revoke` | Revogar consentimento |
| `GET` | `/open-finance/v1/accounts` | Listar contas autorizadas |
| `GET` | `/open-finance/v1/accounts/{id}` | Buscar conta |
| `GET` | `/open-finance/v1/accounts/{id}/balances` | Saldo da conta |
| `GET` | `/open-finance/v1/accounts/{id}/transactions` | Transações da conta |
| `GET` | `/open-finance/v1/credit-cards` | Cartões (Fase 2) |

## OpenAPI

Spec disponível em `/openapi/aurix-openfinance.yaml` e via Swagger UI em `/swagger-ui.html`.

## Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run -pl svc-openfinance

# Run com profile dev
./mvnw spring-boot:run -pl svc-openfinance -Dspring-boot.run.profiles=dev
```

## Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/aurix_db` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `aurix_user` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `aurix_dev_password` | Senha do banco |
| `AURIX_BACEN_URL` | `http://localhost:8095` | URL do mock BACEN |
| `AURIX_OF_CONSENT_MAX_DAYS` | `365` | Máximo de dias para consentimento |
| `AURIX_OF_RATE_LIMIT` | `10` | Rate limit por segundo |
