package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.SeguroConsentido;
import com.aurix.platform.openfinance.extractor.adapter.InsuranceSourceAdapter;
import com.aurix.platform.openfinance.repository.SeguroConsentidoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementação do {@link InsuranceSourceAdapter} para o core "aurix".
 */
@Component
public class AurixCoreInsuranceSourceAdapter implements InsuranceSourceAdapter {

    private final SeguroConsentidoRepository repository;

    public AurixCoreInsuranceSourceAdapter(SeguroConsentidoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InsurancePolicy> findPolicies(String consentId) {
        return repository.findByConsentId(consentId).stream()
                .map(this::toPolicy)
                .toList();
    }

    private InsurancePolicy toPolicy(SeguroConsentido s) {
        BigDecimal premio = s.getPremioTotal() != null ? s.getPremioTotal()
                : (s.getPremioMensal() != null ? s.getPremioMensal() : BigDecimal.ZERO);
        return new InsurancePolicy(
                s.getApoliceId(),
                s.getClienteId(),
                s.getTipoSeguro(),
                s.getValorSegurado() != null ? s.getValorSegurado() : BigDecimal.ZERO,
                premio,
                s.getStatusApolice() != null ? s.getStatusApolice() : "ATIVA",
                s.getDataInicio(),
                s.getDataFim(),
                s.getDataAtualizacao()
        );
    }
}
