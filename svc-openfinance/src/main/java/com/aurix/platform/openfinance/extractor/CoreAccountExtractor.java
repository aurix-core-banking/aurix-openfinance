package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.AccountSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extrai contas, saldos e transações via {@link AccountSourceAdapter} — agnóstico
 * de qual core está por trás do adapter (aurix hoje, qualquer outro amanhã).
 * Suporta: ACCOUNTS, BALANCES, TRANSACTIONS.
 */
@Component
public class CoreAccountExtractor extends BaseExtractor {

    private final AccountSourceAdapter sourceAdapter;

    public CoreAccountExtractor(AccountSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<AccountSourceAdapter.Account> contas = sourceAdapter.findAccounts(context.getConsentId());
        List<AccountSourceAdapter.Transaction> transacoes = sourceAdapter.findTransactions(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "contas", contas,
                        "transacoes", transacoes
                ))
                .recordCount(contas.size() + transacoes.size())
                .extractedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.ACCOUNTS
                || type == ResourceType.BALANCES
                || type == ResourceType.TRANSACTIONS;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreAccountExtractor")
                .description("Extração de contas, saldos e transações via AccountSourceAdapter")
                .supportedResourceTypes(List.of(
                        ResourceType.ACCOUNTS,
                        ResourceType.BALANCES,
                        ResourceType.TRANSACTIONS
                ))
                .maxBatchSize(1000)
                .build();
    }
}
