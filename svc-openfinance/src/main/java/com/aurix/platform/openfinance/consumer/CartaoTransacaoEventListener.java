package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.entity.TransacaoConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.shared.event.CartaoTransacaoAutorizadaEvent;
import com.aurix.platform.shared.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CartaoTransacaoEventListener {

    private static final Logger log = LoggerFactory.getLogger(CartaoTransacaoEventListener.class);
    private final TransacaoConsentidaRepository transacaoRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public CartaoTransacaoEventListener(TransacaoConsentidaRepository transacaoRepository,
                                        ConsentimentoRepository consentimentoRepository) {
        this.transacaoRepository = transacaoRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    @KafkaListener(
        topics = {Topics.CARTOES_TRANSACAO_AUTORIZADA},
        groupId = "aurix-openfinance-cartao-group"
    )
    public void onCartaoTransacao(CartaoTransacaoAutorizadaEvent event) {
        log.info("Evento Open Finance recebido: CARTAO_TRANSACAO_AUTORIZADA — Código={}, Cartão={}",
            event.getCodigoTransacao(), event.getCartaoId());

        List<Consentimento> consentimentosAtivos = consentimentoRepository
            .findByStatus(Consentimento.StatusConsentimento.AUTHORISED);

        for (Consentimento consentimento : consentimentosAtivos) {
            if (isExpired(consentimento)) continue;

            boolean jaExiste = transacaoRepository
                .findByConsentId(consentimento.getConsentId())
                .stream()
                .anyMatch(t -> t.getTransactionId().equals(event.getCodigoTransacao()));

            if (!jaExiste) {
                TransacaoConsentida transacao = new TransacaoConsentida();
                transacao.setConsentId(consentimento.getConsentId());
                transacao.setAccountId("CARTAO-" + event.getCartaoId());
                transacao.setTransactionId(event.getCodigoTransacao());
                transacao.setTipoTransacao(TransacaoConsentida.TipoTransacao.DEBITO);
                transacao.setValor(event.getValor());
                transacao.setMoeda("BRL");
                transacao.setEstabelecimento(
                    event.getEstabelecimento() != null ? event.getEstabelecimento() : "CARTAO");
                transacao.setDescricao("Transação cartão: " + event.getAutorizacao());
                transacao.setDataTransacao(LocalDateTime.now());
                transacao.setDataProcessamento(LocalDateTime.now());
                transacaoRepository.save(transacao);
                log.info("Transação cartão consentida criada: consentId={}, código={}",
                    consentimento.getConsentId(), event.getCodigoTransacao());
            }
        }
    }

    private boolean isExpired(Consentimento c) {
        return c.getDataExpiracao().isBefore(LocalDateTime.now());
    }
}
