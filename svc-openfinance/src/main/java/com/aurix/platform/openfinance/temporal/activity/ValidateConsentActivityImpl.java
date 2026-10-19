package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.service.AuthorizedContextService;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação da atividade de validação de vigência de consentimento/contexto.
 * INV-004: consulta exclusivamente {@link AuthorizedContextService} (contextos já
 * autorizados pelo Consent Plane), sem acionar o Policy Engine dentro do Temporal.
 */
@Component
public class ValidateConsentActivityImpl implements ValidateConsentActivity {

    private static final Logger log = LoggerFactory.getLogger(ValidateConsentActivityImpl.class);

    private final AuthorizedContextService authorizedContextService;

    public ValidateConsentActivityImpl(AuthorizedContextService authorizedContextService) {
        this.authorizedContextService = authorizedContextService;
    }

    @Override
    public ConsentValidationResult validateConsent(ConsentValidationRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Validando vigência de consentimento - consentId: {}, planId: {}, activity: {}",
                request.getConsentId(), request.getPlanId(), activityId);

        ConsentValidationResult result = new ConsentValidationResult();
        result.setConsentId(request.getConsentId());

        List<AuthorizedContext> activeContexts = authorizedContextService.listActiveByConsentId(request.getConsentId());
        result.setActiveContextsCount(activeContexts.size());

        if (activeContexts.isEmpty()) {
            result.setValid(false);
            result.setReason("Nenhum AuthorizedContext ativo para o consentimento " + request.getConsentId());
            return result;
        }

        if (request.getExpectedVersion() != null) {
            boolean versaoCompativel = activeContexts.stream()
                    .anyMatch(ctx -> request.getExpectedVersion().equals(ctx.getConsentVersion()));
            if (!versaoCompativel) {
                result.setValid(false);
                result.setReason("Versão do consentimento divergiu do esperado (" + request.getExpectedVersion() + ")");
                return result;
            }
        }

        result.setValid(true);
        result.setReason("Contexto autorizado vigente (" + activeContexts.size() + " ativo(s))");
        return result;
    }
}
