package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.TransacaoResponse;
import com.aurix.platform.openfinance.entity.TransacaoConsentida;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransacaoService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoService.class);
    private final TransacaoConsentidaRepository repository;
    private final ConsentimentoService consentimentoService;

    public TransacaoService(TransacaoConsentidaRepository repository, ConsentimentoService consentimentoService) {
        this.repository = repository;
        this.consentimentoService = consentimentoService;
    }

    public List<TransacaoResponse> listarTransacoes(String accountId, String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        log.info("Listando transações da conta {} com consentimento {}", accountId, consentId);
        return repository.findByAccountIdAndConsentId(accountId, consentId).stream()
            .map(this::toResponse).toList();
    }

    private TransacaoResponse toResponse(TransacaoConsentida t) {
        TransacaoResponse r = new TransacaoResponse();
        r.setTransactionId(t.getTransactionId());
        r.setTipoTransacao(t.getTipoTransacao().name());
        r.setValor(t.getValor());
        r.setMoeda(t.getMoeda());
        r.setEstabelecimento(t.getEstabelecimento());
        r.setDescricao(t.getDescricao());
        r.setDataTransacao(t.getDataTransacao());
        r.setDataProcessamento(t.getDataProcessamento());
        return r;
    }
}
