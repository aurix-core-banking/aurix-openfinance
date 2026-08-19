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
 * Extrai dados de contas do core banking.
 * Suporta: ACCOUNTS, BALANCES, TRANSACTIONS.
 */
@Component
public class CoreAccountExtractor extends BaseExtractor {

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        // Consulta PostgreSQL para dados de conta
        // Filtra por autorização do consentimento
        // Retorna dados brutos da conta
        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of(
                        "contas", List.of(),
                        "saldos", List.of(),
                        "transacoes", List.of()
                ))
                .recordCount(0)
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
                .description("Extração de contas, saldos e transações do core banking")
                .supportedResourceTypes(List.of(
                        ResourceType.ACCOUNTS,
                        ResourceType.BALANCES,
                        ResourceType.TRANSACTIONS
                ))
                .maxBatchSize(1000)
                .build();
    }
}
