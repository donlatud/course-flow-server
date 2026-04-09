package com.techup.course_flow_server.dto.payment;

import com.techup.course_flow_server.entity.PromoCode;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Validation result returned to the checkout UI.
 */
@Getter
@Builder
public class PromoCodeValidationResponse {

    private final boolean valid;
    private final String code;
    private final PromoCode.DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal discountAmount;
    private final BigDecimal finalPrice;
    private final Reason reason;

    public enum Reason {
        NOT_FOUND,
        EXPIRED,
        USAGE_LIMIT_REACHED,
        ALREADY_USED,
        INVALID
    }
}
