package com.techup.course_flow_server.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request used by the checkout page to validate a promo code before order
 * creation or while updating an existing pending order.
 */
public record ValidatePromoCodeRequest(
        String code,
        UUID courseId,
        BigDecimal originalPrice
) {
}
