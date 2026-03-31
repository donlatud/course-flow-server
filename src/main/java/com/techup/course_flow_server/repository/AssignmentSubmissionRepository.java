package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    List<AssignmentSubmission> findAllByAssignmentIdInAndUserId(List<UUID> assignmentIds, UUID userId);
}
