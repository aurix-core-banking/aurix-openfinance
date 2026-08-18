package com.aurix.platform.openfinance.kafka;

import com.aurix.platform.openfinance.service.CartaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class FaturaEventListener {

    private static final Logger log = LoggerFactory.getLogger(FaturaEventListener.class);
    private final CartaoService cartaoService;

    public FaturaEventListener(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    @KafkaListener(topics = {"cartoes.fatura.fechada.v1", "cartoes.fatura.paga.v1"}, groupId = "openfinance-fatura-consumer")
    public void onFatura(Map<String, Object> event) {
        log.info("Recebido evento de fatura: {}", event);
        try {
            String consentId = (String) event.get("consentId");
            if (consentId == null) return;

            String cartaoId = (String) event.get("cartaoId");
            String faturaId = (String) event.get("faturaId");
            BigDecimal valorTotal = event.get("valorTotal") != null ? new BigDecimal(event.get("valorTotal").toString()) : BigDecimal.ZERO;
            BigDecimal valorMinimo = event.get("valorMinimo") != null ? new BigDecimal(event.get("valorMinimo").toString()) : BigDecimal.ZERO;
            BigDecimal valorPago = event.get("valorPago") != null ? new BigDecimal(event.get("valorPago").toString()) : BigDecimal.ZERO;

            LocalDate dataVencimento = event.get("dataVencimento") != null ? LocalDate.parse((String) event.get("dataVencimento")) : null;
            LocalDate dataPagamento = event.get("dataPagamento") != null ? LocalDate.parse((String) event.get("dataPagamento")) : null;

            cartaoService.sincronizarFatura(consentId, cartaoId, faturaId,
                    valorTotal, valorMinimo, valorPago, dataVencimento, dataPagamento);

            log.info("Fatura {} sincronizada para consentimento {}", faturaId, consentId);
        } catch (Exception e) {
            log.error("Erro ao processar evento de fatura: {}", e.getMessage(), e);
        }
    }
}
