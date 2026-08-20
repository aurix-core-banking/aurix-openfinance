package com.aurix.platform.openfinance.pipeline.lineage.repository;

import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineageRecordRepository extends JpaRepository<LineageRecord, Long> {

    Optional<LineageRecord> findByLineageId(String lineageId);

    List<LineageRecord> findByPublicationIdOrderByCreatedAtAsc(String publicationId);

    List<LineageRecord> findByConsentIdOrderByCreatedAtAsc(String consentId);
}
