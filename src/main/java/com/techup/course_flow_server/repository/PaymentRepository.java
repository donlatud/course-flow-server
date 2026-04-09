package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for payment records associated with orders.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByPaymentGatewayRef(String paymentGatewayRef);
}
