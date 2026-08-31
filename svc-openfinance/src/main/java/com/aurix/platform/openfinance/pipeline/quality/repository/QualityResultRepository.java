package com.aurix.platform.openfinance.pipeline.quality.repository;

import com.aurix.platform.openfinance.pipeline.quality.entity.QualityResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de resultados de verificação de qualidade (Gap 5 — persistência do pipeline).
 */
public interface QualityResultRepository extends JpaRepository<QualityResultEntity, Long> {
}
