package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.MaterialProgress;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, UUID> {
    Optional<MaterialProgress> findByEnrollmentIdAndMaterialId(UUID enrollmentId, UUID materialId);

    List<MaterialProgress> findAllByEnrollmentId(UUID enrollmentId);

    @Modifying
    @Query("DELETE FROM MaterialProgress mp WHERE mp.enrollment.course.id = :courseId")
    void deleteByEnrollmentCourseId(@Param("courseId") UUID courseId);
}
