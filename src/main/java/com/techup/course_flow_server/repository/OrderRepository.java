package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for order aggregates used by the payment flow.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds the latest pending order for a given user and course so the service
     * can reuse it when it is still within the 60-minute validity window.
     */
    @Query("""
            SELECT o
            FROM Order o
            JOIN OrderItem oi ON oi.order = o
            WHERE o.user.id = :userId
              AND oi.course.id = :courseId
              AND o.status = :status
            ORDER BY o.createdAt DESC
            """)
    Optional<Order> findLatestByUserIdAndCourseIdAndStatus(
            @Param("userId") UUID userId,
            @Param("courseId") UUID courseId,
            @Param("status") Order.Status status
    );

    /**
     * Convenience lookup for endpoints that must ensure the current user owns
     * the requested order.
     */
    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);
}
