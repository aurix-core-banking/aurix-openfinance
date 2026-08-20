package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.CartaoConsentido;
import com.aurix.platform.openfinance.entity.FaturaConsentida;
import com.aurix.platform.openfinance.entity.TransacaoCartaoConsentida;
import com.aurix.platform.openfinance.extractor.adapter.CardSourceAdapter;
import com.aurix.platform.openfinance.repository.CartaoConsentidoRepository;
import com.aurix.platform.openfinance.repository.FaturaConsentidaRepository;
import com.aurix.platform.openfinance.repository.TransacaoCartaoConsentidaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação do {@link CardSourceAdapter} para o core banking "aurix".
 */
@Component
public class AurixCoreCardSourceAdapter implements CardSourceAdapter {

    private final CartaoConsentidoRepository cartaoRepository;
    private final FaturaConsentidaRepository faturaRepository;
    private final TransacaoCartaoConsentidaRepository transacaoRepository;

    public AurixCoreCardSourceAdapter(CartaoConsentidoRepository cartaoRepository,
                                       FaturaConsentidaRepository faturaRepository,
                                       TransacaoCartaoConsentidaRepository transacaoRepository) {
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Override
    public List<Card> findCards(String consentId) {
        return cartaoRepository.findByConsentId(consentId).stream().map(this::toCard).toList();
    }

    @Override
    public List<Bill> findBills(String consentId) {
        return faturaRepository.findByConsentId(consentId).stream().map(this::toBill).toList();
    }

    @Override
    public List<CardTransaction> findCardTransactions(String consentId) {
        return transacaoRepository.findByConsentId(consentId).stream().map(this::toTransaction).toList();
    }

    private Card toCard(CartaoConsentido c) {
        return new Card(
                c.getCartaoId(),
                c.getClienteId(),
                c.getBandeira(),
                c.getFinalNumero(),
                c.getStatusCartao(),
                c.getLimiteCredito(),
                c.getLimiteDisponivel(),
                c.getValorFaturaAtual(),
                c.getDataVencimentoFatura(),
                c.getDataAtualizacao());
    }

    private Bill toBill(FaturaConsentida f) {
        return new Bill(
                f.getCartaoId(),
                f.getFaturaId(),
                f.getValorTotal(),
                f.getValorMinimo(),
                f.getValorPago(),
                f.getDataVencimento(),
                f.getDataPagamento(),
                f.getDataAtualizacao());
    }

    private CardTransaction toTransaction(TransacaoCartaoConsentida t) {
        return new CardTransaction(
                t.getCartaoId(),
                t.getTransactionId(),
                t.getValor(),
                t.getMoeda(),
                t.getEstabelecimento(),
                t.getTipoTransacao(),
                t.getDataTransacao());
    }
}
