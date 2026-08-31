package com.aurix.platform.openfinance.pipeline.quality.repository;

import com.aurix.platform.openfinance.pipeline.quality.entity.QualityIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de problemas individuais de qualidade (Gap 5 — persistência do pipeline).
 */
public interface QualityIssueRepository extends JpaRepository<QualityIssueEntity, Long> {
}
