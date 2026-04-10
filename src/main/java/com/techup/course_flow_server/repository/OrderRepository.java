package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * All pending orders for this user and course (e.g. to expire every stale row
     * before inserting another PENDING when the DB enforces at most one active checkout).
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
    List<Order> findAllByUserIdAndCourseIdAndStatus(
            @Param("userId") UUID userId,
            @Param("courseId") UUID courseId,
            @Param("status") Order.Status status
    );

    /**
     * Direct bulk PENDING → EXPIRED for the given ids (caller filters with
     * {@link com.techup.course_flow_server.entity.Order#isExpiredAt}). {@code
     * clearAutomatically} evicts the persistence context so the next query sees
     * DB rows, not stale managed entities.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.status = :expiredStatus
            WHERE o.id IN :ids
              AND o.status = :pendingStatus
            """)
    int expirePendingByIds(
            @Param("ids") List<UUID> ids,
            @Param("pendingStatus") Order.Status pendingStatus,
            @Param("expiredStatus") Order.Status expiredStatus
    );

    /**
     * Convenience lookup for endpoints that must ensure the current user owns
     * the requested order.
     */
    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);
}
