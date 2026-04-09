package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.payment.CreateOrderRequest;
import com.techup.course_flow_server.dto.payment.OrderResponse;
import com.techup.course_flow_server.dto.payment.OrderStatusResponse;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.entity.Order;
import com.techup.course_flow_server.entity.OrderItem;
import com.techup.course_flow_server.entity.Payment;
import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import com.techup.course_flow_server.repository.OrderItemRepository;
import com.techup.course_flow_server.repository.OrderRepository;
import com.techup.course_flow_server.repository.PaymentRepository;
import com.techup.course_flow_server.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Core checkout order logic for the payment flow.
 *
 * Responsibilities:
 * - create a new pending order
 * - reuse the latest pending order when still valid
 * - lazily mark expired pending orders as EXPIRED
 * - recalculate promo pricing on reused orders
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PromoCodeService promoCodeService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            PromoCodeService promoCodeService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.promoCodeService = promoCodeService;
    }

    /**
     * Creates a brand-new order or reuses the user's latest valid pending order
     * for the same course.
     */
    @Transactional
    public OrderResponse createOrReuseOrder(UUID userId, CreateOrderRequest request) {
        User user = getUserOrThrow(userId);
        Course course = getPublishedCourseOrThrow(request.courseId());

        ensureUserDoesNotAlreadyOwnCourse(userId, course.getId());

        Order reusableOrder = orderRepository
                .findLatestByUserIdAndCourseIdAndStatus(userId, course.getId(), Order.Status.PENDING)
                .orElse(null);

        if (reusableOrder != null && !markExpiredIfNeeded(reusableOrder)) {
            return updateReusableOrder(reusableOrder, course, request.promoCode());
        }

        return createNewOrder(user, course, request.promoCode());
    }

    /**
     * Returns a full order summary while also lazily expiring old pending
     * orders on read.
     */
    @Transactional
    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        markExpiredIfNeeded(order);
        return toOrderResponse(order, getOrderItemOrThrow(order.getId()));
    }

    /**
     * Used by frontend polling after 3DS or future QR flows.
     */
    @Transactional
    public OrderStatusResponse getOrderStatus(UUID userId, UUID orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        markExpiredIfNeeded(order);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        return OrderStatusResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .expiresAt(order.getExpiresAt())
                .build();
    }

    private OrderResponse createNewOrder(User user, Course course, String promoCodeInput) {
        BigDecimal subtotal = normalizeMoney(course.getPrice());
        PromoCode promoCode = promoCodeService.resolvePromoCodeForOrder(user.getId(), promoCodeInput, subtotal);
        BigDecimal discount = promoCodeService.calculateDiscountAmount(promoCode, subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .user(user)
                .promoCode(promoCode)
                .totalAmount(normalizeMoney(total))
                .status(Order.Status.PENDING)
                .expiresAt(newOrderExpiry())
                .build();
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .course(course)
                .priceAtPurchase(subtotal)
                .build();
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        return toOrderResponse(savedOrder, savedOrderItem);
    }

    private OrderResponse updateReusableOrder(Order order, Course course, String promoCodeInput) {
        OrderItem orderItem = getOrderItemOrThrow(order.getId());
        BigDecimal subtotal = normalizeMoney(orderItem.getPriceAtPurchase());

        PromoCode promoCode = promoCodeService.resolvePromoCodeForOrder(order.getUser().getId(), promoCodeInput, subtotal);
        BigDecimal discount = promoCodeService.calculateDiscountAmount(promoCode, subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        order.setPromoCode(promoCode);
        order.setTotalAmount(normalizeMoney(total));
        if (order.getExpiresAt() == null) {
            order.setExpiresAt(newOrderExpiry());
        }

        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, orderItem);
    }

    /**
     * Lazily converts expired pending orders into EXPIRED status. Returns true
     * when the order was updated during this check.
     */
    private boolean markExpiredIfNeeded(Order order) {
        if (!order.isExpiredAt(LocalDateTime.now())) {
            return false;
        }

        order.setStatus(Order.Status.EXPIRED);
        orderRepository.save(order);
        return true;
    }

    private OrderResponse toOrderResponse(Order order, OrderItem orderItem) {
        BigDecimal subtotal = normalizeMoney(orderItem.getPriceAtPurchase());
        BigDecimal total = normalizeMoney(order.getTotalAmount());
        BigDecimal discount = subtotal.subtract(total).max(BigDecimal.ZERO);

        OrderResponse.PromoCodeInfo promoCodeInfo = null;
        if (order.getPromoCode() != null) {
            promoCodeInfo = OrderResponse.PromoCodeInfo.builder()
                    .code(order.getPromoCode().getCode())
                    .discountType(order.getPromoCode().getDiscountType())
                    .discountValue(order.getPromoCode().getDiscountValue())
                    .build();
        }

        return OrderResponse.builder()
                .orderId(order.getId())
                .courseId(orderItem.getCourse().getId())
                .courseName(orderItem.getCourse().getTitle())
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .promoCode(promoCodeInfo)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    private void ensureUserDoesNotAlreadyOwnCourse(UUID userId, UUID courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (enrollment == null) {
            return;
        }

        if (enrollment.getStatus() != Enrollment.Status.UNSUBSCRIBED) {
            throw new IllegalArgumentException("User already has access to this course");
        }
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Course getPublishedCourseOrThrow(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (course.getStatus() != Course.Status.PUBLISHED) {
            throw new IllegalArgumentException("Course is not available for purchase");
        }

        if (course.getPrice() == null || course.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Course price is invalid");
        }

        return course;
    }

    private Order getOwnedOrderOrThrow(UUID userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private OrderItem getOrderItemOrThrow(UUID orderId) {
        return orderItemRepository.findFirstByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found for order: " + orderId));
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime newOrderExpiry() {
        return LocalDateTime.now().plus(60, ChronoUnit.MINUTES);
    }
}
