package com.aurix.platform.openfinance.policy.repository;

import com.aurix.platform.openfinance.policy.entity.PolicyDecision;
import com.aurix.platform.openfinance.policy.entity.PolicyDecisionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PolicyDecisionRepository extends JpaRepository<PolicyDecision, Long> {

    List<PolicyDecision> findByConsentIdOrderByEvaluatedAtDesc(String consentId);

    Page<PolicyDecision> findByEvaluatedAtBetween(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<PolicyDecision> findByDecisionOrderByEvaluatedAtDesc(
            PolicyDecisionType decision, Pageable pageable);

    @Query("SELECT d FROM PolicyDecision d WHERE d.consentId = :consentId " +
            "AND d.resourceId = :resourceId AND d.decision = :decision " +
            "ORDER BY d.evaluatedAt DESC")
    List<PolicyDecision> findRecentDecisions(
            @Param("consentId") String consentId,
            @Param("resourceId") String resourceId,
            @Param("decision") PolicyDecisionType decision);

    @Query("SELECT COUNT(d) FROM PolicyDecision d WHERE d.evaluatedAt >= :since " +
            "AND d.decision = :decision")
    long countDecisionsSince(
            @Param("since") LocalDateTime since,
            @Param("decision") PolicyDecisionType decision);
}
