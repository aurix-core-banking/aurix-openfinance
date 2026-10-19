package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de adaptação de fonte de dados para seguros (INV-002).
 */
public interface InsuranceSourceAdapter {

    List<InsurancePolicy> findPolicies(String consentId);

    record InsurancePolicy(
            String policyId,
            String documentNumber,
            String type,
            BigDecimal insuredAmount,
            BigDecimal premiumAmount,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime updatedAt
    ) {}
}
