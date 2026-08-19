package com.aurix.platform.openfinance.planner.repository;

import com.aurix.platform.openfinance.planner.entity.ExecutionPlan;
import com.aurix.platform.openfinance.planner.entity.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionPlanRepository extends JpaRepository<ExecutionPlan, Long> {

    Optional<ExecutionPlan> findByPlanId(String planId);

    List<ExecutionPlan> findByConsentId(String consentId);

    List<ExecutionPlan> findByStatus(PlanStatus status);

    List<ExecutionPlan> findByParticipanteId(String participanteId);

    @Query("SELECT p FROM ExecutionPlan p WHERE p.consentId = :consentId AND p.status IN :statuses")
    List<ExecutionPlan> findByConsentIdAndStatuses(
            @Param("consentId") String consentId,
            @Param("statuses") List<PlanStatus> statuses);

    boolean existsByPlanId(String planId);

    boolean existsByConsentIdAndStatusIn(String consentId, List<PlanStatus> statuses);
}
