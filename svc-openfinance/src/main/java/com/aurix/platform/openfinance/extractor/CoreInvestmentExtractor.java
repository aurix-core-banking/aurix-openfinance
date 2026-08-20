package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.InvestmentSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extrai investimentos via {@link InvestmentSourceAdapter}.
 * Suporta: INVESTMENTS, INVESTMENT_PORTFOLIO.
 *
 * <p>Nenhum core plugado hoje fornece dados de investimento — o adapter é opcional
 * (via {@code Optional<InvestmentSourceAdapter>}) para que o contexto Spring suba
 * normalmente sem essa capability; assim que alguém registrar um bean implementando
 * {@link InvestmentSourceAdapter}, este extractor passa a retornar dados reais sem
 * qualquer mudança de código aqui.
 */
@Component
public class CoreInvestmentExtractor extends BaseExtractor {

    private final Optional<InvestmentSourceAdapter> sourceAdapter;

    public CoreInvestmentExtractor(Optional<InvestmentSourceAdapter> sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<InvestmentSourceAdapter.Investment> investimentos = sourceAdapter
                .map(adapter -> adapter.findInvestments(context.getConsentId()))
                .orElseGet(List::of);

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of("investimentos", investimentos))
                .recordCount(investimentos.size())
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
                .description("Extração de investimentos via InvestmentSourceAdapter (opcional)")
                .supportedResourceTypes(List.of(
                        ResourceType.INVESTMENTS,
                        ResourceType.INVESTMENT_PORTFOLIO
                ))
                .maxBatchSize(500)
                .build();
    }
}
