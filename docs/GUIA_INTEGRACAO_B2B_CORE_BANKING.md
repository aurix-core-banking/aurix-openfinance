# Guia de Integração B2B — Aurix Open Finance Gateway

O **Aurix Open Finance Gateway** é uma solução de software bancário regulatório (Fases 1 a 4 do Open Finance Brasil / Banco Central do Brasil) projetada para permitir que **bancos múltiplos, bancos digitais, cooperativas de crédito, SCDs (Sociedades de Crédito Direto) e fintechs** entrem em conformidade com o BACEN em semanas, e não anos.

---

## 1. Visão Geral da Arquitetura Comercial

A solução opera como um **Gateway Regulatório Intermediário (Reverse Proxy FAPI)** entre o ecossistema externo do Open Finance Brasil (Diretório Central do BACEN e instituições receptoras/transmissoras) e os sistemas internos de qualquer instituição financeira:

```
┌────────────────────────────────────────────────────────┐
│        Ecossistema Open Finance Brasil (BACEN)         │
└───────────────────────────┬────────────────────────────┘
                            │ mTLS (ICP-Brasil) + DPoP (RFC 9449)
                            ▼
┌────────────────────────────────────────────────────────┐
│           AURIX OPEN FINANCE GATEWAY (B2B)             │
│                                                        │
│  - Trust Plane (Validação FAPI, DPoP, Assinatura JWS) │
│  - Consent Plane (Ciclo de Vida de Consentimentos)    │
│  - Execution Plane (Orquestração & Reconciliação)     │
│  - Multi-Tenancy & White-Label Nativo                  │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
   [ Modo 1: Sandbox ]        [ Modo 2: REST Connector ]
   - Dados sintéticos BR      - Chamadas HTTP aos endpoints
   - Homologação imediata       do Core Banking existente
```

---

## 2. Modelos de Conector de Core Banking (`SPI`)

A plataforma utiliza o padrão **SPI (Service Provider Interface)** com três modos configuráveis via variável de ambiente `AURIX_OF_CONNECTOR_TYPE`:

### 2.1. Modo `sandbox` (Pronto para Uso Imediato)
Ideal para **demonstrações comerciais**, testes de estresse e homologação inicial de interfaces:
- Fornece massas de dados sintéticas brasileiras (contas correntes, poupanças, limites de cartões, faturas, seguros de vida e transferências Pix).
- Não requer nenhum banco legado conectado.

### 2.2. Modo `rest` (Conexão Universal com Qualquer Core)
Para bancos com sistemas próprios (*Matera, Totvs/Topaz, Sinqia, Mambu* ou legados proprietários):
- O gateway consome 5 a 7 endpoints REST disponibilizados pela TI do cliente.
- Autenticação configurável via cabeçalho HTTP (`X-API-Key` ou `Authorization: Bearer`).

### 2.3. Modo `aurix` (Core Banking Embutido)
- Modo nativo utilizando as tabelas e eventos de mensageria direta do ecossistema Aurix Core Banking.

---

## 3. Especificação dos Endpoints REST do Core Banking

Quando configurado em modo `rest`, a instituição parceira implementa os seguintes contratos na URL base informada (`AURIX_OF_CONNECTOR_REST_BASE_URL`):

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/health` | Healthcheck de conectividade do Core |
| `GET` | `/accounts?consentId={id}` | Lista contas do consentimento informado |
| `GET` | `/transactions?consentId={id}` | Extrato consolidado de transações bancárias |
| `GET` | `/cards?consentId={id}` | Cartões de crédito vinculados |
| `GET` | `/cards/bills?consentId={id}` | Faturas fechadas e abertas dos cartões |
| `GET` | `/customers?consentId={id}` | Dados cadastrais PF/PJ do titular |
| `GET` | `/investments?consentId={id}` | Posições consolidadas em renda fixa/variável |
| `GET` | `/loans?consentId={id}` | Contratos de empréstimos e financiamentos |
| `GET` | `/pix/transactions?consentId={id}` | Histórico de liquidações Pix |

---

## 4. Multi-Tenancy & White-Label

Cada parceiro pode personalizar sua marca, cores e dados regulatórios através das seguintes variáveis de ambiente:

| Variável | Descrição | Exemplo |
|---|---|---|
| `AURIX_OF_TENANT_ID` | Identificador único do banco parceiro | `coop-credito-sp` |
| `AURIX_OF_TENANT_NAME` | Razão Social oficial no BACEN | `Cooperativa de Crédito SP` |
| `AURIX_OF_TENANT_CNPJ` | CNPJ da instituição participante | `12.345.678/0001-90` |
| `AURIX_OF_PARTICIPANT_CODE`| Código de Participante ISPB | `12345678` |
| `AURIX_OF_BRAND_NAME` | Nome comercial exibido ao cliente | `CoopCred` |
| `AURIX_OF_PRIMARY_COLOR` | Cor primária para interface web | `#0052FF` |

Os frontends integrados consultam os metadados dinamicamente via `GET /open-finance/v1/tenant/info`.

---

## 5. Como Subir o Gateway Standalone

Para subir a solução completa em qualquer ambiente (local, máquina virtual ou Kubernetes):

```bash
# 1. Clonar ou copiar os artefatos de deploy
docker compose -f docker-compose.standalone.yml up -d

# 2. Testar o healthcheck
curl http://localhost:8096/actuator/health

# 3. Acessar a documentação interativa OpenAPI
# http://localhost:8096/swagger-ui.html

# 4. Consultar os dados do Tenant White-Label
curl http://localhost:8096/open-finance/v1/tenant/info
```
