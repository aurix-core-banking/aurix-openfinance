package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.PixConsentido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PixConsentidoRepository extends JpaRepository<PixConsentido, Long> {
    List<PixConsentido> findByConsentId(String consentId);
    List<PixConsentido> findByClienteIdAndConsentId(String clienteId, String consentId);
}
