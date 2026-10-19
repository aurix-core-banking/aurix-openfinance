package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.InvestimentoConsentido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestimentoConsentidoRepository extends JpaRepository<InvestimentoConsentido, Long> {
    List<InvestimentoConsentido> findByConsentId(String consentId);
    List<InvestimentoConsentido> findByInvestimentoId(String investimentoId);
}
