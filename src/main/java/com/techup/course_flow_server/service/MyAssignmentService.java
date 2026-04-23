package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.assignment.MyAssignmentCourseResponse;
import com.techup.course_flow_server.entity.AssignmentSubmission;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.repository.AssignmentRepository;
import com.techup.course_flow_server.repository.AssignmentSubmissionRepository;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyAssignmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    public MyAssignmentService(
            EnrollmentRepository enrollmentRepository,
            CourseModuleRepository courseModuleRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public List<MyAssignmentCourseResponse> getMyAssignmentCourses(UUID userId) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserIdAndStatusIn(
                userId,
                List.of(Enrollment.Status.ACTIVE, Enrollment.Status.COMPLETED));

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .distinct()
                .toList();

        Map<UUID, Integer> lessonCountByCourseId = courseModuleRepository.countByCourseIdIn(courseIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).intValue(),
                        (a, b) -> a));

        Map<UUID, Long> assignmentCountByCourseId = assignmentRepository.countByCourseIdIn(courseIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a));

        Map<UUID, Long> submittedCountByCourseId =
                assignmentSubmissionRepository.countByCourseIdInAndUserIdAndStatusIn(
                                courseIds,
                                userId,
                                List.of(AssignmentSubmission.Status.SUBMITTED, AssignmentSubmission.Status.GRADED))
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (UUID) row[0],
                                row -> ((Number) row[1]).longValue(),
                                (a, b) -> a));

        // Keep a stable output order: by course title ASC then courseId.
        return enrollments.stream()
                .map(Enrollment::getCourse)
                .distinct()
                .sorted(Comparator
                        .comparing((com.techup.course_flow_server.entity.Course c) -> c.getTitle() != null ? c.getTitle() : "")
                        .thenComparing(com.techup.course_flow_server.entity.Course::getId))
                // Spec: do not include courses with no assignments.
                .filter(course -> assignmentCountByCourseId.getOrDefault(course.getId(), 0L) > 0)
                .map(course -> {
                    UUID courseId = course.getId();
                    long totalAssignments = assignmentCountByCourseId.getOrDefault(courseId, 0L);
                    long submittedAssignments = submittedCountByCourseId.getOrDefault(courseId, 0L);

                    MyAssignmentCourseResponse.AssignmentStatus status = computeStatus(totalAssignments, submittedAssignments);

                    return MyAssignmentCourseResponse.builder()
                            .courseId(courseId)
                            .courseTitle(course.getTitle())
                            .courseDescription(course.getDescription())
                            .coverImageUrl(course.getCoverImageUrl())
                            .lessonCount(lessonCountByCourseId.getOrDefault(courseId, 0))
                            .totalHours(course.getTotalLearningTime())
                            .totalAssignments(totalAssignments)
                            .submittedAssignments(submittedAssignments)
                            .assignmentStatus(status)
                            .build();
                })
                .toList();
    }

    private static MyAssignmentCourseResponse.AssignmentStatus computeStatus(long total, long submitted) {
        if (total <= 0) {
            return MyAssignmentCourseResponse.AssignmentStatus.NOT_STARTED;
        }
        if (submitted <= 0) {
            return MyAssignmentCourseResponse.AssignmentStatus.NOT_STARTED;
        }
        if (submitted >= total) {
            return MyAssignmentCourseResponse.AssignmentStatus.COMPLETED;
        }
        return MyAssignmentCourseResponse.AssignmentStatus.IN_PROGRESS;
    }
}

