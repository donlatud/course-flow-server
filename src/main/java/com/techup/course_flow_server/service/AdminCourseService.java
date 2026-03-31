package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse.ModuleResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse.SubLessonResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminSummaryResponse;
import com.techup.course_flow_server.dto.admin.course.CreateCourseRequest;
import com.techup.course_flow_server.dto.admin.course.CreateModuleRequest;
import com.techup.course_flow_server.dto.admin.course.CreateSubLessonRequest;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import com.techup.course_flow_server.repository.PromoCodeRepository;
import com.techup.course_flow_server.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;

    public AdminCourseService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            MaterialRepository materialRepository,
            PromoCodeRepository promoCodeRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/courses — Create a full course (modules + sub-lessons)
    // -------------------------------------------------------------------------

    @Transactional
    public CourseAdminDetailResponse createCourse(CreateCourseRequest request, UUID adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .detail(request.getDetail())
                .price(request.getPrice())
                .totalLearningTime(request.getTotalLearningTime())
                .status(Course.Status.DRAFT)
                .admin(admin)
                .build();

        course = courseRepository.save(course);

        // Optional promo code — present only when UI promoEnabled is true
        if (request.getPromoCode() != null) {
            PromoCode promoCode = PromoCode.builder()
                    .code(request.getPromoCode().getCode().trim().toUpperCase())
                    .discountType(request.getPromoCode().getDiscountType())
                    .discountValue(request.getPromoCode().getDiscountValue())
                    .validFrom(request.getPromoCode().getValidFrom())
                    .validUntil(request.getPromoCode().getValidUntil())
                    .build();
            promoCodeRepository.save(promoCode);
        }

        // Persist lessons (modules) and sub-lessons (materials) in order
        List<ModuleResponse> moduleResponses = new ArrayList<>();
        for (int moduleIdx = 0; moduleIdx < request.getModules().size(); moduleIdx++) {
            CreateModuleRequest moduleReq = request.getModules().get(moduleIdx);

            CourseModule module = CourseModule.builder()
                    .course(course)
                    .title(moduleReq.getTitle())
                    .orderIndex(moduleIdx + 1)
                    .build();

            module = courseModuleRepository.save(module);

            List<SubLessonResponse> subLessonResponses = new ArrayList<>();
            for (int matIdx = 0; matIdx < moduleReq.getSubLessons().size(); matIdx++) {
                CreateSubLessonRequest subReq = moduleReq.getSubLessons().get(matIdx);

                Material material = Material.builder()
                        .module(module)
                        .title(subReq.getTitle())
                        .fileType(subReq.getFileType())
                        .detail(subReq.getDetail())
                        .orderIndex(matIdx + 1)
                        .build();

                material = materialRepository.save(material);

                subLessonResponses.add(SubLessonResponse.builder()
                        .id(material.getId())
                        .title(material.getTitle())
                        .fileType(material.getFileType())
                        .detail(material.getDetail())
                        .orderIndex(material.getOrderIndex())
                        .build());
            }

            moduleResponses.add(ModuleResponse.builder()
                    .id(module.getId())
                    .title(module.getTitle())
                    .orderIndex(module.getOrderIndex())
                    .subLessons(subLessonResponses)
                    .build());
        }

        return buildDetailResponse(course, moduleResponses);
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/courses — List all courses for the admin table
    // -------------------------------------------------------------------------

    public List<CourseAdminSummaryResponse> listCourses() {
        List<Course> courses = courseRepository.findAllByOrderByCreatedAtDesc();
        if (courses.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = courses.stream().map(Course::getId).toList();

        // Single query: returns [courseId, count] pairs
        Map<UUID, Long> lessonCountMap = courseModuleRepository.countByCourseIdIn(courseIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return courses.stream()
                .map(c -> CourseAdminSummaryResponse.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .lessonCount(lessonCountMap.getOrDefault(c.getId(), 0L).intValue())
                        .price(c.getPrice())
                        .coverImageUrl(c.getCoverImageUrl())
                        .status(c.getStatus())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/courses/{id} — Get single course detail
    // -------------------------------------------------------------------------

    public CourseAdminDetailResponse getCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);

        List<UUID> moduleIds = modules.stream().map(CourseModule::getId).toList();

        Map<UUID, List<Material>> materialsByModule = moduleIds.isEmpty()
                ? Map.of()
                : materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds)
                        .stream()
                        .collect(Collectors.groupingBy(m -> m.getModule().getId()));

        List<ModuleResponse> moduleResponses = modules.stream()
                .map(module -> {
                    List<SubLessonResponse> subs = materialsByModule
                            .getOrDefault(module.getId(), List.of())
                            .stream()
                            .map(mat -> SubLessonResponse.builder()
                                    .id(mat.getId())
                                    .title(mat.getTitle())
                                    .fileType(mat.getFileType())
                                    .detail(mat.getDetail())
                                    .orderIndex(mat.getOrderIndex())
                                    .build())
                            .toList();

                    return ModuleResponse.builder()
                            .id(module.getId())
                            .title(module.getTitle())
                            .orderIndex(module.getOrderIndex())
                            .subLessons(subs)
                            .build();
                })
                .toList();

        return buildDetailResponse(course, moduleResponses);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/admin/courses/{id}
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteCourse(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found: " + courseId);
        }

        // Materials and modules are deleted via cascading in the service layer
        // (no CascadeType on entity, so we delete explicitly)
        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);
        List<UUID> moduleIds = modules.stream().map(CourseModule::getId).toList();

        if (!moduleIds.isEmpty()) {
            List<Material> materials =
                    materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);
            materialRepository.deleteAll(materials);
            courseModuleRepository.deleteAll(modules);
        }

        courseRepository.deleteById(courseId);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private CourseAdminDetailResponse buildDetailResponse(Course course, List<ModuleResponse> moduleResponses) {
        return CourseAdminDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .detail(course.getDetail())
                .price(course.getPrice())
                .totalLearningTime(course.getTotalLearningTime())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .modules(moduleResponses)
                .build();
    }
}
