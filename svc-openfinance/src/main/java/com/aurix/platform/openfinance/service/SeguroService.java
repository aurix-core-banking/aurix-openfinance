package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.SeguroResponse;
import com.aurix.platform.openfinance.entity.SeguroConsentido;
import com.aurix.platform.openfinance.repository.SeguroConsentidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeguroService {

    private final SeguroConsentidoRepository repo;

    public SeguroService(SeguroConsentidoRepository repo) { this.repo = repo; }

    public List<SeguroResponse> listar(String consentId) {
        return repo.findByConsentId(consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<SeguroResponse> buscar(String consentId, String apoliceId) {
        return repo.findByConsentId(consentId).stream()
                .filter(s -> s.getApoliceId().equals(apoliceId))
                .map(this::toResponse).findFirst();
    }

    @Transactional
    public void sincronizar(String consentId, String apoliceId, String clienteId,
                            String tipoSeguro, String nomeSeguradora,
                            BigDecimal premioMensal, BigDecimal premioTotal,
                            BigDecimal valorSegurado, LocalDate dataInicio,
                            LocalDate dataFim, String statusApolice) {
        SeguroConsentido s = repo.findByApoliceId(apoliceId).stream()
                .filter(x -> x.getConsentId().equals(consentId))
                .findFirst().orElseGet(SeguroConsentido::new);
        s.setConsentId(consentId);
        s.setApoliceId(apoliceId);
        s.setClienteId(clienteId);
        s.setTipoSeguro(tipoSeguro);
        s.setNomeSeguradora(nomeSeguradora);
        s.setPremioMensal(premioMensal);
        s.setPremioTotal(premioTotal);
        s.setValorSegurado(valorSegurado);
        s.setDataInicio(dataInicio);
        s.setDataFim(dataFim);
        s.setStatusApolice(statusApolice);
        s.setDataAtualizacao(LocalDateTime.now());
        repo.save(s);
    }

    private SeguroResponse toResponse(SeguroConsentido s) {
        SeguroResponse r = new SeguroResponse();
        r.setApoliceId(s.getApoliceId());
        r.setTipoSeguro(s.getTipoSeguro());
        r.setNomeSeguradora(s.getNomeSeguradora());
        r.setPremioMensal(s.getPremioMensal());
        r.setPremioTotal(s.getPremioTotal());
        r.setValorSegurado(s.getValorSegurado());
        r.setDataInicio(s.getDataInicio());
        r.setDataFim(s.getDataFim());
        r.setStatusApolice(s.getStatusApolice());
        return r;
    }
}
