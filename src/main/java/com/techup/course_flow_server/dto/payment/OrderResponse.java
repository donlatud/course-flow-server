package com.techup.course_flow_server.dto.payment;

import com.techup.course_flow_server.entity.Order;
import com.techup.course_flow_server.entity.PromoCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Checkout order summary returned to the frontend.
 */
@Getter
@Builder
public class OrderResponse {

    private final UUID orderId;
    private final UUID courseId;
    private final String courseName;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final PromoCodeInfo promoCode;
    private final Order.Status status;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    @Getter
    @Builder
    public static class PromoCodeInfo {
        private final String code;
        private final PromoCode.DiscountType discountType;
        private final BigDecimal discountValue;
    }
}
