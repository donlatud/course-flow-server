package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Course;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findAllByOrderByCreatedAtDesc();

    List<Course> findByCategory(String category);

    List<Course> findByStatus(Course.Status status);

    List<Course> findByAdminId(UUID adminId);

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' ORDER BY c.createdAt DESC")
    List<Course> findPublishedCourses();

    @Query("SELECT c.id, COUNT(cm) FROM Course c LEFT JOIN c.modules cm WHERE c.id IN :courseIds GROUP BY c.id")
    List<Object[]> countModulesByCourseIds(@Param("courseIds") List<UUID> courseIds);

    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    boolean existsByTitleIgnoreCase(String title);

    @Query(
            "SELECT c FROM Course c WHERE "
                    + "(:search IS NULL OR :search = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) "
                    + "ORDER BY (SELECT COUNT(m) FROM CourseModule m WHERE m.course.id = c.id) ASC")
    Page<Course> findAllOrderByLessonCountAsc(@Param("search") String search, Pageable pageable);

    @Query(
            "SELECT c FROM Course c WHERE "
                    + "(:search IS NULL OR :search = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) "
                    + "ORDER BY (SELECT COUNT(m) FROM CourseModule m WHERE m.course.id = c.id) DESC")
    Page<Course> findAllOrderByLessonCountDesc(@Param("search") String search, Pageable pageable);
}
