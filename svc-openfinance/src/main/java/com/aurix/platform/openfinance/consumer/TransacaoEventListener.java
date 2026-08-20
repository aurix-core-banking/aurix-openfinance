package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.entity.TransacaoConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.shared.event.TransacaoEvent;
import com.aurix.platform.shared.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TransacaoEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransacaoEventListener.class);
    private final TransacaoConsentidaRepository transacaoRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public TransacaoEventListener(TransacaoConsentidaRepository transacaoRepository,
                                  ConsentimentoRepository consentimentoRepository) {
        this.transacaoRepository = transacaoRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    @KafkaListener(
        topics = {Topics.TRANSACAO_REALIZADA, Topics.TRANSACAO_LIQUIDADA},
        groupId = "aurix-openfinance-transacao-group"
    )
    public void onTransacaoEvent(TransacaoEvent event) {
        log.info("Evento Open Finance recebido: {} — Transação={}, Conta={}",
            event.getEventType(), event.getTransacaoId(), event.getContaId());

        List<Consentimento> consentimentosAtivos = consentimentoRepository
            .findByStatus(Consentimento.StatusConsentimento.AUTHORISED);

        for (Consentimento consentimento : consentimentosAtivos) {
            if (isExpired(consentimento)) continue;

            boolean jaExiste = transacaoRepository
                .findByAccountIdAndConsentId(event.getContaId(), consentimento.getConsentId())
                .stream()
                .anyMatch(t -> t.getTransactionId().equals(event.getTransacaoId()));

            if (!jaExiste) {
                TransacaoConsentida transacao = new TransacaoConsentida();
                transacao.setConsentId(consentimento.getConsentId());
                transacao.setAccountId(event.getContaId());
                transacao.setTransactionId(event.getTransacaoId());
                transacao.setTipoTransacao(mapTipoTransacao(event.getTipoTransacao()));
                transacao.setValor(event.getValor());
                transacao.setMoeda("BRL");
                transacao.setEstabelecimento(event.getDescricao() != null ? event.getDescricao() : "TRANSACAO");
                transacao.setDescricao(event.getDescricao());
                transacao.setDataTransacao(LocalDateTime.now());
                transacao.setDataProcessamento(LocalDateTime.now());
                transacaoRepository.save(transacao);
                log.info("Transação consentida criada: consentId={}, transacaoId={}",
                    consentimento.getConsentId(), event.getTransacaoId());
            }
        }
    }

    private boolean isExpired(Consentimento c) {
        return c.getDataExpiracao().isBefore(LocalDateTime.now());
    }

    private TransacaoConsentida.TipoTransacao mapTipoTransacao(String tipo) {
        if (tipo == null) return TransacaoConsentida.TipoTransacao.DEBITO;
        return switch (tipo.toUpperCase()) {
            case "CREDITO", "CREDIT" -> TransacaoConsentida.TipoTransacao.CREDITO;
            default -> TransacaoConsentida.TipoTransacao.DEBITO;
        };
    }
}
