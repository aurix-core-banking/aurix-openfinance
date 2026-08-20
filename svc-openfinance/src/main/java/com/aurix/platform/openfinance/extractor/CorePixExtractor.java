package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.PixSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extrai transações PIX via {@link PixSourceAdapter}.
 * Suporta: PIX_KEYS, PIX_TRANSACTIONS.
 */
@Component
public class CorePixExtractor extends BaseExtractor {

    private final PixSourceAdapter sourceAdapter;

    public CorePixExtractor(PixSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<PixSourceAdapter.PixTransaction> transacoesPix =
                sourceAdapter.findPixTransactions(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of("transacoes_pix", transacoesPix))
                .recordCount(transacoesPix.size())
                .extractedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.PIX_KEYS
                || type == ResourceType.PIX_TRANSACTIONS;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CorePixExtractor")
                .description("Extração de transações PIX via PixSourceAdapter")
                .supportedResourceTypes(List.of(
                        ResourceType.PIX_KEYS,
                        ResourceType.PIX_TRANSACTIONS
                ))
                .maxBatchSize(2000)
                .build();
    }
}
