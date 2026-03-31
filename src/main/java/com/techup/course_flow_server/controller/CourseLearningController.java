package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.courselearning.CourseLearningResponse;
import com.techup.course_flow_server.security.MockAuthFilter;
import com.techup.course_flow_server.service.CourseLearningService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
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
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return courseLearningService.getCourseLearning(courseId, userId);
    }
}
