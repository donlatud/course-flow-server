package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.payment.PaymentResponse;
import com.techup.course_flow_server.dto.payment.ProcessCardPaymentRequest;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.entity.Order;
import com.techup.course_flow_server.entity.OrderItem;
import com.techup.course_flow_server.entity.Payment;
import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.entity.PromoRedemption;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import com.techup.course_flow_server.repository.OrderItemRepository;
import com.techup.course_flow_server.repository.OrderRepository;
import com.techup.course_flow_server.repository.PaymentRepository;
import com.techup.course_flow_server.repository.PromoCodeRepository;
import com.techup.course_flow_server.repository.PromoRedemptionRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coordinates charge creation, local payment persistence, and webhook-driven
 * side effects such as enrollment and promo redemption recording.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String PAYMENT_GATEWAY_OMISE = "OMISE";

    private static final String OMISE_EVENT_CHARGE_CREATE = "charge.create";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoRedemptionRepository promoRedemptionRepository;
    private final PaymentGatewayService paymentGatewayService;

    public PaymentService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            EnrollmentRepository enrollmentRepository,
            PromoCodeRepository promoCodeRepository,
            PromoRedemptionRepository promoRedemptionRepository,
            PaymentGatewayService paymentGatewayService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.promoRedemptionRepository = promoRedemptionRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Creates an Omise charge for a valid pending order owned by the current
     * user. If the order already has a successful payment, the response is
     * returned from local state without creating a duplicate charge.
     */
    @Transactional
    public PaymentResponse processCardPayment(UUID userId, ProcessCardPaymentRequest request) {
        Order order = orderRepository.findByIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.orderId()));

        if (order.isExpiredAt(LocalDateTime.now())) {
            order.setStatus(Order.Status.EXPIRED);
            orderRepository.save(order);
            throw new IllegalArgumentException("Order has expired");
        }

        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalArgumentException("Order is not available for payment");
        }

        Payment existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getStatus() == Payment.Status.SUCCESS) {
            return toPaymentResponse(existingPayment, null, null, null);
        }

        OrderItem orderItem = orderItemRepository.findFirstByOrderId(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order item not found for order: " + order.getId()));

        String returnUri = buildReturnUri(orderItem.getCourse().getId(), order.getId());
        String description = "CourseFlow order " + order.getId();

        PaymentGatewayService.ChargeResult charge = paymentGatewayService.createCardCharge(
                order.getTotalAmount(),
                request.token(),
                description,
                returnUri);

        Payment payment = existingPayment != null ? existingPayment : Payment.builder()
                .order(order)
                .paymentGateway(PAYMENT_GATEWAY_OMISE)
                .paymentMethod("CREDIT_CARD")
                .currency(normalizeCurrency(charge.currency()))
                .amount(normalizeAmount(order.getTotalAmount()))
                .status(Payment.Status.PENDING)
                .build();

        payment.setPaymentGatewayRef(charge.chargeId());
        payment.setPaymentGateway(PAYMENT_GATEWAY_OMISE);
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setCurrency(normalizeCurrency(charge.currency()));
        payment.setAmount(normalizeAmount(order.getTotalAmount()));

        applyGatewayResult(
                order,
                payment,
                charge.status(),
                charge.paid(),
                charge.authorized(),
                charge.failureCode(),
                charge.failureMessage());
        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        if (savedPayment.getStatus() == Payment.Status.SUCCESS) {
            finalizeSuccessfulPayment(order);
        }

        return toPaymentResponse(savedPayment, charge.authorizeUri(), charge.returnUri(), charge.failureMessage());
    }

    /**
     * Updates local payment/order state from an Omise webhook payload. This
     * method is idempotent so duplicate webhook deliveries are safe.
     */
    @Transactional
    public void handleOmiseWebhook(String payload) {
        PaymentGatewayService.ChargeWebhookEvent event = paymentGatewayService.parseChargeWebhook(payload);
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentGatewayRef(event.chargeId());
        if (paymentOpt.isEmpty() && OMISE_EVENT_CHARGE_CREATE.equals(event.eventKey())) {
            sleepQuietly(200L);
            paymentOpt = paymentRepository.findByPaymentGatewayRef(event.chargeId());
        }
        if (paymentOpt.isEmpty()) {
            if (OMISE_EVENT_CHARGE_CREATE.equals(event.eventKey())) {
                log.warn(
                        "Omise webhook {}: no payment row for charge {} yet (often arrives before /api/payments/card commits); acknowledging without update",
                        event.eventKey(),
                        event.chargeId());
                return;
            }
            throw new IllegalArgumentException("Payment not found for charge: " + event.chargeId());
        }
        Payment payment = paymentOpt.get();

        Order order = payment.getOrder();
        payment.setPaymentGateway(PAYMENT_GATEWAY_OMISE);
        if (event.currency() != null && !event.currency().isBlank()) {
            payment.setCurrency(normalizeCurrency(event.currency()));
        }
        applyGatewayResult(
                order,
                payment,
                event.status(),
                event.paid(),
                event.authorized(),
                event.failureCode(),
                event.failureMessage());
        paymentRepository.save(payment);
        orderRepository.save(order);

        if (payment.getStatus() == Payment.Status.SUCCESS) {
            finalizeSuccessfulPayment(order);
        }
    }

    private void applyGatewayResult(
            Order order,
            Payment payment,
            String gatewayStatus,
            boolean paid,
            boolean authorized,
            String failureCode,
            String failureMessage) {
        String normalized = gatewayStatus == null ? "" : gatewayStatus.trim().toLowerCase();

        if (paid || "successful".equals(normalized)) {
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setFailureCode(null);
            payment.setFailureMessage(null);
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
            order.setStatus(Order.Status.COMPLETED);
            return;
        }

        if ("failed".equals(normalized) || "expired".equals(normalized) || "reversed".equals(normalized)) {
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureCode(failureCode);
            payment.setFailureMessage(failureMessage);
            order.setStatus("expired".equals(normalized) ? Order.Status.EXPIRED : Order.Status.FAILED);
            return;
        }

        // Pending keeps the order reusable for polling/webhook completion later,
        // especially during 3DS where authorization is not final yet.
        payment.setStatus(Payment.Status.PENDING);
        payment.setFailureCode(null);
        payment.setFailureMessage(failureMessage);
        order.setStatus(Order.Status.PENDING);
    }

    private void finalizeSuccessfulPayment(Order order) {
        OrderItem orderItem = orderItemRepository.findFirstByOrderId(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order item not found for order: " + order.getId()));

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(order.getUser().getId(), orderItem.getCourse().getId())
                .orElse(null);

        if (enrollment == null) {
            enrollment = Enrollment.builder()
                    .user(order.getUser())
                    .course(orderItem.getCourse())
                    .status(Enrollment.Status.ACTIVE)
                    .build();
        } else if (enrollment.getStatus() == Enrollment.Status.UNSUBSCRIBED) {
            enrollment.setStatus(Enrollment.Status.ACTIVE);
        }
        enrollmentRepository.save(enrollment);

        PromoCode promoCode = order.getPromoCode();
        if (promoCode == null || promoRedemptionRepository.findByOrderId(order.getId()).isPresent()) {
            return;
        }

        promoCode.setUsageCount((promoCode.getUsageCount() == null ? 0 : promoCode.getUsageCount()) + 1);
        promoCodeRepository.save(promoCode);

        PromoRedemption redemption = PromoRedemption.builder()
                .promoCode(promoCode)
                .user(order.getUser())
                .order(order)
                .build();
        promoRedemptionRepository.save(redemption);
    }

    private PaymentResponse toPaymentResponse(
            Payment payment,
            String authorizeUri,
            String returnUri) {
        return toPaymentResponse(payment, authorizeUri, returnUri, null);
    }

    private PaymentResponse toPaymentResponse(
            Payment payment,
            String authorizeUri,
            String returnUri,
            String reason) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .chargeId(payment.getPaymentGatewayRef())
                .status(payment.getStatus())
                .authorizeUri(authorizeUri)
                .returnUri(returnUri)
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .reason(reason)
                .build();
    }

    private String buildReturnUri(UUID courseId, UUID orderId) {
        return "http://localhost:5173/courses/" + courseId + "/checkout?orderId=" + orderId;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2) : amount.setScale(2);
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? null : currency.trim().toUpperCase();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
