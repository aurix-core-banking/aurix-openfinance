package com.aurix.platform.openfinance.context.repository;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizedContextRepository extends JpaRepository<AuthorizedContext, Long> {

    Optional<AuthorizedContext> findByContextId(String contextId);

    Optional<AuthorizedContext> findByContextIdAndRevokedFalse(String contextId);

    List<AuthorizedContext> findByConsentIdAndRevokedFalse(String consentId);

    List<AuthorizedContext> findBySubjectAndRevokedFalse(String subject);

    @Query("SELECT c FROM AuthorizedContext c WHERE c.consentId = :consentId " +
            "AND c.revoked = false AND c.validUntil > CURRENT_TIMESTAMP")
    List<AuthorizedContext> findActiveContexts(@Param("consentId") String consentId);

    boolean existsByContextIdAndRevokedFalse(String contextId);
}
