package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.entity.PromoCodeCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for promo code lookup and validation.
 */
public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = {"promoCodeCourses", "promoCodeCourses.course"})
    List<PromoCode> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = {"promoCodeCourses", "promoCodeCourses.course"})
    Optional<PromoCode> findWithCoursesById(UUID id);

    @Query("SELECT p FROM PromoCode p ORDER BY (SELECT COUNT(pc) FROM PromoCodeCourse pc WHERE pc.promoCode.id = p.id) ASC")
    Page<PromoCode> findAllOrderByCoursesCountAsc(Pageable pageable);

    @Query("SELECT p FROM PromoCode p ORDER BY (SELECT COUNT(pc) FROM PromoCodeCourse pc WHERE pc.promoCode.id = p.id) DESC")
    Page<PromoCode> findAllOrderByCoursesCountDesc(Pageable pageable);

    @Query(value = "SELECT * FROM promo_codes ORDER BY CASE WHEN minimum_purchase_amount IS NULL THEN 1 ELSE 0 END, CAST(minimum_purchase_amount AS NUMERIC) ASC", nativeQuery = true)
    Page<PromoCode> findAllOrderByMinimumPurchaseAsc(Pageable pageable);

    @Query(value = "SELECT * FROM promo_codes ORDER BY CASE WHEN minimum_purchase_amount IS NULL THEN 0 ELSE 1 END, CAST(minimum_purchase_amount AS NUMERIC) DESC", nativeQuery = true)
    Page<PromoCode> findAllOrderByMinimumPurchaseDesc(Pageable pageable);

    @Query(value = "SELECT * FROM promo_codes ORDER BY discount_type ASC", nativeQuery = true)
    Page<PromoCode> findAllOrderByDiscountTypeAsc(Pageable pageable);

    @Query(value = "SELECT * FROM promo_codes ORDER BY discount_type DESC", nativeQuery = true)
    Page<PromoCode> findAllOrderByDiscountTypeDesc(Pageable pageable);
}
