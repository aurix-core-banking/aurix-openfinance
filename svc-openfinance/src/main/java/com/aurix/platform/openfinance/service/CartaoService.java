package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.*;
import com.aurix.platform.openfinance.entity.*;
import com.aurix.platform.openfinance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartaoService {

    private final CartaoConsentidoRepository cartaoRepo;
    private final FaturaConsentidaRepository faturaRepo;
    private final TransacaoCartaoConsentidaRepository txRepo;
    private final ConsentimentoRepository consentimentoRepo;

    public CartaoService(CartaoConsentidoRepository cartaoRepo,
                         FaturaConsentidaRepository faturaRepo,
                         TransacaoCartaoConsentidaRepository txRepo,
                         ConsentimentoRepository consentimentoRepo) {
        this.cartaoRepo = cartaoRepo;
        this.faturaRepo = faturaRepo;
        this.txRepo = txRepo;
        this.consentimentoRepo = consentimentoRepo;
    }

    public List<CartaoResponse> listarCartoes(String consentId) {
        return cartaoRepo.findByConsentId(consentId).stream()
                .map(this::toCartaoResponse).collect(Collectors.toList());
    }

    public Optional<CartaoResponse> buscarCartao(String consentId, String cartaoId) {
        return cartaoRepo.findByConsentId(consentId).stream()
                .filter(c -> c.getCartaoId().equals(cartaoId))
                .map(this::toCartaoResponse).findFirst();
    }

    public List<FaturaResponse> listarFaturas(String consentId, String cartaoId) {
        return faturaRepo.findByCartaoIdAndConsentId(cartaoId, consentId).stream()
                .map(this::toFaturaResponse).collect(Collectors.toList());
    }

    public Optional<FaturaResponse> buscarFatura(String consentId, String cartaoId, String faturaId) {
        return faturaRepo.findByCartaoIdAndConsentId(cartaoId, consentId).stream()
                .filter(f -> f.getFaturaId().equals(faturaId))
                .map(this::toFaturaResponse).findFirst();
    }

    public List<TransacaoCartaoResponse> listarTransacoesCartao(String consentId, String cartaoId) {
        return txRepo.findByCartaoIdAndConsentId(cartaoId, consentId).stream()
                .map(this::toTransacaoCartaoResponse).collect(Collectors.toList());
    }

    @Transactional
    public void sincronizarCartao(String consentId, String cartaoId, String bandeira,
                                  String finalNumero, String statusCartao,
                                  BigDecimal limiteCredito, BigDecimal limiteDisponivel,
                                  BigDecimal valorFaturaAtual, java.time.LocalDate dataVencimento) {
        CartaoConsentido cartao = cartaoRepo.findByCartaoId(cartaoId).stream()
                .filter(c -> c.getConsentId().equals(consentId))
                .findFirst().orElseGet(CartaoConsentido::new);
        cartao.setConsentId(consentId);
        cartao.setCartaoId(cartaoId);
        cartao.setBandeira(bandeira);
        cartao.setFinalNumero(finalNumero);
        cartao.setStatusCartao(statusCartao);
        cartao.setLimiteCredito(limiteCredito);
        cartao.setLimiteDisponivel(limiteDisponivel);
        cartao.setValorFaturaAtual(valorFaturaAtual);
        cartao.setDataVencimentoFatura(dataVencimento);
        cartao.setDataAtualizacao(LocalDateTime.now());
        cartaoRepo.save(cartao);
    }

    @Transactional
    public void sincronizarFatura(String consentId, String cartaoId, String faturaId,
                                  BigDecimal valorTotal, BigDecimal valorMinimo,
                                  BigDecimal valorPago, java.time.LocalDate dataVencimento,
                                  java.time.LocalDate dataPagamento) {
        FaturaConsentida fatura = faturaRepo.findByCartaoIdAndConsentId(cartaoId, consentId).stream()
                .filter(f -> f.getFaturaId().equals(faturaId))
                .findFirst().orElseGet(FaturaConsentida::new);
        fatura.setConsentId(consentId);
        fatura.setCartaoId(cartaoId);
        fatura.setFaturaId(faturaId);
        fatura.setValorTotal(valorTotal);
        fatura.setValorMinimo(valorMinimo);
        fatura.setValorPago(valorPago);
        fatura.setDataVencimento(dataVencimento);
        fatura.setDataPagamento(dataPagamento);
        fatura.setDataAtualizacao(LocalDateTime.now());
        faturaRepo.save(fatura);
    }

    @Transactional
    public void sincronizarTransacao(String consentId, String cartaoId, String transactionId,
                                     BigDecimal valor, String moeda, String estabelecimento,
                                     String tipoTransacao, LocalDateTime dataTransacao) {
        Optional<TransacaoCartaoConsentida> existing = txRepo.findByCartaoIdAndConsentId(cartaoId, consentId)
                .stream().filter(t -> t.getTransactionId().equals(transactionId)).findFirst();
        if (existing.isPresent()) return;

        TransacaoCartaoConsentida tx = new TransacaoCartaoConsentida();
        tx.setConsentId(consentId);
        tx.setCartaoId(cartaoId);
        tx.setTransactionId(transactionId);
        tx.setValor(valor);
        tx.setMoeda(moeda);
        tx.setEstabelecimento(estabelecimento);
        tx.setTipoTransacao(tipoTransacao);
        tx.setDataTransacao(dataTransacao);
        txRepo.save(tx);
    }

    private CartaoResponse toCartaoResponse(CartaoConsentido c) {
        CartaoResponse r = new CartaoResponse();
        r.setCartaoId(c.getCartaoId());
        r.setBandeira(c.getBandeira());
        r.setFinalNumero(c.getFinalNumero());
        r.setStatusCartao(c.getStatusCartao());
        r.setLimiteCredito(c.getLimiteCredito());
        r.setLimiteDisponivel(c.getLimiteDisponivel());
        r.setValorFaturaAtual(c.getValorFaturaAtual());
        r.setDataVencimentoFatura(c.getDataVencimentoFatura());
        return r;
    }

    private FaturaResponse toFaturaResponse(FaturaConsentida f) {
        FaturaResponse r = new FaturaResponse();
        r.setFaturaId(f.getFaturaId());
        r.setValorTotal(f.getValorTotal());
        r.setValorMinimo(f.getValorMinimo());
        r.setValorPago(f.getValorPago());
        r.setDataVencimento(f.getDataVencimento());
        r.setDataPagamento(f.getDataPagamento());
        return r;
    }

    private TransacaoCartaoResponse toTransacaoCartaoResponse(TransacaoCartaoConsentida t) {
        TransacaoCartaoResponse r = new TransacaoCartaoResponse();
        r.setTransactionId(t.getTransactionId());
        r.setValor(t.getValor());
        r.setMoeda(t.getMoeda());
        r.setEstabelecimento(t.getEstabelecimento());
        r.setTipoTransacao(t.getTipoTransacao());
        r.setDataTransacao(t.getDataTransacao());
        return r;
    }
}
