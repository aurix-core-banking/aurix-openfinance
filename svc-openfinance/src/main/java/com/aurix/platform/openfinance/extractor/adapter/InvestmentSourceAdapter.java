package com.aurix.platform.openfinance.extractor.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de acesso a investimentos de um core bancário/corretora.
 * Ver {@link AccountSourceAdapter} para o racional de desacoplamento.
 *
 * <p>Nenhum core plugado hoje fornece dados de investimento — o Spring injeta um
 * {@code Optional<InvestmentSourceAdapter>} vazio em
 * {@link com.aurix.platform.openfinance.extractor.CoreInvestmentExtractor} até que
 * alguém registre um bean implementando esta porta.
 */
public interface InvestmentSourceAdapter {

    List<Investment> findInvestments(String consentId);

    record Investment(
            String investmentId,
            String customerId,
            String type,
            String product,
            BigDecimal quantity,
            BigDecimal unitValue,
            BigDecimal totalValue,
            LocalDateTime updatedAt) {
    }
}
