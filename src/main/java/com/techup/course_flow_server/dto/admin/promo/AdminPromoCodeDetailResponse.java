package com.techup.course_flow_server.dto.admin.promo;

import com.techup.course_flow_server.entity.PromoCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Full promo payload for admin edit form. */
public record AdminPromoCodeDetailResponse(
        UUID id,
        String code,
        BigDecimal minimumPurchaseAmount,
        PromoCode.DiscountType discountType,
        BigDecimal discountValue,
        List<UUID> courseIds) {}
