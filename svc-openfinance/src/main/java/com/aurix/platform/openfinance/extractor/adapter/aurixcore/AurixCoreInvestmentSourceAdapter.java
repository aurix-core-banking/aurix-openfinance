package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.InvestimentoConsentido;
import com.aurix.platform.openfinance.extractor.adapter.InvestmentSourceAdapter;
import com.aurix.platform.openfinance.repository.InvestimentoConsentidoRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Implementação do {@link InvestmentSourceAdapter} para o core banking/corretora "aurix".
 */
@Component
@org.springframework.context.annotation.Primary
public class AurixCoreInvestmentSourceAdapter implements InvestmentSourceAdapter {

    private final InvestimentoConsentidoRepository investimentoRepository;

    public AurixCoreInvestmentSourceAdapter(InvestimentoConsentidoRepository investimentoRepository) {
        this.investimentoRepository = investimentoRepository;
    }

    @Override
    public List<Investment> findInvestments(String consentId) {
        return investimentoRepository.findByConsentId(consentId).stream()
                .map(this::toInvestment)
                .toList();
    }

    private Investment toInvestment(InvestimentoConsentido inv) {
        return new Investment(
                inv.getInvestimentoId(),
                inv.getClienteId(),
                inv.getTipoInvestimento(),
                inv.getProduto(),
                inv.getQuantidade(),
                inv.getValorUnitario(),
                inv.getValorTotal(),
                inv.getDataAtualizacao()
        );
    }
}
