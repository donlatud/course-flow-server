package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Material;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    List<Material> findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(List<UUID> moduleIds);

    List<Material> findAllByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    /**
     * Bulk-deletes all materials belonging to a course in a single SQL statement,
     * without loading any entities into memory.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Material m WHERE m.module.id IN (SELECT cm.id FROM CourseModule cm WHERE cm.course.id = :courseId)")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
