package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoCodeCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoCodeCourseRepository extends JpaRepository<PromoCodeCourse, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PromoCodeCourse pcc WHERE pcc.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT pcc.promoCode.id FROM PromoCodeCourse pcc WHERE pcc.course.id = :courseId")
    List<UUID> findPromoCodeIdsByCourseId(@Param("courseId") UUID courseId);
}