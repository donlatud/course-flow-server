package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.materialprogress.BatchUpdateRequest;
import com.techup.course_flow_server.dto.materialprogress.BatchUpdateResponse;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressCreateRequest;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressResponse;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressUpdateRequest;
import com.techup.course_flow_server.security.MockAuthFilter;
import com.techup.course_flow_server.service.MaterialProgressService;
import com.techup.course_flow_server.service.RateLimitService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/material-progress")
public class MaterialProgressController {

    private final MaterialProgressService materialProgressService;
    private final RateLimitService rateLimitService;

    public MaterialProgressController(
            MaterialProgressService materialProgressService,
            RateLimitService rateLimitService) {
        this.materialProgressService = materialProgressService;
        this.rateLimitService = rateLimitService;
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
        // Rate limit: max 1 request per 10 seconds per user
        String rateLimitKey = rateLimitService.buildMaterialProgressUpdateKey(userId);
        if (!rateLimitService.isAllowed(rateLimitKey, 1, 10)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many progress update requests. Please wait before retrying.");
        }

        return materialProgressService.updateProgress(progressId, request, userId);
    }

    @PostMapping("/batch")
    public BatchUpdateResponse batchUpdateProgress(
            @Valid @RequestBody BatchUpdateRequest request,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return materialProgressService.batchUpdateProgress(request, userId);
    }
}
