package com.aurix.platform.openfinance.trust.receipt.repository;

import com.aurix.platform.openfinance.trust.receipt.entity.ConsentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório de recibos de consentimento.
 */
public interface ConsentReceiptRepository extends JpaRepository<ConsentReceipt, Long> {

    Optional<ConsentReceipt> findByConsentId(String consentId);

    Optional<ConsentReceipt> findByReceiptId(String receiptId);
}
