package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.assignment.AssignmentResponse;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionRequest;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionResponse;
import com.techup.course_flow_server.security.MockAuthFilter;
import com.techup.course_flow_server.service.AssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<AssignmentResponse> getAssignments(
            @PathVariable UUID courseId,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return assignmentService.getAssignments(courseId, userId);
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public AssignmentSubmissionResponse submitAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return assignmentService.submitAssignment(assignmentId, request, userId);
    }
}
