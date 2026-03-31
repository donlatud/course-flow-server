package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.CourseModule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
    List<CourseModule> findAllByCourseIdOrderByOrderIndexAsc(UUID courseId);
}
