package com.aurix.platform.openfinance.pipeline.canonicalization.repository;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de registros canônicos (Gap 5 — persistência do pipeline).
 */
public interface CanonicalRecordRepository extends JpaRepository<CanonicalRecord, Long> {
}
