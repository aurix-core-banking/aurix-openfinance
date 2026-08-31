package com.aurix.platform.openfinance.pipeline.pii.repository;

import com.aurix.platform.openfinance.pipeline.pii.entity.PiiClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de classificações PII (Gap 5 — persistência do pipeline).
 */
public interface PiiClassificationRepository extends JpaRepository<PiiClassificationEntity, Long> {
}
