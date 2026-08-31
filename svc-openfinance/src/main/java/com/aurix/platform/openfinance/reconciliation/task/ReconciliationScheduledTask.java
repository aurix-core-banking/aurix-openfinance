package com.aurix.platform.openfinance.reconciliation.task;

import com.aurix.platform.openfinance.reconciliation.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarefa agendada que varre registros de reconciliação órfãos/abandonados
 * (em DIVERGENCIA_DETECTADA ou FALHADA há mais tempo que o limite) e tenta o
 * reparo automático — fecha o laço de reconciliação pós-publicação (Gap 8),
 * evitando que divergências não resolvidas acumulem sem ação.
 */
@Component
public class ReconciliationScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduledTask.class);

    private static final int ORFAO_MAX_AGE_MINUTES = 30;
    private static final int ORFAO_MAX_RETRIES = 5;

    private final ReconciliationService reconciliationService;

    public ReconciliationScheduledTask(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Executa a cada 30 minutos a varredura de órfãos de reconciliação.
     */
    @Scheduled(fixedRate = 1800000)
    public void varrerOrfaosDeReconciliacao() {
        log.info("Iniciando varredura agendada de órfãos de reconciliação");
        int reparados = reconciliationService.repararOrfaos(ORFAO_MAX_AGE_MINUTES, ORFAO_MAX_RETRIES);
        log.info("Varredura agendada concluída: reparos tentados={}", reparados);
    }
}
