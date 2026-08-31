package com.aurix.platform.openfinance.pipeline.canonicalization.repository;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de registros brutos extraídos (Gap 5 — persistência do pipeline).
 */
public interface RawRecordRepository extends JpaRepository<RawRecord, Long> {
}
