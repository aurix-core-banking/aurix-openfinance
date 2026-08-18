package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.SeguroConsentido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguroConsentidoRepository extends JpaRepository<SeguroConsentido, Long> {
    List<SeguroConsentido> findByConsentId(String consentId);
    List<SeguroConsentido> findByApoliceId(String apoliceId);
}
