package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.EmprestimoConsentido;
import com.aurix.platform.openfinance.extractor.adapter.LoanSourceAdapter;
import com.aurix.platform.openfinance.repository.EmprestimoConsentidoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação do {@link LoanSourceAdapter} para o core banking "aurix".
 */
@Component
public class AurixCoreLoanSourceAdapter implements LoanSourceAdapter {

    private final EmprestimoConsentidoRepository emprestimoRepository;

    public AurixCoreLoanSourceAdapter(EmprestimoConsentidoRepository emprestimoRepository) {
        this.emprestimoRepository = emprestimoRepository;
    }

    @Override
    public List<Loan> findLoans(String consentId) {
        return emprestimoRepository.findByConsentId(consentId).stream().map(this::toLoan).toList();
    }

    private Loan toLoan(EmprestimoConsentido e) {
        return new Loan(
                e.getEmprestimoId(),
                e.getClienteId(),
                e.getTipoEmprestimo(),
                e.getValorContratado(),
                e.getValorSaldoDevedor(),
                e.getTaxaJuros(),
                e.getPrazoMeses(),
                e.getParcelasPagas(),
                e.getParcelasRestantes(),
                e.getValorParcela(),
                e.getDataContratacao(),
                e.getDataVencimentoPrimeiraParcela(),
                e.getStatusEmprestimo(),
                e.getDataAtualizacao());
    }
}
