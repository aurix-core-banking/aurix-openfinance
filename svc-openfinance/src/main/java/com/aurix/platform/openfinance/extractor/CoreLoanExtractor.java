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
 * Extrai dados de emprestimos do core banking.
 * Suporta: LOANS, LOAN_INSTALLMENTS.
 */
@Component
public class CoreLoanExtractor extends BaseExtractor {

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "emprestimos", List.of(),
                        "parcelas", List.of()
                ))
                .recordCount(0)
                .extractedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.LOANS
                || type == ResourceType.LOAN_INSTALLMENTS;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreLoanExtractor")
                .description("Extracao de emprestimos e parcelas do core banking")
                .supportedResourceTypes(List.of(
                        ResourceType.LOANS,
                        ResourceType.LOAN_INSTALLMENTS
                ))
                .maxBatchSize(500)
                .build();
    }
}
