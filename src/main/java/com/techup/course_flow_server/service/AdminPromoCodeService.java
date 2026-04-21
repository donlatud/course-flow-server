package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.admin.promo.AdminPromoCodeDetailResponse;
import com.techup.course_flow_server.dto.admin.promo.AdminPromoCodeListItemResponse;
import com.techup.course_flow_server.dto.admin.promo.AdminUpsertPromoCodeRequest;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.entity.PromoCodeCourse;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.PromoCodeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPromoCodeService {

    private static final String DELETE_LINKS_SQL = "DELETE FROM promo_code_course WHERE promo_code_id = ?";
    private static final String INSERT_LINK_SQL =
            "INSERT INTO promo_code_course (id, promo_code_id, course_id, created_at, updated_at) "
                    + "VALUES (?, ?, ?, NOW(), NOW())";

    private final PromoCodeRepository promoCodeRepository;
    private final CourseRepository courseRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminPromoCodeService(
            PromoCodeRepository promoCodeRepository,
            CourseRepository courseRepository,
            JdbcTemplate jdbcTemplate) {
        this.promoCodeRepository = promoCodeRepository;
        this.courseRepository = courseRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminPromoCodeListItemResponse> listPromoCodes() {
        long totalCourses = courseRepository.count();
        return promoCodeRepository.findAllByOrderByCodeAsc().stream()
                .map(promo -> toListItem(promo, totalCourses))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminPromoCodeListItemResponse> listPromoCodesPaginated(int page, int size, String sortBy, String sortDir) {
        long totalCourses = courseRepository.count();
        String key = sortBy == null ? "createdAt" : sortBy.trim();
        boolean asc = "asc".equalsIgnoreCase(sortDir);

        Page<PromoCode> promoPage;
        if ("coursesincludedlength".equalsIgnoreCase(key)) {
            Pageable unpagedSort = PageRequest.of(page, size);
            promoPage = asc 
                    ? promoCodeRepository.findAllOrderByCoursesCountAsc(unpagedSort)
                    : promoCodeRepository.findAllOrderByCoursesCountDesc(unpagedSort);
        } else {
            String property = mapPromoCodeSortProperty(key);
            Sort.Direction direction = asc ? Sort.Direction.ASC : Sort.Direction.DESC;

            if ("minimumpurchaseamount".equals(key)) {
                Pageable pageable = PageRequest.of(page, size);
                promoPage = asc
                        ? promoCodeRepository.findAllOrderByMinimumPurchaseAsc(pageable)
                        : promoCodeRepository.findAllOrderByMinimumPurchaseDesc(pageable);
            } else if ("discounttype".equals(key)) {
                Pageable pageable = PageRequest.of(page, size);
                promoPage = asc
                        ? promoCodeRepository.findAllOrderByDiscountTypeAsc(pageable)
                        : promoCodeRepository.findAllOrderByDiscountTypeDesc(pageable);
            } else {
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
                promoPage = promoCodeRepository.findAll(pageable);
            }
        }

        List<AdminPromoCodeListItemResponse> content = promoPage.getContent().stream()
                .map(promo -> toListItem(promo, totalCourses))
                .toList();
        return new PageImpl<>(content, promoPage.getPageable(), promoPage.getTotalElements());
    }

    private String mapPromoCodeSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "code" -> "code";
            case "minimumpurchaseamount" -> "minimumPurchaseAmount";
            case "discounttype" -> "discountType";
            case "createdat" -> "createdAt";
            case "coursesincludedlength" -> "promoCodeCourses";
            default -> "createdAt";
        };
    }

    @Transactional(readOnly = true)
    public AdminPromoCodeDetailResponse getPromoCode(UUID id) {
        return promoCodeRepository
                .findWithCoursesById(id)
                .map(this::toDetail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
    }

    @Transactional
    public AdminPromoCodeDetailResponse create(AdminUpsertPromoCodeRequest request) {
        validateDiscount(request);
        String normalized = request.getCode().trim().toUpperCase();
        if (promoCodeRepository.findByCodeIgnoreCase(normalized).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promo code already exists");
        }
        PromoCode entity = PromoCode.builder()
                .code(normalized)
                .discountType(request.getDiscountType())
                .discountValue(scaleDiscount(request.getDiscountValue()))
                .minimumPurchaseAmount(request.getMinimumPurchaseAmount())
                .build();
        PromoCode saved = promoCodeRepository.save(entity);
        promoCodeRepository.flush();

        List<UUID> courseIds = courseIdsOrEmpty(request);
        if (courseIds != null && !courseIds.isEmpty()) {
            replaceCourseMappings(saved, courseIds);
        }

        return promoCodeRepository
                .findWithCoursesById(saved.getId())
                .map(this::toDetail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load created promo code"));
    }

    @Transactional
    public AdminPromoCodeDetailResponse update(UUID id, AdminUpsertPromoCodeRequest request) {
        PromoCode promo = promoCodeRepository
                .findWithCoursesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
        validateDiscount(request);
        String normalized = request.getCode().trim().toUpperCase();
        Optional<PromoCode> otherWithCode = promoCodeRepository.findByCodeIgnoreCase(normalized);
        if (otherWithCode.isPresent() && !otherWithCode.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promo code already exists");
        }
        replaceCourseMappings(promo, courseIdsOrEmpty(request));
        promo.setCode(normalized);
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(scaleDiscount(request.getDiscountValue()));
        promo.setMinimumPurchaseAmount(request.getMinimumPurchaseAmount());
        promoCodeRepository.save(promo);
        return promoCodeRepository
                .findWithCoursesById(id)
                .map(this::toDetail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load updated promo code"));
    }

    private static List<UUID> courseIdsOrEmpty(AdminUpsertPromoCodeRequest request) {
        List<UUID> ids = request.getCourseIds();
        return ids == null ? List.of() : ids;
    }

    private static BigDecimal scaleDiscount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDiscount(AdminUpsertPromoCodeRequest promoRequest) {
        BigDecimal value = promoRequest.getDiscountValue();
        if (value == null || promoRequest.getDiscountType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount type and value are required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount value must be at least 0");
        }
        if (promoRequest.getDiscountType() == PromoCode.DiscountType.PERCENTAGE
                && value.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Percent discount must be between 0 and 100");
        }
    }

    /**
     * Rewrites promo↔course links using JDBC so DELETE always hits the database before INSERTs.
     * Hibernate batched INSERT/DELETE ordering was still producing uk_promo_code_course duplicates.
     */
    private void replaceCourseMappings(PromoCode promo, List<UUID> courseIds) {
        List<UUID> uniqueIds =
                courseIds == null ? List.of() : courseIds.stream().distinct().toList();

        jdbcTemplate.update(DELETE_LINKS_SQL, promo.getId());
        promo.getPromoCodeCourses().clear();

        if (uniqueIds.isEmpty()) {
            return;
        }

        List<Course> courses = courseRepository.findAllById(uniqueIds);
        if (courses.size() != new HashSet<>(uniqueIds).size()) {
            throw new IllegalArgumentException("One or more courseIds are invalid");
        }

        Map<UUID, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a, LinkedHashMap::new));

        for (UUID courseId : uniqueIds) {
            Course course = courseById.get(courseId);
            if (course == null) {
                throw new IllegalArgumentException("One or more courseIds are invalid");
            }
            UUID rowId = UUID.randomUUID();
            jdbcTemplate.update(INSERT_LINK_SQL, rowId, promo.getId(), course.getId());
        }
    }

    private AdminPromoCodeListItemResponse toListItem(PromoCode promo, long totalCoursesInDb) {
        List<String> titles = promo.getPromoCodeCourses().stream()
                .map(PromoCodeCourse::getCourse)
                .map(Course::getTitle)
                .sorted(Comparator.naturalOrder())
                .toList();
        int linked = titles.size();
        boolean allCourses = totalCoursesInDb > 0 && linked == totalCoursesInDb;
        return new AdminPromoCodeListItemResponse(
                promo.getId(),
                promo.getCode(),
                promo.getMinimumPurchaseAmount(),
                promo.getDiscountType(),
                allCourses,
                allCourses ? List.of() : titles,
                linked,
                promo.getCreatedAt());
    }

    private AdminPromoCodeDetailResponse toDetail(PromoCode promo) {
        List<UUID> courseIds = promo.getPromoCodeCourses().stream()
                .map(pcc -> pcc.getCourse().getId())
                .sorted()
                .toList();
        return new AdminPromoCodeDetailResponse(
                promo.getId(),
                promo.getCode(),
                promo.getMinimumPurchaseAmount(),
                promo.getDiscountType(),
                promo.getDiscountValue(),
                courseIds);
    }

    @Transactional
    public void delete(UUID id) {
        PromoCode promo = promoCodeRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
        jdbcTemplate.update(DELETE_LINKS_SQL, id);
        promoCodeRepository.delete(promo);
    }
}
