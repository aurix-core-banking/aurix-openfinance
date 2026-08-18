package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.ContaConsentida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContaConsentidaRepository extends JpaRepository<ContaConsentida, Long> {
    List<ContaConsentida> findByConsentId(String consentId);
    List<ContaConsentida> findByAccountId(String accountId);
}
