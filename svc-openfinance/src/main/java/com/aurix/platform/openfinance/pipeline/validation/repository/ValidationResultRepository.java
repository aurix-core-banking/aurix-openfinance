package com.aurix.platform.openfinance.pipeline.validation.repository;

import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de resultados de validação de schema (Gap 5 — persistência do pipeline).
 */
public interface ValidationResultRepository extends JpaRepository<ValidationResultEntity, Long> {
}
