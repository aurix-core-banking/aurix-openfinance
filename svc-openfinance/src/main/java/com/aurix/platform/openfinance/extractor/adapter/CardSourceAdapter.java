package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de acesso a cartões de crédito/faturas/transações de um core bancário.
 * Ver {@link AccountSourceAdapter} para o racional de desacoplamento.
 */
public interface CardSourceAdapter {

    List<Card> findCards(String consentId);

    List<Bill> findBills(String consentId);

    List<CardTransaction> findCardTransactions(String consentId);

    record Card(
            String cardId,
            String customerId,
            String brand,
            String lastDigits,
            String status,
            BigDecimal creditLimit,
            BigDecimal availableLimit,
            BigDecimal currentBillAmount,
            LocalDate billDueDate,
            LocalDateTime updatedAt) {
    }

    record Bill(
            String cardId,
            String billId,
            BigDecimal totalAmount,
            BigDecimal minimumAmount,
            BigDecimal paidAmount,
            LocalDate dueDate,
            LocalDate paymentDate,
            LocalDateTime updatedAt) {
    }

    record CardTransaction(
            String cardId,
            String transactionId,
            BigDecimal amount,
            String currency,
            String merchant,
            String type,
            LocalDateTime transactionDate) {
    }
}
