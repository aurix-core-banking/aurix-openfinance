package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.InsuranceSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Extrai apólices de seguro via {@link InsuranceSourceAdapter} (INV-001, INV-002).
 */
@Component
public class CoreInsuranceExtractor extends BaseExtractor {

    private final InsuranceSourceAdapter sourceAdapter;

    public CoreInsuranceExtractor(InsuranceSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<InsuranceSourceAdapter.InsurancePolicy> seguros =
                sourceAdapter.findPolicies(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of("seguros", seguros))
                .recordCount(seguros.size())
                .extractedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.INSURANCE;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreInsuranceExtractor")
                .description("Extração de apólices de seguro via InsuranceSourceAdapter")
                .supportedResourceTypes(List.of(ResourceType.INSURANCE))
                .maxBatchSize(200)
                .build();
    }
}
