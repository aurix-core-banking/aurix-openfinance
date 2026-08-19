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
 * Extrai dados de cartoes de credito do core banking.
 * Suporta: CREDIT_CARDS, CARD_BILLS, CARD_TRANSACTIONS.
 */
@Component
public class CoreCardExtractor extends BaseExtractor {

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "cartoes", List.of(),
                        "faturas", List.of(),
                        "transacoes_cartao", List.of()
                ))
                .recordCount(0)
                .extractedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.CREDIT_CARDS
                || type == ResourceType.CARD_BILLS
                || type == ResourceType.CARD_TRANSACTIONS;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreCardExtractor")
                .description("Extracao de cartoes de credito, faturas e transacoes")
                .supportedResourceTypes(List.of(
                        ResourceType.CREDIT_CARDS,
                        ResourceType.CARD_BILLS,
                        ResourceType.CARD_TRANSACTIONS
                ))
                .maxBatchSize(500)
                .build();
    }
}
