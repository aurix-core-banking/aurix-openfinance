package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.TransacaoConsentida;
import com.aurix.platform.openfinance.extractor.adapter.AccountSourceAdapter;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação do {@link AccountSourceAdapter} para o core banking "aurix"
 * (tabelas *_consentida já materializadas via listeners Kafka). Um core diferente
 * plugaria trocando este bean por outro — nada no extractor mudaria.
 */
@Component
public class AurixCoreAccountSourceAdapter implements AccountSourceAdapter {

    private final ContaConsentidaRepository contaRepository;
    private final TransacaoConsentidaRepository transacaoRepository;

    public AurixCoreAccountSourceAdapter(ContaConsentidaRepository contaRepository,
                                          TransacaoConsentidaRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Override
    public List<Account> findAccounts(String consentId) {
        return contaRepository.findByConsentId(consentId).stream()
                .map(this::toAccount)
                .toList();
    }

    @Override
    public List<Transaction> findTransactions(String consentId) {
        return transacaoRepository.findByConsentId(consentId).stream()
                .map(this::toTransaction)
                .toList();
    }

    private Account toAccount(ContaConsentida c) {
        return new Account(
                c.getAccountId(),
                c.getInstitutionCode(),
                c.getMoeda(),
                c.getTipoConta() != null ? c.getTipoConta().name() : null,
                c.getStatusConta(),
                c.getSaldoDisponivel(),
                c.getSaldoAtual(),
                c.getDataAtualizacao());
    }

    private Transaction toTransaction(TransacaoConsentida t) {
        return new Transaction(
                t.getAccountId(),
                t.getTransactionId(),
                t.getTipoTransacao() != null ? t.getTipoTransacao().name() : null,
                t.getValor(),
                t.getMoeda(),
                t.getEstabelecimento(),
                t.getDescricao(),
                t.getDataTransacao(),
                t.getDataProcessamento());
    }
}
