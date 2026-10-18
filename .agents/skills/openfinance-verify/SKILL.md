---
name: openfinance-verify
description: Runs the Aurix Open Finance architecture invariants (INV-001 to INV-007), schema compatibility, contract, and Temporal workflow integration tests using the local Maven wrapper distribution. Use when verifying Open Finance compliance, invariants, or running tests before completing a task or opening a PR.
---

# Open Finance Verification Skill (`openfinance-verify`)

## Overview

Validates all mandatory architectural invariants (`INV-001` through `INV-007`) declared in `AGENTS.md` and `architecture/policies.yaml`, along with Pact contract tests and in-process Temporal E2E workflow tests.

## Maven Wrapper Command (Windows / PowerShell)

Because `mvn` and `mvnw` are not in the global `PATH` or repository root, always execute Maven via the local wrapper distribution:

```powershell
& "C:\Users\wende\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd" test
```

## Verification Checklist

1. **Architecture & Invariant Tests (`INV-001` to `INV-006`)**
   ```powershell
   & "C:\Users\wende\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd" test -Dtest=ArchitectureTest
   ```
   - `testNoConsentNoExecution` (`INV-001`)
   - `testNoAuthorizedResourceNoAccess` (`INV-002`)
   - `testNoLineageNoPublication` (`INV-003`)
   - `testTemporalDoesNotAuthorize` (`INV-004`)
   - `testImmutableAuthorizedContext` (`INV-005`)
   - `testConsentVersioning` (`INV-006`)
   - `testConsentPlaneDependencies` & `testNoCircularDependencies`

2. **Schema Backward Compatibility & Canonical JSON Validation (`INV-007`)**
   ```powershell
   & "C:\Users\wende\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd" test -Dtest=SchemaCompatibilityTest
   ```

3. **Pipeline, Canonicalization, Quality & Lineage Tests**
   ```powershell
   & "C:\Users\wende\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd" test -Dtest="CanonicalizationServiceTest,DataQualityServiceTest,PipelineOrchestratorLineageTest"
   ```

4. **End-to-End Temporal Workflow & Distribution Plane Tests**
   ```powershell
   & "C:\Users\wende\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd" test -Dtest="DataExtractionWorkflowIntegrationTest,ExtractionPlannerServiceTest,DataPublishedEventListenerTest"
   ```

## Layer & Slice Rules to Respect

- `ExecutionPlane` (`..planner..`, `..temporal..`, `..reconciliation..`) MUST NOT be accessed by any other layer.
- `ConsentPlane` (`..policy..`, `..context..`, `..discovery..`) may ONLY be accessed by `ExecutionPlane` and `DataPlane` (`..pipeline..`, `..extractor..`).
- Top-level slices `com.aurix.platform.openfinance.(*)..` MUST remain free of circular dependencies.
