package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for promo code lookup and validation.
 */
public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = {"promoCodeCourses", "promoCodeCourses.course"})
    List<PromoCode> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = {"promoCodeCourses", "promoCodeCourses.course"})
    Optional<PromoCode> findWithCoursesById(UUID id);
}
