package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.module.ModuleResponse;
import com.techup.course_flow_server.dto.module.UpdateModuleRequest;
import com.techup.course_flow_server.service.ModuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/modules")
public class ModulesController {

    private final ModuleService moduleService;

    public ModulesController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    /**
     * Get all modules across all courses.
     * Returns basic module info without materials.
     */
    @GetMapping
    public List<ModuleResponse> getAllModules() {
        return moduleService.getAllModules();
    }

    /**
     * Get a single module by its ID.
     * Returns basic module info without materials.
     */
    @GetMapping("/{moduleId}")
    public ModuleResponse getModuleById(@PathVariable("moduleId") UUID moduleId) {
        return moduleService.getModuleById(moduleId);
    }

    /**
     * Update a module by its ID.
     */
    @PutMapping("/{moduleId}")
    public ModuleResponse updateModule(
            @PathVariable("moduleId") UUID moduleId,
            @Valid @RequestBody UpdateModuleRequest request) {
        return moduleService.updateModule(moduleId, request);
    }
}
