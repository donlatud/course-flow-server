package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.material.MaterialResponse;
import com.techup.course_flow_server.service.MaterialService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * Get all materials for a specific course, ordered by order_index.
     * Only accessible for published courses.
     */
    @GetMapping("/{courseId}/materials")
    public List<MaterialResponse> getMaterialsByCourseId(@PathVariable("courseId") UUID courseId) {
        return materialService.getMaterialsByCourseId(courseId);
    }
}
