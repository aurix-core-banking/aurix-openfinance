package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.ContaResponse;
import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContaService {

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);
    private final ContaConsentidaRepository repository;
    private final ConsentimentoService consentimentoService;

    public ContaService(ContaConsentidaRepository repository, ConsentimentoService consentimentoService) {
        this.repository = repository;
        this.consentimentoService = consentimentoService;
    }

    public List<ContaResponse> listarContas(String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        log.info("Listando contas consentidas: {}", consentId);
        return repository.findByConsentId(consentId).stream()
            .map(this::toResponse).toList();
    }

    public List<ContaResponse> listarContasPorConta(String accountId) {
        log.info("Listando contas por conta: {}", accountId);
        return repository.findByAccountId(accountId).stream()
            .map(this::toResponse).toList();
    }

    private ContaResponse toResponse(ContaConsentida c) {
        ContaResponse r = new ContaResponse();
        r.setAccountId(c.getAccountId());
        r.setInstitutionCode(c.getInstitutionCode());
        r.setMoeda(c.getMoeda());
        r.setTipoConta(c.getTipoConta().name());
        r.setStatusConta(c.getStatusConta());
        r.setSaldoDisponivel(c.getSaldoDisponivel());
        r.setSaldoAtual(c.getSaldoAtual());
        r.setDataAtualizacao(c.getDataAtualizacao());
        return r;
    }
}
