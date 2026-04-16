package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.dto.enrollment.EnrollmentResponse;
import com.techup.course_flow_server.entity.Enrollment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    @Query("""
        SELECT new com.techup.course_flow_server.dto.enrollment.EnrollmentResponse(
            e.id, 
            c.id, 
            c.title, 
            c.description, 
            c.coverImageUrl, 
            e.progressPercentage, 
            e.status,
            c.totalLearningTime,
            (SELECT COUNT(m) FROM CourseModule m WHERE m.course.id = c.id)
        )
        FROM Enrollment e
        JOIN e.course c
        WHERE e.user.id = :userId
    """)
    List<EnrollmentResponse> findEnrollmentResponsesByUserId(@Param("userId") UUID userId);
    List<Enrollment> findByUserId(UUID userId);
    List<Enrollment> findByUserIdAndStatus(UUID userId, Enrollment.Status status);
    List<Enrollment> findAllByUserIdAndStatusIn(UUID userId, List<Enrollment.Status> statuses);
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);
}