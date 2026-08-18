package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.FaturaConsentida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FaturaConsentidaRepository extends JpaRepository<FaturaConsentida, Long> {
    List<FaturaConsentida> findByCartaoIdAndConsentId(String cartaoId, String consentId);
    List<FaturaConsentida> findByConsentId(String consentId);
}
