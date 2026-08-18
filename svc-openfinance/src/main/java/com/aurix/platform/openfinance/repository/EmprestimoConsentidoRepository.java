package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.EmprestimoConsentido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmprestimoConsentidoRepository extends JpaRepository<EmprestimoConsentido, Long> {
    List<EmprestimoConsentido> findByConsentId(String consentId);
    List<EmprestimoConsentido> findByEmprestimoId(String emprestimoId);
}
