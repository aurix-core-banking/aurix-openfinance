package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.TransacaoConsentida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransacaoConsentidaRepository extends JpaRepository<TransacaoConsentida, Long> {
    List<TransacaoConsentida> findByAccountIdAndConsentId(String accountId, String consentId);
    List<TransacaoConsentida> findByConsentId(String consentId);
}
