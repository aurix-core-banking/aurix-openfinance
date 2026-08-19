# Capability Registration Guide

## How to Add a New Open Finance Capability

This guide walks through adding a new data sharing capability (e.g., "credit-score").

### Step 1: Register in architecture.yaml

Add to the appropriate domain:

```yaml
domains:
  credit:
    capabilities:
      score:
        pipeline: CreditScorePipeline
        extractor: CoreCreditExtractor
        source: PostgreSQL
```

### Step 2: Define Canonical Model in data-models.yaml

```yaml
canonicalModels:
  creditScore:
    name: CanonicalCreditScore
    fields:
      - name: scoreId
        type: string
        required: true
      - name: documentNumber
        type: string
        required: true
        pii: true
        protection: hash
      - name: score
        type: integer
        required: true
      - name: provider
        type: string
        required: true
      - name: timestamp
        type: datetime
        required: true
```

### Step 3: Implement Extractor

```java
@Component
public class CoreCreditExtractor extends BaseExtractor {
    
    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        // Query source system
        // Filter by authorized resources only
        return RawData.of("credit-score", rawData);
    }
    
    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.CREDIT_SCORE;
    }
}
```

### Step 4: Add to ExtractorRegistry

```java
@Component
public class ExtractorRegistry {
    // Add: CoreCreditExtractor
}
```

### Step 5: Add Flow in flows.yaml

```yaml
- id: credit-score-data-sharing
  trigger:
    flow: consent-to-execution
    condition: "consent.permissions contains CREDIT_SCORE"
  executionPlan:
    nodes:
      - id: credit-score
        capability: CREDIT_SCORE
        dependsOn: []
        extractor: CoreCreditExtractor
      - id: publish
        capability: PUBLICATION
        dependsOn:
          - credit-score
```

### Step 6: Add Migration

```sql
-- V7__credit_score.sql
CREATE TABLE aurix.credit_scores ( ... );
```

### Step 7: Add Tests

- Unit test for extractor
- Integration test for pipeline
- Architecture test for invariants
- Contract test for API

### Step 8: Update Architecture Tests

Add any new tests to `ArchitectureTest.java` if needed.

### Step 9: Update ADR

Create `ADR-00X-credit-score-capability.md` documenting the decision.

### Step 10: Update relationships in services.yaml

```yaml
relationships:
  - from: core-credit-extractor
    to: credit-score-service
    type: extracts-from
    contract: CreditScoreData
```
