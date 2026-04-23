package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    @Modifying
    @Query("DELETE FROM Review r WHERE r.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}