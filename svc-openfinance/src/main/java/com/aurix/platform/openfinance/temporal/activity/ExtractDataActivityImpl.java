package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação da atividade de extração de dados.
 * Extrai dados da fonte de origem.
 * Recebe contexto autorizado (já validado pelo Policy Engine).
 * NÃO toma decisões de autorização.
 * Reporta métricas de extração.
 */
@Component
public class ExtractDataActivityImpl implements ExtractDataActivity {

    private static final Logger log = LoggerFactory.getLogger(ExtractDataActivityImpl.class);

    @Override
    public ExtractResult extract(ExtractRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando extração - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();

        try {
            ExtractResult result = new ExtractResult();
            result.setNodeId(request.getNodeId());

            int recordCount = simularExtracao(request.getResource());
            result.setRecordCount(recordCount);
            result.setRawData("dados_extraidos_" + request.getResource());
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Extração concluída - node: {}, registros: {}, duração: {}ms",
                    request.getNodeId(), recordCount, duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na extração - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            ExtractResult result = new ExtractResult();
            result.setNodeId(request.getNodeId());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    private int simularExtracao(String resource) {
        return switch (resource) {
            case "contas" -> 5;
            case "transacoes" -> 150;
            case "cartoes" -> 3;
            case "faturas" -> 12;
            case "transacoes_cartao" -> 200;
            case "emprestimos" -> 2;
            case "seguros" -> 1;
            case "pix" -> 50;
            case "pessoas" -> 1;
            default -> 0;
        };
    }
}
