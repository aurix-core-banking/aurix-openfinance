package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import com.aurix.platform.openfinance.extractor.dto.ValidationResult;

/**
 * Interface core para todos os extractors.
 * INV02: Extractor acessa apenas recursos explicitamente autorizados.
 */
public interface DataExtractor {

    /**
     * Extrai dado dado contexto autorizado e recurso.
     * O contexto já foi validado pelo Policy Engine.
     */
    RawData extract(AuthorizedContext context, ResourceDescriptor resource);

    /**
     * Valida dados extraídos.
     */
    ValidationResult validate(RawData data);

    /**
     * Verifica se o extractor suporta este tipo de recurso.
     */
    boolean supports(ResourceType resourceType);

    /**
     * Retorna capacidades do extractor.
     */
    ExtractorCapabilities getCapabilities();
}
