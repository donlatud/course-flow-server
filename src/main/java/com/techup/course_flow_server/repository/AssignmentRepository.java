package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Assignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findAllByCourseIdOrderByStartDateAsc(UUID courseId);
}
