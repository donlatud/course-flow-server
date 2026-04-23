package com.techup.course_flow_server.dto.payment;

import com.techup.course_flow_server.entity.Order;
import com.techup.course_flow_server.entity.Payment;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight order status response used by frontend polling after 3DS or
 * other async payment flows.
 */
@Getter
@Builder
public class OrderStatusResponse {

    private final UUID orderId;
    private final Order.Status status;
    private final Payment.Status paymentStatus;
    private final LocalDateTime expiresAt;
}
