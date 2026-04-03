package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.CourseModule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findAllByCourseIdOrderByOrderIndexAsc(UUID courseId);

    /**
     * Returns pairs of [courseId, lessonCount] for the given course IDs.
     * Used to build the admin course list without N+1 queries.
     */
    @Query("SELECT m.course.id, COUNT(m) FROM CourseModule m WHERE m.course.id IN :courseIds GROUP BY m.course.id")
    List<Object[]> countByCourseIdIn(@Param("courseIds") List<UUID> courseIds);

    /**
     * Bulk-deletes all modules for a course in a single SQL statement.
     * Call only after materials have already been deleted.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CourseModule cm WHERE cm.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
