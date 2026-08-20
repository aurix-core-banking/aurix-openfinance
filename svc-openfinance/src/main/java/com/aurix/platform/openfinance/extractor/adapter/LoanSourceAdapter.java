package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de acesso a empréstimos de um core bancário.
 * Ver {@link AccountSourceAdapter} para o racional de desacoplamento.
 */
public interface LoanSourceAdapter {

    List<Loan> findLoans(String consentId);

    record Loan(
            String loanId,
            String customerId,
            String type,
            BigDecimal contractedAmount,
            BigDecimal outstandingBalance,
            BigDecimal interestRate,
            Integer termMonths,
            Integer installmentsPaid,
            Integer installmentsRemaining,
            BigDecimal installmentAmount,
            LocalDate contractDate,
            LocalDate firstInstallmentDueDate,
            String status,
            LocalDateTime updatedAt) {
    }
}
