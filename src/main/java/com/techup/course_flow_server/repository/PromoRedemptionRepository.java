package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoRedemption;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for promo redemption history.
 *
 * This will be used by the promo validation flow to enforce the
 * one-user-one-code rule and by the payment success flow to persist
 * successful redemptions.
 */
public interface PromoRedemptionRepository extends JpaRepository<PromoRedemption, UUID> {

    boolean existsByPromoCodeIdAndUserId(UUID promoCodeId, UUID userId);

    Optional<PromoRedemption> findByOrderId(UUID orderId);
}
