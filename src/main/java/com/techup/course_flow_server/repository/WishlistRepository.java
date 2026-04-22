package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Wishlist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByUserId(UUID userId);
    Optional<Wishlist> findByUserIdAndCourseId(UUID userId, UUID courseId);
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);
    void deleteByUserIdAndCourseId(UUID userId, UUID courseId);
}