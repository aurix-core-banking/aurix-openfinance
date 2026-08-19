package com.aurix.platform.openfinance.distribution.subscription.repository;

import com.aurix.platform.openfinance.distribution.subscription.entity.Subscription;
import com.aurix.platform.openfinance.distribution.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de assinaturas.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriptionId(String subscriptionId);

    List<Subscription> findByParticipantId(String participantId);

    List<Subscription> findByDataProductId(String dataProductId);

    List<Subscription> findByDataProductIdAndStatus(String dataProductId, SubscriptionStatus status);
}
