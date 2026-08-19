# AGENTS.md — Implementation Agent Contract

## Source of Truth

Before modifying the system, read:

1. `architecture/architecture.yaml` — architecture spec (planes, domains, invariants)
2. `architecture/services.yaml` — service ownership, interfaces, relationships
3. `architecture/flows.yaml` — executable workflows and DAGs
4. `architecture/policies.yaml` — invariants, rules, compliance
5. `architecture/events.yaml` — event catalog
6. `architecture/data-models.yaml` — canonical models, PII treatment
7. `architecture/infrastructure.yaml` — runtime, deployment
8. Relevant ADRs in `architecture/decisions/`

**Architecture YAML files are authoritative.**
Diagrams are derived artifacts and MUST NOT be treated as architecture sources.

## Mandatory Invariants

Never implement a data extraction path that bypasses:

```
Consent → Resource Discovery → Policy Enforcement → Authorized Context → Execution Plan → Pipeline → Lineage → Publication
```

| Invariant | Rule | Enforcement |
|-----------|------|-------------|
| INV-001 | NO_CONSENT_NO_EXECUTION | Runtime |
| INV-002 | NO_AUTHORIZED_RESOURCE_NO_ACCESS | Runtime |
| INV-003 | NO_LINEAGE_NO_PUBLICATION | Runtime |
| INV-004 | TEMPORAL_MUST_NOT_AUTHORIZE | Architecture Test |
| INV-005 | IMMUTABLE_AUTHORIZED_CONTEXT | Runtime |
| INV-006 | CONSENT_VERSIONING | Runtime |
| INV-007 | SCHEMA_BACKWARD_COMPATIBLE | CI |

## What You MUST NOT Do

- Temporal MUST NOT contain authorization or regulatory business rules
- Pipelines MUST NOT access source systems without an AuthorizedContext
- Publication MUST NOT occur without: valid authorization, successful validation, lineage
- Event handlers MUST NOT make synchronous authorization calls
- Extractors MUST NOT bypass the Policy Engine

## Adding a New Open Finance Capability

When implementing a new capability:

1. Register domain/capability in `architecture.yaml`
2. Define canonical schema in `data-models.yaml`
3. Define source adapter interface
4. Implement extractor (extend `BaseExtractor`)
5. Implement normalizer (canonicalization)
6. Implement validation (schema + quality)
7. Register pipeline in `flows.yaml`
8. Add Temporal Activity
9. Add capability to Planner DAG
10. Add policy rules in `policies.yaml`
11. Add lineage tracking
12. Add reconciliation rules
13. Add metrics and alerts
14. Add contract tests
15. Add architecture tests
16. Update `architecture.yaml` relationships

Do not introduce a new architectural component without an ADR.

## Testing Requirements

### Architecture Tests (CI)
- `ArchitectureTest.testNoConsentNoExecution()` — INV-001
- `ArchitectureTest.testNoAuthorizedResourceNoAccess()` — INV-002
- `ArchitectureTest.testNoLineageNoPublication()` — INV-003
- `ArchitectureTest.testTemporalDoesNotAuthorize()` — INV-004
- `ArchitectureTest.testImmutableAuthorizedContext()` — INV-005
- `ArchitectureTest.testConsentVersioning()` — INV-006

### Schema Compatibility Tests
- `SchemaCompatibilityTest.testBackwardCompatible()` — INV-007

### Contract Tests
- Pact consumer/provider tests for all service relationships

## Code Conventions

- **Language**: Java 25, everything in Portuguese
- **Framework**: Spring Boot 4.1.0, Jakarta namespace
- **Naming**: Classes in Portuguese, method names in Portuguese
- **Packages**: `com.aurix.platform.openfinance.<plane>.<component>`
- **Entities**: JPA with `@Entity`, constructor injection
- **DTOs**: Request/Response suffix
- **Events**: Past-tense naming (`ConsentGrantedEvent`)
- **Invariants**: Every invariant has a test class

## File Structure

```
architecture/
├── architecture.yaml          # SOURCE OF TRUTH
├── services.yaml              # Service ownership
├── flows.yaml                 # Executable workflows
├── policies.yaml              # Invariants + rules
├── events.yaml                # Event catalog
├── data-models.yaml           # Canonical models
├── infrastructure.yaml        # Runtime/deploy
├── schemas/                   # JSON Schemas for validation
│   ├── architecture.schema.json
│   ├── services.schema.json
│   ├── flows.schema.json
│   ├── policies.schema.json
│   └── events.schema.json
├── decisions/                 # ADRs
│   ├── ADR-001-temporal-orchestration.md
│   ├── ADR-002-consent-plane.md
│   ├── ADR-003-resource-discovery.md
│   └── ADR-004-execution-plan-dag.md
├── diagrams/                  # Mermaid (derived, NOT source of truth)
│   ├── overview.mmd
│   ├── consent-flow.mmd
│   ├── extraction-flow.mmd
│   └── data-flow.mmd
├── contracts/                 # JSON Schemas for data
│   ├── consent.schema.json
│   └── execution-plan.schema.json
└── rules/
    └── architecture-rules.yaml
```
