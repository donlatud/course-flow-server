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
@RequestMapping("/api/admin/modules")
public class AdminModulesController {

    private final MaterialService materialService;

    public AdminModulesController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * Get all materials (sub-lessons) for a specific module.
     */
    @GetMapping("/{moduleId}/sub-lessons")
    public List<MaterialResponse> getMaterialsByModuleId(@PathVariable("moduleId") UUID moduleId) {
        return materialService.getMaterialsByModuleId(moduleId);
    }
}
