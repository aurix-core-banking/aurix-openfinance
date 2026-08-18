package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConsentimentoService {

    private static final Logger log = LoggerFactory.getLogger(ConsentimentoService.class);
    private final ConsentimentoRepository repository;
    private final ContaConsentidaRepository contaRepository;
    private final TransacaoConsentidaRepository transacaoRepository;

    @Value("${aurix.openfinance.consent-max-duration-days:365}")
    private int maxDurationDays;

    public ConsentimentoService(ConsentimentoRepository repository,
                                ContaConsentidaRepository contaRepository,
                                TransacaoConsentidaRepository transacaoRepository) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public ConsentimentoResponse criar(ConsentimentoRequest request, Long userId) {
        Consentimento c = new Consentimento();
        c.setConsentId(UUID.randomUUID().toString());
        c.setClientId(request.getClientId());
        c.setUserId(userId);
        c.setPermissions(request.getPermissions() != null ? String.join(",", request.getPermissions()) : "accounts");
        c.setDataCriacao(LocalDateTime.now());
        int days = request.getExpirationDays() != null ? Math.min(request.getExpirationDays(), maxDurationDays) : maxDurationDays;
        c.setDataExpiracao(LocalDateTime.now().plusDays(days));
        c = repository.save(c);
        log.info("Consentimento criado: {}", c.getConsentId());
        return toResponse(c);
    }

    public ConsentimentoResponse buscar(String consentId) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        return toResponse(c);
    }

    public ConsentimentoResponse aprovar(String consentId) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        c.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        c.setDataAprovacao(LocalDateTime.now());
        c.setVersion(c.getVersion() + 1);
        c = repository.save(c);
        log.info("Consentimento aprovado: {}", consentId);
        return toResponse(c);
    }

    public ConsentimentoResponse rejeitar(String consentId, String motivo) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        c.setStatus(Consentimento.StatusConsentimento.REJECTED);
        c.setMotivoRejeicao(motivo);
        c.setVersion(c.getVersion() + 1);
        c = repository.save(c);
        log.info("Consentimento rejeitado: {} - {}", consentId, motivo);
        return toResponse(c);
    }

    public ConsentimentoResponse revogar(String consentId, String motivo) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        c.setStatus(Consentimento.StatusConsentimento.REVOKED);
        c.setMotivoRejeicao(motivo);
        c.setVersion(c.getVersion() + 1);
        c = repository.save(c);
        log.info("Consentimento revogado: {} - {}", consentId, motivo);
        return toResponse(c);
    }

    public boolean verificarConsentimentoAtivo(String consentId) {
        return repository.findByConsentId(consentId)
            .filter(c -> c.getStatus() == Consentimento.StatusConsentimento.AUTHORISED)
            .filter(c -> c.getDataExpiracao().isAfter(LocalDateTime.now()))
            .isPresent();
    }

    @Scheduled(fixedRate = 3600000)
    public void processarExpirados() {
        List<Consentimento> expirados = repository.findByStatus(Consentimento.StatusConsentimento.AUTHORISED);
        long count = 0;
        for (Consentimento c : expirados) {
            if (c.getDataExpiracao().isBefore(LocalDateTime.now())) {
                c.setStatus(Consentimento.StatusConsentimento.EXPIRED);
                c.setVersion(c.getVersion() + 1);
                repository.save(c);
                limparDadosExpirados(c.getConsentId());
                count++;
            }
        }
        if (count > 0) {
            log.info("Consentimentos expirados processados: {}", count);
        }
    }

    private void limparDadosExpirados(String consentId) {
        long contas = contaRepository.findByConsentId(consentId).size();
        long transacoes = transacaoRepository.findByConsentId(consentId).size();
        log.info("Limpando dados expirados: consentId={}, contas={}, transações={}",
            consentId, contas, transacoes);
        transacaoRepository.findByConsentId(consentId)
            .forEach(t -> transacaoRepository.deleteById(t.getId()));
        contaRepository.findByConsentId(consentId)
            .forEach(c -> contaRepository.deleteById(c.getId()));
    }

    private ConsentimentoResponse toResponse(Consentimento c) {
        ConsentimentoResponse r = new ConsentimentoResponse();
        r.setConsentId(c.getConsentId());
        r.setClientId(c.getClientId());
        r.setStatus(c.getStatus().name());
        r.setPermissions(List.of(c.getPermissions().split(",")));
        r.setDataCriacao(c.getDataCriacao());
        r.setDataExpiracao(c.getDataExpiracao());
        r.setVersion(c.getVersion());
        return r;
    }
}
