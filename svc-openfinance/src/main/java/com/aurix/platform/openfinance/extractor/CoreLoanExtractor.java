package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.LoanSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extrai empréstimos via {@link LoanSourceAdapter}.
 * Suporta: LOANS, LOAN_INSTALLMENTS.
 */
@Component
public class CoreLoanExtractor extends BaseExtractor {

    private final LoanSourceAdapter sourceAdapter;

    public CoreLoanExtractor(LoanSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<LoanSourceAdapter.Loan> emprestimos = sourceAdapter.findLoans(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of("emprestimos", emprestimos))
                .recordCount(emprestimos.size())
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
                .description("Extração de empréstimos via LoanSourceAdapter")
                .supportedResourceTypes(List.of(
                        ResourceType.LOANS,
                        ResourceType.LOAN_INSTALLMENTS
                ))
                .maxBatchSize(500)
                .build();
    }
}
