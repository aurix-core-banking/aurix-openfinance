package com.aurix.platform.openfinance.kafka;

import com.aurix.platform.openfinance.service.SeguroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class SeguroEventListener {

    private static final Logger log = LoggerFactory.getLogger(SeguroEventListener.class);
    private final SeguroService seguroService;

    public SeguroEventListener(SeguroService seguroService) {
        this.seguroService = seguroService;
    }

    @KafkaListener(topics = {"core.seguro.contratado.v1", "core.seguro.atualizado.v1",
            "core.seguro.cancelado.v1"}, groupId = "openfinance-seguro-consumer")
    public void onSeguro(Map<String, Object> event) {
        log.info("Recebido evento de seguro: {}", event);
        try {
            String consentId = (String) event.get("consentId");
            if (consentId == null) return;

            seguroService.sincronizar(
                    consentId,
                    (String) event.get("apoliceId"),
                    (String) event.get("clienteId"),
                    (String) event.get("tipoSeguro"),
                    (String) event.get("nomeSeguradora"),
                    new BigDecimal(event.getOrDefault("premioMensal", "0").toString()),
                    new BigDecimal(event.getOrDefault("premioTotal", "0").toString()),
                    new BigDecimal(event.getOrDefault("valorSegurado", "0").toString()),
                    event.get("dataInicio") != null ? LocalDate.parse((String) event.get("dataInicio")) : null,
                    event.get("dataFim") != null ? LocalDate.parse((String) event.get("dataFim")) : null,
                    (String) event.getOrDefault("statusApolice", "ATIVA")
            );
            log.info("Seguro {} sincronizado", event.get("apoliceId"));
        } catch (Exception e) {
            log.error("Erro ao processar evento de seguro: {}", e.getMessage(), e);
        }
    }
}
