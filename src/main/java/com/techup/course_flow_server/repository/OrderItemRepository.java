package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.OrderItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for order items. In the first phase one order is expected to have
 * one item, but the schema remains extensible for multi-course orders later.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findFirstByOrderId(UUID orderId);
}
