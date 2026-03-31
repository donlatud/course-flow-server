package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.assignment.AssignmentResponse;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionRequest;
import com.techup.course_flow_server.dto.assignment.AssignmentSubmissionResponse;
import com.techup.course_flow_server.entity.Assignment;
import com.techup.course_flow_server.entity.AssignmentSubmission;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.repository.AssignmentRepository;
import com.techup.course_flow_server.repository.AssignmentSubmissionRepository;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            EnrollmentRepository enrollmentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public List<AssignmentResponse> getAssignments(UUID courseId, UUID userId) {
        // Ensure caller is enrolled in this course before exposing assignments.
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for this user and course"));

        List<Assignment> assignments = assignmentRepository.findAllByCourseIdOrderByStartDateAsc(courseId);
        List<UUID> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        Map<UUID, AssignmentSubmission> submissionByAssignmentId = assignmentIds.isEmpty()
                ? Map.of()
                : assignmentSubmissionRepository.findAllByAssignmentIdInAndUserId(assignmentIds, userId).stream()
                        .collect(Collectors.toMap(submission -> submission.getAssignment().getId(), Function.identity()));

        return assignments.stream()
                .map(assignment -> {
                    AssignmentSubmission submission = submissionByAssignmentId.get(assignment.getId());
                    boolean submitted = submission != null && submission.getStatus() == AssignmentSubmission.Status.SUBMITTED;
                    return AssignmentResponse.builder()
                            .assignmentId(assignment.getId())
                            .title(assignment.getTitle())
                            .description(assignment.getDescription())
                            .startDate(assignment.getStartDate())
                            .endDate(assignment.getEndDate())
                            .submitted(submitted)
                            .submissionId(submission != null ? submission.getId() : null)
                            .submissionStatus(submission != null ? submission.getStatus() : null)
                            .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                            .build();
                })
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(
            UUID assignmentId,
            AssignmentSubmissionRequest request,
            UUID userId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, assignment.getCourse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for this user and course"));

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndUserId(assignmentId, userId)
                .orElseGet(() -> AssignmentSubmission.builder()
                        .assignment(assignment)
                        .user(enrollment.getUser())
                        .status(AssignmentSubmission.Status.IN_PROGRESS)
                        .build());

        submission.setSubmissionText(request.getSubmissionText());
        submission.setFileUrl(request.getFileUrl());
        submission.setStatus(AssignmentSubmission.Status.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionResponse.builder()
                .submissionId(saved.getId())
                .assignmentId(saved.getAssignment().getId())
                .userId(saved.getUser().getId())
                .status(saved.getStatus())
                .submissionText(saved.getSubmissionText())
                .fileUrl(saved.getFileUrl())
                .submittedAt(saved.getSubmittedAt())
                .build();
    }
}
