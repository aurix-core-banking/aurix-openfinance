package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.PessoaConsentida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PessoaConsentidaRepository extends JpaRepository<PessoaConsentida, Long> {
    List<PessoaConsentida> findByConsentId(String consentId);
    Optional<PessoaConsentida> findByCustomerIdAndConsentId(String customerId, String consentId);
    List<PessoaConsentida> findByCustomerId(String customerId);
}
