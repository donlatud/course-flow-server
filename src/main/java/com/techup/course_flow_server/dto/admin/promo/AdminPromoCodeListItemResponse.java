package com.techup.course_flow_server.dto.admin.promo;

import com.techup.course_flow_server.entity.PromoCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One row for the admin promo code table.
 * Sortable fields: code, minimumPurchaseAmount, discountType, createdAt, coursesIncludedLength
 */
public record AdminPromoCodeListItemResponse(
        UUID id,
        String code,
        BigDecimal minimumPurchaseAmount,
        PromoCode.DiscountType discountType,
        /** True when this promo is linked to every course in the database (at list time). */
        boolean allCourses,
        List<String> courseTitles,
        int coursesIncludedLength,
        LocalDateTime createdAt) {}
