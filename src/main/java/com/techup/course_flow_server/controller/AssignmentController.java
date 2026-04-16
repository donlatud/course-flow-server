package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.assignment.MyAssignmentCourseResponse;
import com.techup.course_flow_server.dto.assignment.AssignmentResponse;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionRequest;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionResponse;
import com.techup.course_flow_server.service.AssignmentService;
import com.techup.course_flow_server.service.MyAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final MyAssignmentService myAssignmentService;

    public AssignmentController(AssignmentService assignmentService, MyAssignmentService myAssignmentService) {
        this.assignmentService = assignmentService;
        this.myAssignmentService = myAssignmentService;
    }

    @GetMapping("/assignments/my-courses")
    public List<MyAssignmentCourseResponse> getMyAssignmentCourses(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return myAssignmentService.getMyAssignmentCourses(userId);
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<AssignmentResponse> getAssignments(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return assignmentService.getAssignments(courseId, userId);
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public AssignmentSubmissionResponse submitAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return assignmentService.submitAssignment(assignmentId, request, userId);
    }
}