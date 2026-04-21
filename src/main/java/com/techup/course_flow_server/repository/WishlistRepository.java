package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Wishlist;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}