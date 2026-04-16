package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    List<AssignmentSubmission> findAllByAssignmentIdInAndUserId(List<UUID> assignmentIds, UUID userId);

    @Query("""
            SELECT COUNT(s)
            FROM AssignmentSubmission s
            WHERE s.assignment.course.id = :courseId
              AND s.user.id = :userId
              AND s.status = :status
            """)
    long countByAssignmentCourseIdAndUserIdAndStatus(
            @Param("courseId") UUID courseId,
            @Param("userId") UUID userId,
            @Param("status") AssignmentSubmission.Status status);

    long countByAssignment_Course_IdAndUserIdAndStatus(
            UUID courseId,
            UUID userId,
            AssignmentSubmission.Status status);

    @Query("""
            SELECT COUNT(s)
            FROM AssignmentSubmission s
            WHERE s.assignment.course.id = :courseId
              AND s.user.id = :userId
              AND s.status IN :statuses
            """)
    long countByAssignmentCourseIdAndUserIdAndStatusIn(
            @Param("courseId") UUID courseId,
            @Param("userId") UUID userId,
            @Param("statuses") List<AssignmentSubmission.Status> statuses);

    @Query("""
            SELECT s.assignment.course.id, COUNT(s)
            FROM AssignmentSubmission s
            WHERE s.assignment.course.id IN :courseIds
              AND s.user.id = :userId
              AND s.status IN :statuses
            GROUP BY s.assignment.course.id
            """)
    List<Object[]> countByCourseIdInAndUserIdAndStatusIn(
            @Param("courseIds") List<UUID> courseIds,
            @Param("userId") UUID userId,
            @Param("statuses") List<AssignmentSubmission.Status> statuses);
}
