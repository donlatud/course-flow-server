package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoCodeCourse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeCourseRepository extends JpaRepository<PromoCodeCourse, UUID> {}
