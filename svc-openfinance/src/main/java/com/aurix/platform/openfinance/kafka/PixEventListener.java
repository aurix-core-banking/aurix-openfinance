package com.aurix.platform.openfinance.kafka;

import com.aurix.platform.openfinance.service.PixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class PixEventListener {

    private static final Logger log = LoggerFactory.getLogger(PixEventListener.class);
    private final PixService pixService;

    public PixEventListener(PixService pixService) {
        this.pixService = pixService;
    }

    @KafkaListener(topics = {"core.pix.enviado.v1", "core.pix.recebido.v1",
            "core.pix.cancelado.v1"}, groupId = "openfinance-pix-consumer")
    public void onPix(Map<String, Object> event) {
        log.info("Recebido evento PIX: {}", event);
        try {
            String consentId = (String) event.get("consentId");
            if (consentId == null) return;

            pixService.sincronizar(
                    consentId,
                    (String) event.get("pixId"),
                    (String) event.get("clienteId"),
                    (String) event.get("tipoPix"),
                    (String) event.get("chavePix"),
                    (String) event.get("tipoChave"),
                    new BigDecimal(event.getOrDefault("valor", "0").toString()),
                    (String) event.getOrDefault("moeda", "BRL"),
                    (String) event.get("descricao"),
                    (String) event.getOrDefault("statusPix", "EFETIVADO"),
                    event.get("dataPix") != null ? LocalDateTime.parse((String) event.get("dataPix")) : LocalDateTime.now()
            );
            log.info("PIX {} sincronizado", event.get("pixId"));
        } catch (Exception e) {
            log.error("Erro ao processar evento PIX: {}", e.getMessage(), e);
        }
    }
}
