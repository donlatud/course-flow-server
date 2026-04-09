package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.payment.CreateOrderRequest;
import com.techup.course_flow_server.dto.payment.OrderResponse;
import com.techup.course_flow_server.dto.payment.OrderStatusResponse;
import com.techup.course_flow_server.dto.payment.PaymentResponse;
import com.techup.course_flow_server.dto.payment.PromoCodeValidationResponse;
import com.techup.course_flow_server.dto.payment.ProcessCardPaymentRequest;
import com.techup.course_flow_server.dto.payment.ValidatePromoCodeRequest;
import com.techup.course_flow_server.service.OmiseWebhookSignatureVerifier;
import com.techup.course_flow_server.service.OrderService;
import com.techup.course_flow_server.service.PaymentService;
import com.techup.course_flow_server.service.PromoCodeService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * PaymentController
 *
 * First-phase checkout endpoints used by the frontend payment flow.
 *
 * Scope:
 * - create/reuse single-course order, order status polling
 * - promo validation
 * - Omise card charge and webhook (signature verified when {@code omise.webhook-secret} is set)
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private final OrderService orderService;
    private final PromoCodeService promoCodeService;
    private final PaymentService paymentService;
    private final OmiseWebhookSignatureVerifier omiseWebhookSignatureVerifier;

    public PaymentController(
            OrderService orderService,
            PromoCodeService promoCodeService,
            PaymentService paymentService,
            OmiseWebhookSignatureVerifier omiseWebhookSignatureVerifier) {
        this.orderService = orderService;
        this.promoCodeService = promoCodeService;
        this.paymentService = paymentService;
        this.omiseWebhookSignatureVerifier = omiseWebhookSignatureVerifier;
    }

    private static UUID userIdFromJwt(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id in token");
        }
    }

    /**
     * Creates a new order or reuses the latest valid pending order for the same
     * user and course.
     */
    @PostMapping("/orders")
    public OrderResponse createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateOrderRequest request) {
        try {
            UUID userId = userIdFromJwt(jwt);
            return orderService.createOrReuseOrder(userId, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Returns the current order summary for the authenticated user.
     */
    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        try {
            UUID userId = userIdFromJwt(jwt);
            return orderService.getOrder(userId, orderId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Lightweight polling endpoint used after 3DS redirect or future async
     * payment methods such as PromptPay QR.
     */
    @GetMapping("/orders/{orderId}/status")
    public OrderStatusResponse getOrderStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        try {
            UUID userId = userIdFromJwt(jwt);
            return orderService.getOrderStatus(userId, orderId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Validates a promo code before payment submission and returns the computed
     * discount values expected by the checkout UI.
     */
    @PostMapping("/promo-codes/validate")
    public PromoCodeValidationResponse validatePromoCode(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ValidatePromoCodeRequest request) {
        try {
            UUID userId = userIdFromJwt(jwt);
            return promoCodeService.validatePromoCode(userId, request.code(), request.originalPrice());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Creates a card charge using an Omise token that was already generated on
     * the frontend with Omise.js.
     */
    @PostMapping("/payments/card")
    public PaymentResponse processCardPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ProcessCardPaymentRequest request) {
        try {
            UUID userId = userIdFromJwt(jwt);
            return paymentService.processCardPayment(userId, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Omise webhook endpoint used to confirm asynchronous card outcomes such as
     * post-3DS completion. When {@code omise.webhook-secret} is configured, requests must
     * include valid {@code Omise-Signature} / {@code Omise-Signature-Timestamp} headers.
     */
    @PostMapping("/webhooks/omise")
    public Map<String, String> handleOmiseWebhook(
            HttpServletRequest request,
            // ต้องเป็น raw body ตามที่ Omise ใช้คำนวณ HMAC — ห้าม parse JSON แล้ว serialize ใหม่ก่อน verify
            @RequestBody byte[] rawBody) {
        if (!omiseWebhookSignatureVerifier.verify(request, rawBody)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Omise webhook signature");
        }
        // หลัง verify แล้วค่อยแปลงเป็น String ให้ Jackson/service parse JSON เหมือนเดิม
        String payload = new String(rawBody, StandardCharsets.UTF_8);
        try {
            paymentService.handleOmiseWebhook(payload);
            return Map.of("status", "ok");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
