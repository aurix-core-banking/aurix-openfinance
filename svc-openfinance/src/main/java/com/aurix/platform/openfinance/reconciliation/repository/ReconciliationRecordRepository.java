package com.aurix.platform.openfinance.reconciliation.repository;

import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {

    Optional<ReconciliationRecord> findByReconciliationId(String reconciliationId);

    List<ReconciliationRecord> findByPlanId(String planId);

    List<ReconciliationRecord> findByNodeId(String nodeId);

    List<ReconciliationRecord> findByStatus(ReconciliationStatus status);

    List<ReconciliationRecord> findByPlanIdAndStatus(String planId, ReconciliationStatus status);

    @Query("SELECT r FROM ReconciliationRecord r WHERE r.planId = :planId AND r.status IN :statuses")
    List<ReconciliationRecord> findByPlanIdAndStatuses(
            @Param("planId") String planId,
            @Param("statuses") List<ReconciliationStatus> statuses);

    long countByPlanIdAndStatus(String planId, ReconciliationStatus status);

    boolean existsByPlanIdAndNodeId(String planId, String nodeId);

    @Query("SELECT SUM(CASE WHEN r.actualCount < r.expectedCount THEN r.expectedCount - r.actualCount ELSE 0 END) " +
            "FROM ReconciliationRecord r WHERE r.planId = :planId")
    Integer totalRegistrosFaltantes(@Param("planId") String planId);

    @Query("SELECT SUM(CASE WHEN r.actualCount > r.expectedCount THEN r.actualCount - r.expectedCount ELSE 0 END) " +
            "FROM ReconciliationRecord r WHERE r.planId = :planId")
    Integer totalRegistrosExcedentes(@Param("planId") String planId);
}
