package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.materialprogress.MaterialProgressCreateRequest;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressResponse;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressUpdateRequest;
import com.techup.course_flow_server.security.MockAuthFilter;
import com.techup.course_flow_server.service.MaterialProgressService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/material-progress")
public class MaterialProgressController {

    private final MaterialProgressService materialProgressService;

    public MaterialProgressController(MaterialProgressService materialProgressService) {
        this.materialProgressService = materialProgressService;
    }

    @PostMapping
    public MaterialProgressResponse createProgress(
            @Valid @RequestBody MaterialProgressCreateRequest request,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return materialProgressService.createProgress(request, userId);
    }

    @PutMapping("/{id}")
    public MaterialProgressResponse updateProgress(
            @PathVariable("id") UUID progressId,
            @Valid @RequestBody MaterialProgressUpdateRequest request,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return materialProgressService.updateProgress(progressId, request, userId);
    }
}
