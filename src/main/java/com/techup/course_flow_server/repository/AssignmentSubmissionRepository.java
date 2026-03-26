package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.AssignmentSubmission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
}
