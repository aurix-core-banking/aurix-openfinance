package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extrai dados de investimentos do core banking.
 * Suporta: INVESTMENTS, INVESTMENT_PORTFOLIO.
 */
@Component
public class CoreInvestmentExtractor extends BaseExtractor {

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "investimentos", List.of(),
                        "portfolio", List.of()
                ))
                .recordCount(0)
                .extractedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.INVESTMENTS
                || type == ResourceType.INVESTMENT_PORTFOLIO;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreInvestmentExtractor")
                .description("Extracao de investimentos e portfolio do core banking")
                .supportedResourceTypes(List.of(
                        ResourceType.INVESTMENTS,
                        ResourceType.INVESTMENT_PORTFOLIO
                ))
                .maxBatchSize(500)
                .build();
    }
}
