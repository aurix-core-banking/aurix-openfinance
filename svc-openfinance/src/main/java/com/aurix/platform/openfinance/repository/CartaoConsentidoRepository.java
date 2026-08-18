package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.CartaoConsentido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartaoConsentidoRepository extends JpaRepository<CartaoConsentido, Long> {
    List<CartaoConsentido> findByConsentId(String consentId);
    List<CartaoConsentido> findByCartaoId(String cartaoId);
}
