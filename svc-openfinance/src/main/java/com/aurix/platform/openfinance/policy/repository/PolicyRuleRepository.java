package com.aurix.platform.openfinance.policy.repository;

import com.aurix.platform.openfinance.policy.entity.PolicyRule;
import com.aurix.platform.openfinance.policy.entity.PolicyRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    Optional<PolicyRule> findByRuleCode(String ruleCode);

    List<PolicyRule> findByActiveTrueOrderByPriorityAsc();

    List<PolicyRule> findByActiveTrueAndTypeOrderByPriorityAsc(PolicyRuleType type);

    @Query("SELECT r FROM PolicyRule r WHERE r.active = true ORDER BY r.priority ASC")
    List<PolicyRule> findAllActiveRulesOrdered();

    boolean existsByRuleCode(String ruleCode);
}
