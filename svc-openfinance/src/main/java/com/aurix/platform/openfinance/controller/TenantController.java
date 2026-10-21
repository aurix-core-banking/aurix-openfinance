package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.TenantInfoResponse;
import com.aurix.platform.openfinance.extractor.connector.CoreBankingConnectorRegistry;
import com.aurix.platform.openfinance.tenant.TenantContext;
import com.aurix.platform.openfinance.tenant.TenantProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de metadados do Tenant para clientes e frontends White-Label.
 */
@RestController
@RequestMapping("/open-finance/v1/tenant")
@Tag(name = "Tenant & White-Label", description = "Identificação institucional e parametrização B2B")
public class TenantController {

    private final TenantProperties tenantProperties;
    private final CoreBankingConnectorRegistry connectorRegistry;

    public TenantController(TenantProperties tenantProperties, CoreBankingConnectorRegistry connectorRegistry) {
        this.tenantProperties = tenantProperties;
        this.connectorRegistry = connectorRegistry;
    }

    @GetMapping("/info")
    @Operation(summary = "Obter informações do Tenant e marca da instituição")
    public ResponseEntity<TenantInfoResponse> getInfo() {
        TenantInfoResponse info = new TenantInfoResponse(
                TenantContext.getTenantId(),
                tenantProperties.getName(),
                tenantProperties.getCnpj(),
                tenantProperties.getParticipantCode(),
                tenantProperties.getBrandName(),
                tenantProperties.getPrimaryColor(),
                tenantProperties.getLogoUrl(),
                connectorRegistry.getConectorAtivo().getNome()
        );
        return ResponseEntity.ok(info);
    }
}
