package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.EmprestimoResponse;
import com.aurix.platform.openfinance.entity.EmprestimoConsentido;
import com.aurix.platform.openfinance.repository.EmprestimoConsentidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmprestimoService {

    private final EmprestimoConsentidoRepository repo;

    public EmprestimoService(EmprestimoConsentidoRepository repo) { this.repo = repo; }

    public List<EmprestimoResponse> listar(String consentId) {
        return repo.findByConsentId(consentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<EmprestimoResponse> buscar(String consentId, String emprestimoId) {
        return repo.findByConsentId(consentId).stream()
                .filter(e -> e.getEmprestimoId().equals(emprestimoId))
                .map(this::toResponse).findFirst();
    }

    @Transactional
    public void sincronizar(String consentId, String emprestimoId, String clienteId,
                            String tipoEmprestimo, BigDecimal valorContratado,
                            BigDecimal valorSaldoDevedor, BigDecimal taxaJuros,
                            Integer prazoMeses, Integer parcelasPagas,
                            Integer parcelasRestantes, BigDecimal valorParcela,
                            LocalDate dataContratacao, LocalDate dataVencimentoPrimeiraParcela,
                            String statusEmprestimo) {
        EmprestimoConsentido e = repo.findByEmprestimoId(emprestimoId).stream()
                .filter(x -> x.getConsentId().equals(consentId))
                .findFirst().orElseGet(EmprestimoConsentido::new);
        e.setConsentId(consentId);
        e.setEmprestimoId(emprestimoId);
        e.setClienteId(clienteId);
        e.setTipoEmprestimo(tipoEmprestimo);
        e.setValorContratado(valorContratado);
        e.setValorSaldoDevedor(valorSaldoDevedor);
        e.setTaxaJuros(taxaJuros);
        e.setPrazoMeses(prazoMeses);
        e.setParcelasPagas(parcelasPagas);
        e.setParcelasRestantes(parcelasRestantes);
        e.setValorParcela(valorParcela);
        e.setDataContratacao(dataContratacao);
        e.setDataVencimentoPrimeiraParcela(dataVencimentoPrimeiraParcela);
        e.setStatusEmprestimo(statusEmprestimo);
        e.setDataAtualizacao(LocalDateTime.now());
        repo.save(e);
    }

    private EmprestimoResponse toResponse(EmprestimoConsentido e) {
        EmprestimoResponse r = new EmprestimoResponse();
        r.setEmprestimoId(e.getEmprestimoId());
        r.setTipoEmprestimo(e.getTipoEmprestimo());
        r.setValorContratado(e.getValorContratado());
        r.setValorSaldoDevedor(e.getValorSaldoDevedor());
        r.setTaxaJuros(e.getTaxaJuros());
        r.setPrazoMeses(e.getPrazoMeses());
        r.setParcelasPagas(e.getParcelasPagas());
        r.setParcelasRestantes(e.getParcelasRestantes());
        r.setValorParcela(e.getValorParcela());
        r.setDataContratacao(e.getDataContratacao());
        r.setDataVencimentoPrimeiraParcela(e.getDataVencimentoPrimeiraParcela());
        r.setStatusEmprestimo(e.getStatusEmprestimo());
        return r;
    }
}
