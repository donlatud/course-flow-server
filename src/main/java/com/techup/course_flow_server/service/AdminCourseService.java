package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse.ModuleResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse.SubLessonResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminSummaryResponse;
import com.techup.course_flow_server.dto.admin.course.CreateCourseRequest;
import com.techup.course_flow_server.dto.admin.course.CreateModuleRequest;
import com.techup.course_flow_server.dto.admin.course.CreateSubLessonRequest;
import com.techup.course_flow_server.dto.admin.course.UpdateCourseRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    // GET /api/admin/courses/exists — Check if a course title is already taken
    // -------------------------------------------------------------------------

    public boolean isTitleTaken(String title) {
        if (title == null || title.isBlank()) return false;
        return courseRepository.existsByTitleIgnoreCase(title.trim());
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/courses — Create a full course (modules + sub-lessons)
    // -------------------------------------------------------------------------

    @Transactional
    public CourseAdminDetailResponse createCourse(CreateCourseRequest request, UUID adminUserId) {
        validateUniqueNames(request);
        User admin = fetchAdmin(adminUserId);
        Course course = buildAndSaveCourse(request, admin);
        if (request.getPromoCode() != null) {
            savePromoCode(request);
        }
        List<ModuleResponse> moduleResponses = saveModulesAndMaterials(request.getModules(), course);
        return buildDetailResponse(course, moduleResponses);
    }

    private User fetchAdmin(UUID adminUserId) {
        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
        if (user.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        return user;
    }

    private Course buildAndSaveCourse(CreateCourseRequest request, User admin) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .detail(request.getDetail())
                .price(request.getPrice())
                .totalLearningTime(request.getTotalLearningTime())
                .coverImageUrl(blankToNull(request.getCoverImageUrl()))
                .trailerVideoUrl(blankToNull(request.getTrailerVideoUrl()))
                .attachmentUrl(blankToNull(request.getAttachmentUrl()))
                .status(Course.Status.DRAFT)
                .admin(admin)
                .build();
        return courseRepository.save(course);
    }

    private void savePromoCode(CreateCourseRequest request) {
        PromoCode promoCode = PromoCode.builder()
                .code(request.getPromoCode().getCode().trim().toUpperCase())
                .discountType(request.getPromoCode().getDiscountType())
                .discountValue(request.getPromoCode().getDiscountValue())
                .validFrom(request.getPromoCode().getValidFrom())
                .validUntil(request.getPromoCode().getValidUntil())
                .build();
        promoCodeRepository.save(promoCode);
    }

    private List<ModuleResponse> saveModulesAndMaterials(
            List<CreateModuleRequest> moduleRequests, Course course) {

        // 1. Build + saveAll modules in one batch
        List<CourseModule> moduleEntities = new ArrayList<>();
        for (int i = 0; i < moduleRequests.size(); i++) {
            moduleEntities.add(CourseModule.builder()
                    .course(course)
                    .title(moduleRequests.get(i).getTitle())
                    .orderIndex(i + 1)
                    .build());
        }
        List<CourseModule> savedModules = courseModuleRepository.saveAll(moduleEntities);

        // 2. Build all materials across all modules, then saveAll in one batch
        List<Material> allMaterials = new ArrayList<>();
        for (int moduleIdx = 0; moduleIdx < moduleRequests.size(); moduleIdx++) {
            CourseModule savedModule = savedModules.get(moduleIdx);
            List<CreateSubLessonRequest> subLessons = moduleRequests.get(moduleIdx).getSubLessons();
            for (int matIdx = 0; matIdx < subLessons.size(); matIdx++) {
                CreateSubLessonRequest subReq = subLessons.get(matIdx);
                allMaterials.add(Material.builder()
                        .module(savedModule)
                        .title(subReq.getTitle())
                        .fileType(subReq.getFileType())
                        .detail(subReq.getDetail())
                        .fileUrl(blankToNull(subReq.getMediaUrl()))
                        .orderIndex(matIdx + 1)
                        .build());
            }
        }
        List<Material> savedMaterials = materialRepository.saveAll(allMaterials);

        // 3. Group saved materials by moduleId to build responses
        Map<UUID, List<Material>> materialsByModule = savedMaterials.stream()
                .collect(Collectors.groupingBy(m -> m.getModule().getId()));

        return savedModules.stream()
                .map(module -> toModuleResponse(
                        module,
                        materialsByModule.getOrDefault(module.getId(), List.of())
                                .stream()
                                .map(this::toSubLessonResponse)
                                .toList()))
                .toList();
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
                            .map(this::toSubLessonResponse)
                            .toList();
                    return toModuleResponse(module, subs);
                })
                .toList();

        return buildDetailResponse(course, moduleResponses);
    }

    // -------------------------------------------------------------------------
    // PUT /api/admin/courses/{id} — Update course (replaces modules + materials)
    // -------------------------------------------------------------------------

    @Transactional
    public CourseAdminDetailResponse updateCourse(UUID courseId, UpdateCourseRequest request, UUID adminUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        fetchAdmin(adminUserId);

        if (course.getAdmin() != null && !course.getAdmin().getId().equals(adminUserId)) {
            throw new IllegalArgumentException("You are not authorized to update this course");
        }

        // Title uniqueness check only when title actually changed
        if (!course.getTitle().equalsIgnoreCase(request.getTitle())) {
            if (courseRepository.existsByTitleIgnoreCase(request.getTitle().trim())) {
                throw new IllegalArgumentException("Course name already exists");
            }
        }

        course.setTitle(request.getTitle().trim());
        course.setDescription(request.getDescription());
        course.setDetail(request.getDetail());
        course.setPrice(request.getPrice());
        course.setTotalLearningTime(request.getTotalLearningTime());
        course.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        course.setTrailerVideoUrl(blankToNull(request.getTrailerVideoUrl()));
        course.setAttachmentUrl(blankToNull(request.getAttachmentUrl()));
        courseRepository.save(course);

        // Replace all modules + materials
        materialRepository.deleteByCourseId(courseId);
        courseModuleRepository.deleteByCourseId(courseId);

        List<ModuleResponse> moduleResponses = (request.getModules() == null || request.getModules().isEmpty())
                ? List.of()
                : saveModulesAndMaterials(request.getModules(), course);

        return buildDetailResponse(course, moduleResponses);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/admin/courses/{id}
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteCourse(UUID courseId, UUID requestingAdminId) {
        fetchAdmin(requestingAdminId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (course.getAdmin() != null && !course.getAdmin().getId().equals(requestingAdminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this course");
        }

        // Bulk delete: materials first (FK constraint), then modules, then course
        materialRepository.deleteByCourseId(courseId);
        courseModuleRepository.deleteByCourseId(courseId);
        courseRepository.deleteById(courseId);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private SubLessonResponse toSubLessonResponse(Material material) {
        return SubLessonResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .fileType(material.getFileType())
                .detail(material.getDetail())
                .fileUrl(material.getFileUrl())
                .orderIndex(material.getOrderIndex())
                .build();
    }

    private ModuleResponse toModuleResponse(CourseModule module, List<SubLessonResponse> subLessons) {
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .orderIndex(module.getOrderIndex())
                .subLessons(subLessons)
                .build();
    }

    private CourseAdminDetailResponse buildDetailResponse(Course course, List<ModuleResponse> moduleResponses) {
        return CourseAdminDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .detail(course.getDetail())
                .price(course.getPrice())
                .totalLearningTime(course.getTotalLearningTime())
                .coverImageUrl(course.getCoverImageUrl())
                .trailerVideoUrl(course.getTrailerVideoUrl())
                .attachmentUrl(course.getAttachmentUrl())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .modules(moduleResponses)
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateUniqueNames(CreateCourseRequest request) {
        String normalizedCourseTitle = normalizeName(request.getTitle());
        if (normalizedCourseTitle != null && courseRepository.existsByTitleIgnoreCase(normalizedCourseTitle)) {
            throw new IllegalArgumentException("Course name already exists");
        }

        Set<String> lessonNames = new HashSet<>();
        for (CreateModuleRequest module : request.getModules()) {
            String lessonName = normalizeName(module.getTitle());
            if (lessonName == null) {
                continue;
            }
            if (!lessonNames.add(lessonName)) {
                throw new IllegalArgumentException("Lesson name must be unique within a course");
            }

            Set<String> subLessonNames = new HashSet<>();
            for (CreateSubLessonRequest subLesson : module.getSubLessons()) {
                String subLessonName = normalizeName(subLesson.getTitle());
                if (subLessonName == null) {
                    continue;
                }
                if (!subLessonNames.add(subLessonName)) {
                    throw new IllegalArgumentException(
                            "Sub-lesson name must be unique within the same lesson");
                }
            }
        }
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toLowerCase();
    }
}
