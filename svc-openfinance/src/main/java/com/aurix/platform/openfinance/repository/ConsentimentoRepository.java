package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {
    Optional<Consentimento> findByConsentId(String consentId);
    List<Consentimento> findByUserIdAndStatus(Long userId, Consentimento.StatusConsentimento status);
    List<Consentimento> findByClientId(String clientId);
    List<Consentimento> findByStatus(Consentimento.StatusConsentimento status);
}
