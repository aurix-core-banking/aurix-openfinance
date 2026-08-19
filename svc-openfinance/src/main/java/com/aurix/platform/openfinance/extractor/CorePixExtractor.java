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
 * Extrai dados de PIX do core banking.
 * Suporta: PIX_KEYS, PIX_TRANSACTIONS.
 */
@Component
public class CorePixExtractor extends BaseExtractor {

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "chaves_pix", List.of(),
                        "transacoes_pix", List.of()
                ))
                .recordCount(0)
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
                .description("Extracao de chaves PIX e transacoes PIX")
                .supportedResourceTypes(List.of(
                        ResourceType.PIX_KEYS,
                        ResourceType.PIX_TRANSACTIONS
                ))
                .maxBatchSize(2000)
                .build();
    }
}
