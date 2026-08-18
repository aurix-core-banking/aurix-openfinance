package com.aurix.platform.openfinance.kafka;

import com.aurix.platform.openfinance.service.EmprestimoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class EmprestimoEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmprestimoEventListener.class);
    private final EmprestimoService emprestimoService;

    public EmprestimoEventListener(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @KafkaListener(topics = {"core.emprestimo.concedido.v1", "core.emprestimo.atualizado.v1",
            "consignado.emprestimo.concedido.v1", "consignado.emprestimo.quitado.v1",
            "financiamento.concedido.v1", "financiamento.atualizado.v1"}, groupId = "openfinance-emprestimo-consumer")
    public void onEmprestimo(Map<String, Object> event) {
        log.info("Recebido evento de empréstimo: {}", event);
        try {
            String consentId = (String) event.get("consentId");
            if (consentId == null) return;

            emprestimoService.sincronizar(
                    consentId,
                    (String) event.get("emprestimoId"),
                    (String) event.get("clienteId"),
                    (String) event.get("tipoEmprestimo"),
                    new BigDecimal(event.getOrDefault("valorContratado", "0").toString()),
                    new BigDecimal(event.getOrDefault("valorSaldoDevedor", "0").toString()),
                    new BigDecimal(event.getOrDefault("taxaJuros", "0").toString()),
                    (Integer) event.getOrDefault("prazoMeses", 0),
                    (Integer) event.getOrDefault("parcelasPagas", 0),
                    (Integer) event.getOrDefault("parcelasRestantes", 0),
                    new BigDecimal(event.getOrDefault("valorParcela", "0").toString()),
                    event.get("dataContratacao") != null ? LocalDate.parse((String) event.get("dataContratacao")) : null,
                    event.get("dataVencimentoPrimeiraParcela") != null ? LocalDate.parse((String) event.get("dataVencimentoPrimeiraParcela")) : null,
                    (String) event.getOrDefault("statusEmprestimo", "ATIVO")
            );
            log.info("Empréstimo {} sincronizado", event.get("emprestimoId"));
        } catch (Exception e) {
            log.error("Erro ao processar evento de empréstimo: {}", e.getMessage(), e);
        }
    }
}
