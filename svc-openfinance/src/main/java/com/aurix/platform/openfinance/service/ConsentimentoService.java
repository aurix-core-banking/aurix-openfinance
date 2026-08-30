package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.ConsentimentoVersao;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoVersaoRepository;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.TransacaoConsentidaRepository;
import com.aurix.platform.openfinance.repository.PessoaConsentidaRepository;
import com.aurix.platform.openfinance.repository.CartaoConsentidoRepository;
import com.aurix.platform.openfinance.repository.FaturaConsentidaRepository;
import com.aurix.platform.openfinance.repository.TransacaoCartaoConsentidaRepository;
import com.aurix.platform.openfinance.repository.EmprestimoConsentidoRepository;
import com.aurix.platform.openfinance.repository.SeguroConsentidoRepository;
import com.aurix.platform.openfinance.repository.PixConsentidoRepository;
import com.aurix.platform.openfinance.event.OpenFinanceEventPublisher;
import com.aurix.platform.openfinance.trust.receipt.service.ConsentReceiptService;
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
    private final ConsentimentoVersaoRepository versaoRepository;
    private final ContaConsentidaRepository contaRepository;
    private final TransacaoConsentidaRepository transacaoRepository;
    private final PessoaConsentidaRepository pessoaRepository;
    private final CartaoConsentidoRepository cartaoRepository;
    private final FaturaConsentidaRepository faturaRepository;
    private final TransacaoCartaoConsentidaRepository txCartaoRepository;
    private final EmprestimoConsentidoRepository emprestimoRepository;
    private final SeguroConsentidoRepository seguroRepository;
    private final PixConsentidoRepository pixRepository;
    private final OpenFinanceEventPublisher eventPublisher;
    private final ConsentReceiptService consentReceiptService;

    @Value("${aurix.openfinance.consent-max-duration-days:365}")
    private int maxDurationDays;

    public ConsentimentoService(ConsentimentoRepository repository,
                                ConsentimentoVersaoRepository versaoRepository,
                                ContaConsentidaRepository contaRepository,
                                TransacaoConsentidaRepository transacaoRepository,
                                PessoaConsentidaRepository pessoaRepository,
                                CartaoConsentidoRepository cartaoRepository,
                                FaturaConsentidaRepository faturaRepository,
                                TransacaoCartaoConsentidaRepository txCartaoRepository,
                                EmprestimoConsentidoRepository emprestimoRepository,
                                SeguroConsentidoRepository seguroRepository,
                                PixConsentidoRepository pixRepository,
                                OpenFinanceEventPublisher eventPublisher,
                                ConsentReceiptService consentReceiptService) {
        this.repository = repository;
        this.versaoRepository = versaoRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.txCartaoRepository = txCartaoRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.seguroRepository = seguroRepository;
        this.pixRepository = pixRepository;
        this.eventPublisher = eventPublisher;
        this.consentReceiptService = consentReceiptService;
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
        registrarVersao(c.getConsentId(), c.getVersion(),
                Consentimento.StatusConsentimento.AWAITING_AUTHORISATION, null, null);
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
        validarTransicao(c.getStatus(), Consentimento.StatusConsentimento.AUTHORISED);
        c.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        c.setDataAprovacao(LocalDateTime.now());
        int novaVersao = c.getVersion() + 1;
        c.setVersion(novaVersao);
        c = repository.save(c);

        registrarVersao(consentId, novaVersao,
                Consentimento.StatusConsentimento.AUTHORISED, c.getDataAprovacao(), null);
        log.info("Consentimento aprovado: {}", consentId);

        // Formação do recibo de consentimento (não-repúdio — issue #1 / WP2)
        consentReceiptService.gerarRecibo(consentId, c.getClientId(),
                c.getPermissions(), "dados-compartilhamento", c.getDataExpiracao());

        eventPublisher.publishConsentGranted(consentId, c.getClientId());
        return toResponse(c);
    }

    public ConsentimentoResponse rejeitar(String consentId, String motivo) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        validarTransicao(c.getStatus(), Consentimento.StatusConsentimento.REJECTED);
        c.setStatus(Consentimento.StatusConsentimento.REJECTED);
        c.setMotivoRejeicao(motivo);
        int novaVersao = c.getVersion() + 1;
        c.setVersion(novaVersao);
        c = repository.save(c);
        registrarVersao(consentId, novaVersao,
                Consentimento.StatusConsentimento.REJECTED, null, motivo);
        log.info("Consentimento rejeitado: {} - {}", consentId, motivo);
        return toResponse(c);
    }

    public ConsentimentoResponse revogar(String consentId, String motivo) {
        Consentimento c = repository.findByConsentId(consentId)
            .orElseThrow(() -> new IllegalArgumentException("Consentimento não encontrado: " + consentId));
        validarTransicao(c.getStatus(), Consentimento.StatusConsentimento.REVOKED);
        c.setStatus(Consentimento.StatusConsentimento.REVOKED);
        c.setMotivoRejeicao(motivo);
        int novaVersao = c.getVersion() + 1;
        c.setVersion(novaVersao);
        c = repository.save(c);
        registrarVersao(consentId, novaVersao,
                Consentimento.StatusConsentimento.REVOKED, null, motivo);
        log.info("Consentimento revogado: {} - {}", consentId, motivo);
        eventPublisher.publishConsentRevoked(consentId, motivo);
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
                validarTransicao(c.getStatus(), Consentimento.StatusConsentimento.EXPIRED);
                c.setStatus(Consentimento.StatusConsentimento.EXPIRED);
                int novaVersao = c.getVersion() + 1;
                c.setVersion(novaVersao);
                repository.save(c);
                registrarVersao(c.getConsentId(), novaVersao,
                        Consentimento.StatusConsentimento.EXPIRED, null, "Consentimento expirado");
                limparDadosExpirados(c.getConsentId());
                eventPublisher.publishConsentExpired(c.getConsentId());
                count++;
            }
        }
        if (count > 0) {
            log.info("Consentimentos expirados processados: {}", count);
        }
    }

    /**
     * INV-006: valida a transição de estado do consentimento. Transições inválidas
     * (mutações do histórico) são rejeitadas — ex.: rejeitar/reaprovar um
     * consentimento já aprovado.
     */
    private void validarTransicao(Consentimento.StatusConsentimento atual,
                                  Consentimento.StatusConsentimento novo) {
        boolean valida = switch (atual) {
            case AWAITING_AUTHORISATION -> List.of(
                    Consentimento.StatusConsentimento.AUTHORISED,
                    Consentimento.StatusConsentimento.REJECTED,
                    Consentimento.StatusConsentimento.REVOKED,
                    Consentimento.StatusConsentimento.EXPIRED).contains(novo);
            case AUTHORISED -> List.of(
                    Consentimento.StatusConsentimento.REVOKED,
                    Consentimento.StatusConsentimento.EXPIRED).contains(novo);
            default -> false;
        };
        if (!valida) {
            throw new IllegalStateException("Mutação de consentimento inválida: "
                    + atual + " -> " + novo + " (INV-006)");
        }
    }

    /**
     * INV-006: registra uma versão imutável (append-only) no histórico do
     * consentimento. Linhas de versões anteriores nunca são alteradas.
     */
    private void registrarVersao(String consentId, int versao,
                                 Consentimento.StatusConsentimento status,
                                 LocalDateTime dataAprovacao, String motivo) {
        ConsentimentoVersao v = new ConsentimentoVersao();
        v.setConsentId(consentId);
        v.setVersao(versao);
        v.setStatus(status);
        v.setDataEvento(LocalDateTime.now());
        v.setDataAprovacao(dataAprovacao);
        v.setMotivo(motivo);
        versaoRepository.save(v);
        log.debug("Versão {} do consentimento {} registrada: {}", versao, consentId, status);
    }

    private void limparDadosExpirados(String consentId) {
        log.info("Limpando dados expirados: consentId={}", consentId);
        transacaoRepository.findByConsentId(consentId)
            .forEach(t -> transacaoRepository.deleteById(t.getId()));
        contaRepository.findByConsentId(consentId)
            .forEach(c -> contaRepository.deleteById(c.getId()));
        pessoaRepository.findByConsentId(consentId)
            .forEach(p -> pessoaRepository.deleteById(p.getId()));
        cartaoRepository.findByConsentId(consentId)
            .forEach(c -> cartaoRepository.deleteById(c.getId()));
        faturaRepository.findByConsentId(consentId)
            .forEach(f -> faturaRepository.deleteById(f.getId()));
        txCartaoRepository.findByConsentId(consentId)
            .forEach(t -> txCartaoRepository.deleteById(t.getId()));
        emprestimoRepository.findByConsentId(consentId)
            .forEach(e -> emprestimoRepository.deleteById(e.getId()));
        seguroRepository.findByConsentId(consentId)
            .forEach(s -> seguroRepository.deleteById(s.getId()));
        pixRepository.findByConsentId(consentId)
            .forEach(p -> pixRepository.deleteById(p.getId()));
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
