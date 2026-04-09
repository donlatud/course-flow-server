package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.material.MaterialResponse;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final CourseRepository courseRepository;

    public MaterialService(MaterialRepository materialRepository, CourseRepository courseRepository) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Get all materials for a specific course, ordered by module order index and material order index.
     * Only returns materials if the course is published.
     */
    public List<MaterialResponse> getMaterialsByCourseId(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Only allow access to published courses
        if (course.getStatus() != Course.Status.PUBLISHED) {
            throw new RuntimeException("Course not found or not published");
        }

        List<Material> materials = materialRepository.findAllByCourseIdOrderByModuleOrderIndexAscOrderIndexAsc(courseId);

        return materials.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MaterialResponse convertToResponse(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .moduleId(material.getModule().getId())
                .moduleTitle(material.getModule().getTitle())
                .title(material.getTitle())
                .orderIndex(material.getOrderIndex())
                .fileUrl(material.getFileUrl())
                .detail(material.getDetail())
                .fileType(material.getFileType())
                .duration(material.getDuration())
                .build();
    }
}
