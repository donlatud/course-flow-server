package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.courselearning.CourseLearningResponse;
import com.techup.course_flow_server.dto.courselearning.MaterialLearningResponse;
import com.techup.course_flow_server.dto.courselearning.ModuleLearningResponse;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.MaterialProgress;
import com.techup.course_flow_server.entity.ModuleProgress;
import com.techup.course_flow_server.mapper.MaterialLearningMapper;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import com.techup.course_flow_server.repository.MaterialProgressRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import com.techup.course_flow_server.repository.ModuleProgressRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CourseLearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;
    private final MaterialProgressRepository materialProgressRepository;
    private final ModuleProgressRepository moduleProgressRepository;
    private final MaterialLearningMapper materialLearningMapper;

    public CourseLearningService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            MaterialRepository materialRepository,
            MaterialProgressRepository materialProgressRepository,
            ModuleProgressRepository moduleProgressRepository,
            MaterialLearningMapper materialLearningMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
        this.materialProgressRepository = materialProgressRepository;
        this.moduleProgressRepository = moduleProgressRepository;
        this.materialLearningMapper = materialLearningMapper;
    }

    public CourseLearningResponse getCourseLearning(UUID courseId, UUID userId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for this user and course"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);

        List<UUID> moduleIds = modules.stream().map(CourseModule::getId).toList();
        List<Material> materials = moduleIds.isEmpty()
                ? Collections.emptyList()
                : materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);

        Map<UUID, List<Material>> materialsByModuleId = materials.stream()
                .collect(Collectors.groupingBy(material -> material.getModule().getId()));

        Map<UUID, MaterialProgress> progressByMaterialId = materialProgressRepository
                .findAllByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(mp -> mp.getMaterial().getId(), Function.identity()));

        Map<UUID, ModuleProgress> moduleProgressByModuleId = moduleProgressRepository
                .findAllByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(mp -> mp.getModule().getId(), Function.identity()));

        List<ModuleLearningResponse> moduleResponses = modules.stream()
                .map(module -> mapModule(module, materialsByModuleId, progressByMaterialId, moduleProgressByModuleId))
                .toList();

        int totalMaterials = materials.size();
        int completedMaterials = (int) materials.stream()
                .filter(material -> {
                    MaterialProgress progress = progressByMaterialId.get(material.getId());
                    return progress != null && progress.getStatus() == MaterialProgress.Status.COMPLETED;
                })
                .count();
        int inProgressMaterials = (int) materials.stream()
                .filter(material -> {
                    MaterialProgress progress = progressByMaterialId.get(material.getId());
                    return progress != null && progress.getStatus() == MaterialProgress.Status.IN_PROGRESS;
                })
                .count();

        BigDecimal progressPercentage = toPercentage(completedMaterials, totalMaterials);

        return CourseLearningResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseDescription(course.getDescription())
                .totalMaterials(totalMaterials)
                .completedMaterials(completedMaterials)
                .inProgressMaterials(inProgressMaterials)
                .progressPercentage(progressPercentage)
                .modules(moduleResponses)
                .build();
    }

    private ModuleLearningResponse mapModule(
            CourseModule module,
            Map<UUID, List<Material>> materialsByModuleId,
            Map<UUID, MaterialProgress> progressByMaterialId,
            Map<UUID, ModuleProgress> moduleProgressByModuleId) {
        List<MaterialLearningResponse> materialResponses = materialsByModuleId
                .getOrDefault(module.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(Material::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .map(material -> materialLearningMapper.toResponse(material, progressByMaterialId.get(material.getId())))
                .toList();

        ModuleProgress moduleProgress = moduleProgressByModuleId.get(module.getId());
        boolean isCompletedFromProgress = moduleProgress != null && Boolean.TRUE.equals(moduleProgress.getIsCompleted());
        boolean isCompletedFromMaterials = !materialResponses.isEmpty()
                && materialResponses.stream().allMatch(MaterialLearningResponse::getCompleted);

        return ModuleLearningResponse.builder()
                .moduleId(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .completed(isCompletedFromProgress || isCompletedFromMaterials)
                .materials(materialResponses)
                .build();
    }

    private BigDecimal toPercentage(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
