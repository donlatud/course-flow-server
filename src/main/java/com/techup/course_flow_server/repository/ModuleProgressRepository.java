package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.ModuleProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, UUID> {
    List<ModuleProgress> findAllByEnrollmentId(UUID enrollmentId);

    Optional<ModuleProgress> findByEnrollmentIdAndModuleId(UUID enrollmentId, UUID moduleId);

    @Modifying
    @Query("DELETE FROM ModuleProgress mp WHERE mp.enrollment.course.id = :courseId")
    void deleteByEnrollmentCourseId(@Param("courseId") UUID courseId);
}
