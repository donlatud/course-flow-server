package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.module.MaterialSummaryResponse;
import com.techup.course_flow_server.dto.module.ModuleResponse;
import com.techup.course_flow_server.dto.module.ModuleWithMaterialsResponse;
import com.techup.course_flow_server.dto.module.UpdateModuleRequest;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuleService {

    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;
    private final CourseRepository courseRepository;

    public ModuleService(CourseModuleRepository courseModuleRepository, 
                         MaterialRepository materialRepository,
                         CourseRepository courseRepository) {
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Get all modules with their materials for a specific course.
     * Only returns data if the course is published.
     * Results are ordered by module orderIndex and material orderIndex.
     */
    public List<ModuleWithMaterialsResponse> getModulesWithMaterialsByCourseId(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Only allow access to published courses
        if (course.getStatus() != Course.Status.PUBLISHED) {
            throw new RuntimeException("Course not found or not published");
        }

        // Get all modules for the course ordered by orderIndex
        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);
        
        if (modules.isEmpty()) {
            return List.of();
        }

        // Get all module IDs
        List<UUID> moduleIds = modules.stream()
                .map(CourseModule::getId)
                .collect(Collectors.toList());

        // Get all materials for these modules ordered by orderIndex
        List<Material> materials = materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);

        // Group materials by module ID
        Map<UUID, List<Material>> materialsByModuleId = materials.stream()
                .collect(Collectors.groupingBy(material -> material.getModule().getId()));

        // Build response with modules and their materials
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

    /**
     * Get all modules for a specific course.
     * Returns basic module info without materials.
     */
    public List<ModuleResponse> getModulesByCourseId(UUID courseId) {
        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);
        return modules.stream()
                .map(this::convertToModuleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all modules across all courses.
     * Returns basic module info without materials.
     */
    public List<ModuleResponse> getAllModules() {
        List<CourseModule> modules = courseModuleRepository.findAll();
        return modules.stream()
                .map(this::convertToModuleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single module by its ID.
     * Returns basic module info without materials.
     */
    public ModuleResponse getModuleById(UUID moduleId) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));
        
        return convertToModuleResponse(module);
    }

    /**
     * Update a module by its ID.
     */
    @Transactional
    public ModuleResponse updateModule(UUID moduleId, UpdateModuleRequest request) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));

        // Update fields if provided
        if (request.getTitle() != null) {
            module.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }
        if (request.getOrderIndex() != null) {
            module.setOrderIndex(request.getOrderIndex());
        }
        if (request.getIsSample() != null) {
            module.setIsSample(request.getIsSample());
        }

        CourseModule updatedModule = courseModuleRepository.save(module);
        return convertToModuleResponse(updatedModule);
    }

    private ModuleResponse convertToModuleResponse(CourseModule module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourse() != null ? module.getCourse().getId() : null)
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .isSample(module.getIsSample())
                .build();
    }
}
