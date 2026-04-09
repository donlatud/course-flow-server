package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.course.CourseDetailResponse;
import com.techup.course_flow_server.dto.course.CourseResponse;
import com.techup.course_flow_server.dto.course.CourseWithLessonCountResponse;
import com.techup.course_flow_server.dto.module.MaterialSummaryResponse;
import com.techup.course_flow_server.dto.module.ModuleWithMaterialsResponse;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;

    public CourseService(CourseRepository courseRepository, 
                         CourseModuleRepository courseModuleRepository,
                         MaterialRepository materialRepository) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    public CourseResponse getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        return convertToResponse(course);
    }

    /**
     * Get course by ID with modules and materials.
     * Only returns data if the course is published.
     */
    public CourseDetailResponse getCourseWithModulesAndMaterialsById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        // Only allow access to published courses
        if (course.getStatus() != Course.Status.PUBLISHED) {
            throw new RuntimeException("Course not found or not published");
        }

        // Get modules with materials
        List<ModuleWithMaterialsResponse> modules = getModulesWithMaterials(course.getId());

        // Count total lessons (modules)
        int lessonCount = modules.size();

        return convertToDetailResponse(course, modules, lessonCount);
    }

    private List<ModuleWithMaterialsResponse> getModulesWithMaterials(UUID courseId) {
        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);
        
        if (modules.isEmpty()) {
            return List.of();
        }

        List<UUID> moduleIds = modules.stream()
                .map(CourseModule::getId)
                .collect(Collectors.toList());

        List<Material> materials = materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);

        Map<UUID, List<Material>> materialsByModuleId = materials.stream()
                .collect(Collectors.groupingBy(material -> material.getModule().getId()));

        return modules.stream()
                .map(module -> buildModuleWithMaterialsResponse(module, materialsByModuleId.getOrDefault(module.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private ModuleWithMaterialsResponse buildModuleWithMaterialsResponse(CourseModule module, List<Material> materials) {
        List<MaterialSummaryResponse> materialResponses = materials.stream()
                .map(this::buildMaterialSummaryResponse)
                .collect(Collectors.toList());

        return ModuleWithMaterialsResponse.builder()
                .moduleId(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .materials(materialResponses)
                .build();
    }

    private MaterialSummaryResponse buildMaterialSummaryResponse(Material material) {
        return MaterialSummaryResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .orderIndex(material.getOrderIndex())
                .fileUrl(material.getFileUrl())
                .detail(material.getDetail())
                .fileType(material.getFileType())
                .duration(material.getDuration())
                .build();
    }

    private CourseDetailResponse convertToDetailResponse(Course course, List<ModuleWithMaterialsResponse> modules, int lessonCount) {
        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .detail(course.getDetail())
                .price(course.getPrice())
                .category(course.getCategory())
                .subject(course.getSubject())
                .status(course.getStatus())
                .adminId(course.getAdmin() != null ? course.getAdmin().getId() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .coverImageUrl(course.getCoverImageUrl())
                .trailerVideoUrl(course.getTrailerVideoUrl())
                .attachmentUrl(course.getAttachmentUrl())
                .totalLearningTime(course.getTotalLearningTime())
                .lessonCount(lessonCount)
                .modules(modules)
                .build();
    }

    public List<CourseResponse> getCoursesByCategory(String category) {
        return courseRepository.findByCategory(category).stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<CourseWithLessonCountResponse> getPublishedCourses() {
        // Fetch all published courses
        List<Course> courses = courseRepository.findPublishedCourses();
        
        if (courses.isEmpty()) {
            return List.of();
        }
        
        // Get all course IDs
        List<UUID> courseIds = courses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());
        
        // Count modules for each course
        List<Object[]> counts = courseRepository.countModulesByCourseIds(courseIds);
        Map<UUID, Long> lessonCountMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a
                ));
        
        // Build response
        return courses.stream()
                .map(course -> convertToWithLessonCountResponse(course, lessonCountMap.getOrDefault(course.getId(), 0L).intValue()))
                .toList();
    }

    public List<CourseWithLessonCountResponse> getPublishedCoursesWithLessonCount() {
        return getPublishedCourses();
    }

    private CourseResponse convertToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .category(course.getCategory())
                .subject(course.getSubject())
                .status(course.getStatus())
                .adminId(course.getAdmin() != null ? course.getAdmin().getId() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .coverImageUrl(course.getCoverImageUrl())
                .trailerVideoUrl(course.getTrailerVideoUrl())
                .attachmentUrl(course.getAttachmentUrl())
                .totalLearningTime(course.getTotalLearningTime())
                .build();
    }

    private CourseWithLessonCountResponse convertToWithLessonCountResponse(Course course, int lessonCount) {
        return CourseWithLessonCountResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .category(course.getCategory())
                .subject(course.getSubject())
                .status(course.getStatus())
                .adminId(course.getAdmin() != null ? course.getAdmin().getId() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .coverImageUrl(course.getCoverImageUrl())
                .trailerVideoUrl(course.getTrailerVideoUrl())
                .attachmentUrl(course.getAttachmentUrl())
                .totalLearningTime(course.getTotalLearningTime())
                .lessonCount(lessonCount)
                .build();
    }
}
