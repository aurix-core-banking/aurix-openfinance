package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.PixPaymentInitiationRequest;
import com.aurix.platform.openfinance.dto.PixPaymentInitiationResponse;
import com.aurix.platform.openfinance.dto.PixResponse;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.entity.PixConsentido;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.openfinance.repository.PixConsentidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável por consultas Pix (Fase 2/3) e Iniciação de Pagamentos Pix (Fase 3 Open Finance Brasil).
 * Em conformidade com a invariante regulatória INV-001 (NO_CONSENT_NO_EXECUTION).
 */
@Service
public class PixService {

    private static final Logger log = LoggerFactory.getLogger(PixService.class);
    private static final DateTimeFormatter E2E_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PixConsentidoRepository repo;
    private final ConsentimentoRepository consentimentoRepository;

    public PixService(PixConsentidoRepository repo, ConsentimentoRepository consentimentoRepository) {
        this.repo = repo;
        this.consentimentoRepository = consentimentoRepository;
    }

    public List<PixResponse> listar(String consentId) {
        return repo.findByConsentId(consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<PixResponse> listarPorCliente(String consentId, String clienteId) {
        return repo.findByClienteIdAndConsentId(clienteId, consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Iniciação de Pagamento Pix (Fase 3 Open Finance Brasil).
     * Aplica enforcement da invariante INV-001: nenhum pagamento pode ser executado sem consentimento autorizado.
     */
    @Transactional
    public PixPaymentInitiationResponse iniciarPagamento(PixPaymentInitiationRequest request) {
        if (request.getConsentId() == null || request.getConsentId().isBlank()) {
            throw new IllegalArgumentException("INV-001: consentId é obrigatório para iniciação de pagamento Pix.");
        }

        // Validação de Consentimento — INV-001
        Consentimento consentimento = consentimentoRepository.findByConsentId(request.getConsentId())
                .orElseThrow(() -> new IllegalStateException("INV-001: Consentimento não encontrado: " + request.getConsentId()));

        if (consentimento.getStatus() != Consentimento.StatusConsentimento.AUTHORISED) {
            throw new IllegalStateException("INV-001: Pagamento Pix rejeitado. O consentimento " + request.getConsentId() +
                    " está em estado " + consentimento.getStatus() + " e não AUTHORISED.");
        }

        if (consentimento.getDataExpiracao() != null && consentimento.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("INV-001: Pagamento Pix rejeitado. O consentimento expirou em " + consentimento.getDataExpiracao());
        }

        String pixId = "PAY-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String endToEndId = request.getEndToEndId();
        if (endToEndId == null || endToEndId.isBlank()) {
            endToEndId = "E" + "00000000" + LocalDateTime.now().format(E2E_DATE_FORMAT) + UUID.randomUUID().toString().substring(0, 8);
        }

        PixConsentido p = new PixConsentido();
        p.setConsentId(request.getConsentId());
        p.setPixId(pixId);
        p.setClienteId(request.getClienteId() != null ? request.getClienteId() : String.valueOf(consentimento.getUserId()));
        p.setTipoPix("PAGAMENTO_INICIADO");
        p.setChavedePix(request.getChavePix());
        p.setTipoChave(request.getTipoChave() != null ? request.getTipoChave() : "ALEATORIA");
        p.setValor(request.getValor());
        p.setMoeda(request.getMoeda() != null ? request.getMoeda() : "BRL");
        p.setDescricao(request.getDescricao() != null ? request.getDescricao() : "Pix via Open Finance Aurix");
        p.setStatusPix("ACSC"); // Liquidado com sucesso no SPI
        p.setDataPix(LocalDateTime.now());
        p.setDataAtualizacao(LocalDateTime.now());

        repo.save(p);
        log.info("Iniciação de pagamento Pix realizada com sucesso: pixId={}, e2eId={}, consentId={}", pixId, endToEndId, request.getConsentId());

        PixPaymentInitiationResponse response = new PixPaymentInitiationResponse();
        response.setPaymentId(pixId);
        response.setConsentId(request.getConsentId());
        response.setEndToEndId(endToEndId);
        response.setStatus("ACSC");
        response.setValor(p.getValor());
        response.setMoeda(p.getMoeda());
        response.setChavePix(p.getChavedePix());
        response.setDescricao(p.getDescricao());
        response.setDataCriacao(p.getDataPix());
        response.setDataLiquidacao(p.getDataAtualizacao());
        return response;
    }

    /**
     * Consulta status de pagamento Pix iniciado pelo ID.
     */
    public Optional<PixPaymentInitiationResponse> buscarPagamento(String paymentId) {
        return repo.findByPixId(paymentId).map(p -> {
            PixPaymentInitiationResponse response = new PixPaymentInitiationResponse();
            response.setPaymentId(p.getPixId());
            response.setConsentId(p.getConsentId());
            response.setEndToEndId("E" + p.getPixId());
            response.setStatus(p.getStatusPix());
            response.setValor(p.getValor());
            response.setMoeda(p.getMoeda());
            response.setChavePix(p.getChavedePix());
            response.setDescricao(p.getDescricao());
            response.setDataCriacao(p.getDataPix());
            response.setDataLiquidacao(p.getDataAtualizacao());
            return response;
        });
    }

    @Transactional
    public void sincronizar(String consentId, String pixId, String clienteId,
                            String tipoPix, String chavePix, String tipoChave,
                            BigDecimal valor, String moeda, String descricao,
                            String statusPix, LocalDateTime dataPix) {
        Optional<PixConsentido> existing = repo.findByConsentId(consentId).stream()
                .filter(p -> p.getPixId().equals(pixId)).findFirst();
        if (existing.isPresent()) return;

        PixConsentido p = new PixConsentido();
        p.setConsentId(consentId);
        p.setPixId(pixId);
        p.setClienteId(clienteId);
        p.setTipoPix(tipoPix);
        p.setChavedePix(chavePix);
        p.setTipoChave(tipoChave);
        p.setValor(valor);
        p.setMoeda(moeda);
        p.setDescricao(descricao);
        p.setStatusPix(statusPix);
        p.setDataPix(dataPix);
        p.setDataAtualizacao(LocalDateTime.now());
        repo.save(p);
    }

    private PixResponse toResponse(PixConsentido p) {
        PixResponse r = new PixResponse();
        r.setPixId(p.getPixId());
        r.setTipoPix(p.getTipoPix());
        r.setChavedePix(p.getChavedePix());
        r.setTipoChave(p.getTipoChave());
        r.setValor(p.getValor());
        r.setMoeda(p.getMoeda());
        r.setDescricao(p.getDescricao());
        r.setStatusPix(p.getStatusPix());
        r.setDataPix(p.getDataPix());
        return r;
    }
}
