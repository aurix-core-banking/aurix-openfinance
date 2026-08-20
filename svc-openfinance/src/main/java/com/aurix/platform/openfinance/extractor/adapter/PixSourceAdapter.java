package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de acesso a transações PIX de um core bancário.
 * Ver {@link AccountSourceAdapter} para o racional de desacoplamento.
 */
public interface PixSourceAdapter {

    List<PixTransaction> findPixTransactions(String consentId);

    record PixTransaction(
            String pixId,
            String customerId,
            String type,
            String pixKey,
            String keyType,
            BigDecimal amount,
            String currency,
            String description,
            String status,
            LocalDateTime transactionDate,
            LocalDateTime updatedAt) {
    }
}
