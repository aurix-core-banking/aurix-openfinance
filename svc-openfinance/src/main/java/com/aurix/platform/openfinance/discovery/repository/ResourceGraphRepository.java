package com.aurix.platform.openfinance.discovery.repository;

import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceGraphRepository extends JpaRepository<ResourceGraph, Long> {

    Optional<ResourceGraph> findByGraphId(String graphId);

    Optional<ResourceGraph> findByConsentId(String consentId);

    @Query("SELECT g FROM ResourceGraph g WHERE g.consentId = :consentId " +
            "ORDER BY g.version DESC LIMIT 1")
    Optional<ResourceGraph> findLatestByConsentId(@Param("consentId") String consentId);
}
