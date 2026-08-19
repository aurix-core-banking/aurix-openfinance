package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.extractor.dto.ExtractorCapabilities;
import com.aurix.platform.openfinance.extractor.dto.ValidationResult;
import com.aurix.platform.openfinance.extractor.exception.ExtractorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base abstrata com lógica comum: retry, circuit breaker, métricas.
 * Template Method: subclasses implementam doExtract().
 */
public abstract class BaseExtractor implements DataExtractor {

    private static final Logger log = LoggerFactory.getLogger(BaseExtractor.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    @Override
    public RawData extract(AuthorizedContext context, ResourceDescriptor resource) {
        String extractionId = UUID.randomUUID().toString();

        // 1. Validar que contexto está ativo (INV05 — contexto imutável)
        if (!context.isActive()) {
            throw new ExtractorException("Contexto não está ativo: " + context.getContextId());
        }

        // 2. Validar que recurso é autorizado (INV02)
        if (!isResourceAuthorized(context, resource)) {
            throw new ExtractorException("Recurso não autorizado: " + resource.getResourceId());
        }

        log.info("Iniciando extração [{}] para recurso {} no contexto {}",
                extractionId, resource.getResourceId(), context.getContextId());

        // 3. Chamar doExtract (template method) com retry
        RawData rawData = executeWithRetry(context, resource, extractionId);

        // 4. Registrar métricas
        recordMetrics(extractionId, resource, rawData);

        log.info("Extração concluída [{}]: {} registros extraídos",
                extractionId, rawData.getRecordCount());

        return rawData;
    }

    @Override
    public ValidationResult validate(RawData data) {
        if (data == null) {
            return ValidationResult.invalid("Dados nulos");
        }
        if (data.getPayload() == null || data.getPayload().isEmpty()) {
            return ValidationResult.invalid("Payload vazio");
        }
        return ValidationResult.valid();
    }

    /**
     * Template method — subclasses implementam a lógica específica de extração.
     */
    protected abstract RawData doExtract(AuthorizedContext context, ResourceDescriptor resource);

    /**
     * Verifica se o recurso está autorizado pelo contexto.
     */
    private boolean isResourceAuthorized(AuthorizedContext context, ResourceDescriptor resource) {
        // Validação básica — implementação real consultaria o AuthorizedContext
        return context.getResourceGraph() != null
                && context.getResourceGraph().contains(resource.getResourceId());
    }

    /**
     * Executa extração com retry em caso de falha transitória.
     */
    private RawData executeWithRetry(AuthorizedContext context, ResourceDescriptor resource,
                                      String extractionId) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                return doExtract(context, resource);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                log.warn("Tentativa {} falhou para extração [{}]: {}",
                        attempts, extractionId, e.getMessage());

                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ExtractorException("Extração interrompida", ie);
                    }
                }
            }
        }

        throw new ExtractorException(
                "Extração falhou após " + MAX_RETRIES + " tentativas: " + extractionId,
                lastException);
    }

    /**
     * Registra métricas da extração.
     */
    private void recordMetrics(String extractionId, ResourceDescriptor resource, RawData data) {
        log.debug("Métricas [{}]: tipo={}, registros={}, tamanho={} bytes",
                extractionId,
                resource.getResourceType(),
                data.getRecordCount(),
                data.getPayloadSize());
    }
}
