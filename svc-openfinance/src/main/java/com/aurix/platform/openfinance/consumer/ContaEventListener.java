package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.shared.event.ContaEvent;
import com.aurix.platform.shared.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ContaEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContaEventListener.class);
    private final ContaConsentidaRepository contaRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public ContaEventListener(ContaConsentidaRepository contaRepository,
                              ConsentimentoRepository consentimentoRepository) {
        this.contaRepository = contaRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    @KafkaListener(
        topics = {Topics.CONTA_CRIADA, Topics.CONTA_ATUALIZADA},
        groupId = "aurix-openfinance-conta-group"
    )
    public void onContaEvent(ContaEvent event) {
        log.info("Evento Open Finance recebido: {} — Conta={}", event.getEventType(), event.getContaId());

        List<Consentimento> consentimentosAtivos = consentimentoRepository
            .findByStatus(Consentimento.StatusConsentimento.AUTHORISED);

        for (Consentimento consentimento : consentimentosAtivos) {
            if (isExpired(consentimento)) continue;

            List<ContaConsentida> existentes = contaRepository
                .findByConsentId(consentimento.getConsentId());

            boolean jaExiste = existentes.stream()
                .anyMatch(c -> c.getAccountId().equals(event.getContaId()));

            if (!jaExiste) {
                ContaConsentida conta = new ContaConsentida();
                conta.setConsentId(consentimento.getConsentId());
                conta.setAccountId(event.getContaId());
                conta.setInstitutionCode("AURIX");
                conta.setMoeda("BRL");
                conta.setTipoConta(mapTipoConta(event.getTipoConta()));
                conta.setStatusConta(event.getStatus() != null ? event.getStatus() : "ACTIVE");
                conta.setSaldoDisponivel(event.getSaldo());
                conta.setSaldoAtual(event.getSaldo());
                conta.setDataAtualizacao(LocalDateTime.now());
                contaRepository.save(conta);
                log.info("Conta consentida criada: consentId={}, accountId={}",
                    consentimento.getConsentId(), event.getContaId());
            }
        }
    }

    private boolean isExpired(Consentimento c) {
        return c.getDataExpiracao().isBefore(LocalDateTime.now());
    }

    private String mapTipoConta(String tipo) {
        if (tipo == null) return "CONTA_DE_DEPOSITO";
        return switch (tipo.toUpperCase()) {
            case "POUPANCA" -> "CONTA_POUPANCA";
            case "SALARIO" -> "CONTA_SALARIO";
            case "PAGAMENTO" -> "CONTA_DE_PAGAMENTO";
            default -> "CONTA_DE_DEPOSITO";
        };
    }
}
