---
name: openfinance-capability
description: Step-by-step workflow for adding or extending an Open Finance domain capability in aurix-openfinance following AGENTS.md and architecture/*.yaml. Use when adding a new financial resource type, extractor, canonical model, validation schema, or pipeline stage.
---

# Open Finance Capability Skill (`openfinance-capability`)

## Overview

Guides the implementation of a new or extended Open Finance capability across all 5 architectural planes (`Consent`, `Execution`, `Data`, `Distribution`, `Trust & Governance`) without bypassing any mandatory invariant (`INV-001` to `INV-007`).

## Mandatory Extraction & Publication Path

Every capability MUST traverse the complete chain:

```text
Consent → Resource Discovery → Policy Enforcement → Authorized Context → Execution Plan → Pipeline → Lineage → Publication → DataProduct Materialization
```

## 16-Step Implementation Checklist

1. **Architecture Registration**: Register the domain and capability in `architecture/architecture.yaml` and `architecture/services.yaml`.
2. **Canonical Data Model**: Define the canonical schema and PII sensitivity classification in `architecture/data-models.yaml` and create the record/model class in `com.aurix.platform.openfinance.pipeline.canonicalization.model`.
3. **Source Adapter Interface**: Define the source adapter contract in `com.aurix.platform.openfinance.adapter` and its JPA-backed implementation in `com.aurix.platform.openfinance.adapter.impl`.
4. **Extractor**: Implement the extractor extending `BaseExtractor` in `com.aurix.platform.openfinance.extractor.impl` (validating `AuthorizedContext` before accessing the source adapter — `INV-002`).
5. **Canonicalization (Normalizer)**: Add envelope unwrapping (`desempacotarNodo`) and canonical field mapping in `CanonicalizationService`.
6. **Schema Validation**: Register required canonical fields from `architecture/data-models.yaml` in `SchemaValidationService.camposObrigatoriosPorTipo` and test backward compatibility in `SchemaCompatibilityTest` (`INV-007`).
7. **Data Quality Rules**: Register domain accuracy/consistency rules in `DataQualityService.getRules` and evaluate them in `DataQualityService.avaliarRegra`.
8. **PII Classification**: Ensure sensitive fields (`CPF`, `CNPJ`, account/card numbers, PIX keys) are masked by `PiiClassificationService` before publication.
9. **Flows Registration**: Register the pipeline and DAG dependencies in `architecture/flows.yaml`.
10. **Temporal Activity Mapping**: Map the domain resource name to its `ResourceType` in `TransformDataActivityImpl.RESOURCE_TYPE_BY_DOMAIN`.
11. **Planner DAG**: Register resource dependencies (`RESOURCE_DEPENDENCIES`) and timeouts (`RESOURCE_TIMEOUTS`) in `ExtractionPlannerService`.
12. **Policy Rules**: Add scope/permission rules in `architecture/policies.yaml` and `PolicyEvaluationService`.
13. **Lineage Tracking**: Ensure `PipelineOrchestrator` records `LineageRecord` and `PublishDataActivityImpl` updates `publicationId` (`INV-003`).
14. **Reconciliation**: Register source-count vs. published-count verification in `ReconciliationService`.
15. **Contract & E2E Tests**: Add unit tests (`CanonicalizationServiceTest`, `DataQualityServiceTest`, `SchemaCompatibilityTest`) and multi-domain workflow verification in `DataExtractionWorkflowIntegrationTest`.
16. **Architecture Verification**: Run the `openfinance-verify` skill to confirm all 12 ArchUnit rules in `ArchitectureTest` pass.
