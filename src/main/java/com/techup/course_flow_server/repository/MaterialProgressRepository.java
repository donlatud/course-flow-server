package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.MaterialProgress;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, UUID> {
    Optional<MaterialProgress> findByEnrollmentIdAndMaterialId(UUID enrollmentId, UUID materialId);

    List<MaterialProgress> findAllByEnrollmentId(UUID enrollmentId);
}
