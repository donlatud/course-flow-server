package com.techup.course_flow_server.dto.payment;

import java.util.UUID;

/**
 * Request sent after the frontend has tokenized card details via Omise.js.
 */
public record ProcessCardPaymentRequest(
        UUID orderId,
        String token
) {
}
