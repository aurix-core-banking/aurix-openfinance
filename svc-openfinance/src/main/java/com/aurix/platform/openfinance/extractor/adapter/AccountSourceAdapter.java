package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de acesso a contas/transações de um core bancário.
 *
 * <p>{@link com.aurix.platform.openfinance.extractor.CoreAccountExtractor} depende só
 * desta interface — nunca de um repositório JPA concreto. Plugar o Open Finance em
 * outro core (outro banco de dados, uma API REST, um arquivo SFTP) é implementar esta
 * interface e registrar o bean; nada no extractor muda.
 */
public interface AccountSourceAdapter {

    List<Account> findAccounts(String consentId);

    List<Transaction> findTransactions(String consentId);

    record Account(
            String accountId,
            String institutionCode,
            String currency,
            String type,
            String status,
            BigDecimal availableBalance,
            BigDecimal currentBalance,
            LocalDateTime updatedAt) {
    }

    record Transaction(
            String accountId,
            String transactionId,
            String type,
            BigDecimal amount,
            String currency,
            String merchant,
            String description,
            LocalDateTime transactionDate,
            LocalDateTime processingDate) {
    }
}
