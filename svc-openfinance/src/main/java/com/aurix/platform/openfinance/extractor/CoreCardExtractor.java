package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.CardSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extrai cartões de crédito, faturas e transações via {@link CardSourceAdapter}.
 * Suporta: CREDIT_CARDS, CARD_BILLS, CARD_TRANSACTIONS.
 */
@Component
public class CoreCardExtractor extends BaseExtractor {

    private final CardSourceAdapter sourceAdapter;

    public CoreCardExtractor(CardSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<CardSourceAdapter.Card> cartoes = sourceAdapter.findCards(context.getConsentId());
        List<CardSourceAdapter.Bill> faturas = sourceAdapter.findBills(context.getConsentId());
        List<CardSourceAdapter.CardTransaction> transacoes =
                sourceAdapter.findCardTransactions(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "cartoes", cartoes,
                        "faturas", faturas,
                        "transacoes_cartao", transacoes
                ))
                .recordCount(cartoes.size() + faturas.size() + transacoes.size())
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
                .description("Extração de cartões, faturas e transações via CardSourceAdapter")
                .supportedResourceTypes(List.of(
                        ResourceType.CREDIT_CARDS,
                        ResourceType.CARD_BILLS,
                        ResourceType.CARD_TRANSACTIONS
                ))
                .maxBatchSize(500)
                .build();
    }
}
