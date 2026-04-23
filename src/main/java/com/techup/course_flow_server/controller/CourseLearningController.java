package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.courselearning.CourseLearningResponse;
import com.techup.course_flow_server.service.CourseLearningService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseLearningController {

    private final CourseLearningService courseLearningService;

    public CourseLearningController(CourseLearningService courseLearningService) {
        this.courseLearningService = courseLearningService;
    }

    @GetMapping("/{id}/learning")
    public CourseLearningResponse getCourseLearning(
            @PathVariable("id") UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return courseLearningService.getCourseLearning(courseId, userId);
    }
}