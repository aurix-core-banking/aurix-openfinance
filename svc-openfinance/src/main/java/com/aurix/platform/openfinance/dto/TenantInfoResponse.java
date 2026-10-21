package com.aurix.platform.openfinance.dto;

/**
 * DTO com dados de identificação e personalização White-Label da instituição financeira.
 */
public record TenantInfoResponse(
        String tenantId,
        String razaoSocial,
        String cnpj,
        String codigoParticipanteBacen,
        String nomeMarca,
        String corPrimaria,
        String urlLogo,
        String tipoConectorCore
) {
}
