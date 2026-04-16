package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Assignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findAllByCourseIdOrderByStartDateAsc(UUID courseId);

    List<Assignment> findAllByOrderByStartDateDesc();

    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.module LEFT JOIN FETCH a.material WHERE a.id = :assignmentId")
    Optional<Assignment> findWithCourseById(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.module LEFT JOIN FETCH a.material ORDER BY a.startDate DESC")
    List<Assignment> findAllWithDetailsOrderByStartDateDesc();

    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.module LEFT JOIN FETCH a.material")
    List<Assignment> findAllWithDetails(Sort sort);

    // Custom query for pagination
    @Query("SELECT a FROM Assignment a")
    Page<Assignment> findAllWithPagination(Pageable pageable);

    // Search by title (case-insensitive, contains)
    @Query("SELECT a FROM Assignment a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Assignment> findByTitleContainingIgnoreCase(@Param("search") String search, Pageable pageable);
}
