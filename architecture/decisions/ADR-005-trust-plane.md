# ADR-005: Trust Plane — mTLS/DPoP + Assinatura Real + Formalização do Consentimento

## Status

Accepted

## Contexto

O Trust Plane estava pendente (issue #1). O `OpenFinanceSecurityConfig` só validava JWT via `oauth2ResourceServer().jwt()`, sem mTLS e sem DPoP. A "assinatura" do `AuthorizedContext` era um **hash SHA-256 rotulado como ES256/RS256** — sem chave privada, sem verificabilidade, sem valor de não-repúdio. Isso violava a conformidade FAPI-Brasil e o INV05 do `architecture.yaml`.

Além disso, o consentimento em si não era formalizado criptograficamente: não havia artefato imutável e verificável provando *o que* foi consentido e *quando*.

## Decisão

Criar o Trust Plane em `com.aurix.platform.openfinance.trust` com cinco capacidades:

### 1. `SigningService` (criptografia assimétrica real)

Assinatura assimétrica real (EC P-256 → ES256, EC P-384 → ES384, RSA 2048 → RS256), verificável e com não-repúdio. Substitui o hash disfarçado em `AuthorizedContextService.computeSignature()`.

- Em dev/local, chaves geradas em memória.
- Em produção, a chave deve vir do Vault (extension point — a interface `SigningService` permanece a mesma).

### 2. `ChannelTrustVerifier` (mTLS) — duas portas, um serviço

- `GatewayHeaderTrustVerifier` (perfil `!prod`): confia em header repassado pelo gateway (`X-Client-Cert-Subject`), padrão de mercado para FAPI.
- `MutualTlsTrustVerifier` (perfil `prod`): valida o certificado do cliente contra a cadeia de uma CA configurada.

Profiles garantem que apenas **um** bean existe por vez (evita ambiguidade de injeção).

### 3. `ProofOfPossessionVerifier` (DPoP — RFC 9449)

- `DpopProofVerifier`: valida o proof JWT (typ `dpop+jwt`), método (`htm`), URI (`htu`) e o thumbprint `jwk` → `cnf.jkt` do access token.
- `DpopFilter` (Spring Security, antes do `oauth2ResourceServer`): enforça a prova de posse por requisição.

### 4. `TrustPlaneService`

Orquestra as duas portas (canal + DPoP) e expõe `verify(request) -> TrustResult(allowed, channelStatus, popStatus)`.

### 5. `ConsentReceipt` (formalização do consentimento)

Ao aprovar um consentimento (`ConsentimentoService.aprovar()`), gera um **recibo imutável e assinado**:
- `receiptId`, `consentId`, `contentHash` (SHA-256 do que foi consentido), `timestamp`, `signature`, `signingAlgorithm`.
- Persistido em `consent_receipts` (migração `V9`), imutável (sem setters).
- Verificação criptográfica de não-repúdio (endpoint `POST /api/v1/trust/receipts/{consentId}/verify`).

Faz do consentimento o primeiro elo verificável da cadeia de rastreabilidade do `architecture.yaml`.

## Consequências

Positivas:
- Não-repúdio real no consentimento e no contexto autorizado (INV05).
- DPoP + mTLS atendendo FAPI-Brasil (issue #1, WP2).
- Profiles dev/prod mantêm o fluxo utilizável em testes sem certificado.

Trade-offs / pendências:
- `ConsentReceiptService` reconstitui o payload de verificação truncando o timestamp a segundos para consistência com a coluna `TIMESTAMP` do banco.
- DPoP em produção termina na validade do JWT junto ao AS (Keycloak) em ambiente com chave real; a coerência proof↔token é validada criptograficamente neste serviço.
- Certificado ICP-Brasil real e Autoridade de Carimbo do Tempo (RFC 3161) ficam como extensão futura (fora do escopo dev/local, exigem credenciamento BACEN).

## Referências

- architecture/architecture.yaml — INV05, Trust Plane
- RFC 9449 (DPoP), RFC 3161 (Time-Stamp Protocol)
- issue #1 — Trust Plane (WP2)
