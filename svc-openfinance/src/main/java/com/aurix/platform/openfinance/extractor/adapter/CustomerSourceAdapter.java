package com.aurix.platform.openfinance.extractor.adapter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de adaptação de fonte de dados para dados cadastrais de clientes (INV-002).
 */
public interface CustomerSourceAdapter {

    List<CustomerRecord> findCustomers(String consentId);

    record CustomerRecord(
            String customerId,
            String documentNumber,
            String personType,
            String fullName,
            String email,
            String phone,
            String status,
            LocalDateTime updatedAt
    ) {}
}
