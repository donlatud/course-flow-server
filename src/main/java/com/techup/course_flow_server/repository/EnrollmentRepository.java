package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Enrollment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
}
