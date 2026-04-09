package com.techup.course_flow_server.dto.payment;

import com.techup.course_flow_server.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Payment result returned to the checkout UI after charge creation.
 */
@Getter
@Builder
public class PaymentResponse {

    private final UUID paymentId;
    private final UUID orderId;
    private final String chargeId;
    private final Payment.Status status;
    private final String authorizeUri;
    private final String returnUri;
    private final BigDecimal amount;
    private final LocalDateTime paidAt;
    private final String reason;
}
