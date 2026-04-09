package com.techup.course_flow_server.dto.payment;

import java.util.UUID;

/**
 * Request for creating or reusing a single-course checkout order.
 */
public record CreateOrderRequest(
        UUID courseId,
        String promoCode
) {
}
