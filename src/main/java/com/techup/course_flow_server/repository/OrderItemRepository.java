package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.OrderItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for order items. In the first phase one order is expected to have
 * one item, but the schema remains extensible for multi-course orders later.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findFirstByOrderId(UUID orderId);

    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
