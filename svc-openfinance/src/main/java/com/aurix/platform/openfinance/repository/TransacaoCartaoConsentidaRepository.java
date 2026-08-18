package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.TransacaoCartaoConsentida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransacaoCartaoConsentidaRepository extends JpaRepository<TransacaoCartaoConsentida, Long> {
    List<TransacaoCartaoConsentida> findByCartaoIdAndConsentId(String cartaoId, String consentId);
    List<TransacaoCartaoConsentida> findByConsentId(String consentId);
}
