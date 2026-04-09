package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.module.ModuleWithMaterialsResponse;
import com.techup.course_flow_server.service.ModuleService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    /**
     * Get all modules (lessons) with their materials (sub-lessons) for a specific course.
     * Results are ordered by module order_index and material order_index.
     * Only accessible for published courses.
     */
    @GetMapping("/{courseId}/modules-with-materials")
    public List<ModuleWithMaterialsResponse> getModulesWithMaterialsByCourseId(@PathVariable("courseId") UUID courseId) {
        return moduleService.getModulesWithMaterialsByCourseId(courseId);
    }
}
