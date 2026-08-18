package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.PixResponse;
import com.aurix.platform.openfinance.entity.PixConsentido;
import com.aurix.platform.openfinance.repository.PixConsentidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PixService {

    private final PixConsentidoRepository repo;

    public PixService(PixConsentidoRepository repo) { this.repo = repo; }

    public List<PixResponse> listar(String consentId) {
        return repo.findByConsentId(consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<PixResponse> listarPorCliente(String consentId, String clienteId) {
        return repo.findByClienteIdAndConsentId(clienteId, consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
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
