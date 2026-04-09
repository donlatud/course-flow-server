package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Course;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findAllByOrderByCreatedAtDesc();

    boolean existsByTitleIgnoreCase(String title);
    List<Course> findByCategory(String category);
    List<Course> findByStatus(Course.Status status);
    List<Course> findByAdminId(UUID adminId);
    
    // Query to get published courses
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' ORDER BY c.createdAt DESC")
    List<Course> findPublishedCourses();

    // Query to count modules for specific courses
    @Query("SELECT c.id, COUNT(cm) FROM Course c LEFT JOIN c.modules cm WHERE c.id IN :courseIds GROUP BY c.id")
    List<Object[]> countModulesByCourseIds(@Param("courseIds") List<UUID> courseIds);
}
