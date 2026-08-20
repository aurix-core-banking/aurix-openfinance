package com.aurix.platform.openfinance.distribution.dataproduct.repository;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProductRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataProductRecordRepository extends JpaRepository<DataProductRecord, Long> {

    Page<DataProductRecord> findByProductId(String productId, Pageable pageable);

    long countByProductId(String productId);
}
