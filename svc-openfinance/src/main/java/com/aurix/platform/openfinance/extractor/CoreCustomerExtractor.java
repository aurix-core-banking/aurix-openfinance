package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.CustomerSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Extrai dados cadastrais de clientes via {@link CustomerSourceAdapter} (INV-001, INV-002).
 */
@Component
public class CoreCustomerExtractor extends BaseExtractor {

    private final CustomerSourceAdapter sourceAdapter;

    public CoreCustomerExtractor(CustomerSourceAdapter sourceAdapter) {
        this.sourceAdapter = sourceAdapter;
    }

    @Override
    protected RawData doExtract(AuthorizedContext context, ResourceDescriptor resource) {
        List<CustomerSourceAdapter.CustomerRecord> pessoas =
                sourceAdapter.findCustomers(context.getConsentId());

        return RawData.builder()
                .resourceType(resource.getResourceType())
                .contextId(context.getContextId())
                .payload(Map.of("pessoas", pessoas))
                .recordCount(pessoas.size())
                .extractedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(ResourceType type) {
        return type == ResourceType.CUSTOMERS;
    }

    @Override
    public ExtractorCapabilities getCapabilities() {
        return ExtractorCapabilities.builder()
                .name("CoreCustomerExtractor")
                .description("Extração de dados cadastrais via CustomerSourceAdapter")
                .supportedResourceTypes(List.of(ResourceType.CUSTOMERS))
                .maxBatchSize(200)
                .build();
    }
}
