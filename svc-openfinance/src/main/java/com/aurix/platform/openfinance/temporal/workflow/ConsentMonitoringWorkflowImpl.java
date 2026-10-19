package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.temporal.activity.ValidateConsentActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Implementação do workflow de monitoramento de consentimento.
 * Verifica a vigência do contexto autorizado via {@link ValidateConsentActivity}
 * e reage a sinais de revogação do consentimento.
 */
public class ConsentMonitoringWorkflowImpl implements ConsentMonitoringWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ConsentMonitoringWorkflowImpl.class);

    private final ValidateConsentActivity validateConsentActivity = Workflow.newActivityStub(
            ValidateConsentActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());

    private final MonitoringStatus status = new MonitoringStatus();
    private boolean revoked = false;
    private String revocationReason;

    @Override
    public MonitoringResult monitorar(MonitoringInput input) {
        log.info("Iniciando monitoramento de consentimento: consentId={}, planId={}",
                input.getConsentId(), input.getPlanId());

        status.setConsentId(input.getConsentId());
        status.setActive(true);

        if (revoked) {
            status.setActive(false);
            status.setRevoked(true);
            status.setRevocationReason(revocationReason);
            MonitoringResult result = new MonitoringResult();
            result.setConsentId(input.getConsentId());
            result.setPlanId(input.getPlanId());
            result.setConsentValid(false);
            result.setRevoked(true);
            result.setReason(revocationReason);
            return result;
        }

        ValidateConsentActivity.ConsentValidationRequest req =
                new ValidateConsentActivity.ConsentValidationRequest(
                        input.getConsentId(), input.getPlanId(), input.getExpectedVersion());
        ValidateConsentActivity.ConsentValidationResult validation =
                validateConsentActivity.validateConsent(req);

        status.setActive(false);
        status.setRevoked(revoked || !validation.isValid());
        status.setRevocationReason(revoked ? revocationReason : validation.getReason());

        MonitoringResult result = new MonitoringResult();
        result.setConsentId(input.getConsentId());
        result.setPlanId(input.getPlanId());
        result.setConsentValid(!revoked && validation.isValid());
        result.setRevoked(revoked);
        result.setReason(revoked ? revocationReason : validation.getReason());
        return result;
    }

    @Override
    public void notificarRevogacao(String motivo) {
        log.info("Sinal de revogação recebido no monitoramento de consentimento: {}", motivo);
        this.revoked = true;
        this.revocationReason = motivo;
        this.status.setRevoked(true);
        this.status.setRevocationReason(motivo);
    }

    @Override
    public MonitoringStatus getStatus() {
        return status;
    }
}
