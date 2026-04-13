package com.techup.course_flow_server.service;

import com.techup.course_flow_server.entity.Assignment;
import com.techup.course_flow_server.entity.AssignmentSubmission;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.MaterialProgress;
import com.techup.course_flow_server.repository.AssignmentRepository;
import com.techup.course_flow_server.repository.AssignmentSubmissionRepository;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.MaterialProgressRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Course completion % = (completed materials + finished assignment submissions) /
 * (total materials + total assignments). Assignments without a row in assignment_submissions
 * count as not done.
 */
@Component
public class EnrollmentProgressCalculator {

    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;
    private final MaterialProgressRepository materialProgressRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    public EnrollmentProgressCalculator(
            CourseModuleRepository courseModuleRepository,
            MaterialRepository materialRepository,
            MaterialProgressRepository materialProgressRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository) {
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
        this.materialProgressRepository = materialProgressRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
    }

    public BigDecimal computeProgressPercentage(Enrollment enrollment) {
        ProgressTotals totals = computeTotals(enrollment);
        return toPercentage(totals.completedUnits(), totals.totalUnits());
    }

    public ProgressTotals computeTotals(Enrollment enrollment) {
        UUID courseId = enrollment.getCourse().getId();
        UUID userId = enrollment.getUser().getId();

        List<CourseModule> courseModules =
                courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);
        List<UUID> moduleIds = courseModules.stream().map(CourseModule::getId).toList();

        List<Material> materials = moduleIds.isEmpty()
                ? Collections.emptyList()
                : materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);

        Map<UUID, MaterialProgress> progressByMaterialId = materialProgressRepository
                .findAllByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(
                        mp -> mp.getMaterial().getId(),
                        Function.identity(),
                        (a, b) -> a));

        int totalMaterials = materials.size();
        int completedMaterials = (int) materials.stream()
                .filter(material -> {
                    MaterialProgress progress = progressByMaterialId.get(material.getId());
                    return progress != null && progress.getStatus() == MaterialProgress.Status.COMPLETED;
                })
                .count();

        List<Assignment> assignments = assignmentRepository.findAllByCourseIdOrderByStartDateAsc(courseId);
        List<UUID> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        Map<UUID, AssignmentSubmission> submissionByAssignmentId = assignmentIds.isEmpty()
                ? Map.of()
                : assignmentSubmissionRepository.findAllByAssignmentIdInAndUserId(assignmentIds, userId).stream()
                        .collect(Collectors.toMap(
                                s -> s.getAssignment().getId(),
                                Function.identity(),
                                (a, b) -> a));

        int totalAssignments = assignments.size();
        int completedAssignments = (int) assignments.stream()
                .filter(a -> {
                    AssignmentSubmission s = submissionByAssignmentId.get(a.getId());
                    return s != null && submissionCountsAsComplete(s);
                })
                .count();

        return new ProgressTotals(totalMaterials, completedMaterials, totalAssignments, completedAssignments);
    }

    private static boolean submissionCountsAsComplete(AssignmentSubmission s) {
        return s.getStatus() == AssignmentSubmission.Status.SUBMITTED
                || s.getStatus() == AssignmentSubmission.Status.GRADED;
    }

    private static BigDecimal toPercentage(int completed, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public record ProgressTotals(
            int totalMaterials,
            int completedMaterials,
            int totalAssignments,
            int completedAssignments) {
        public int totalUnits() {
            return totalMaterials + totalAssignments;
        }

        public int completedUnits() {
            return completedMaterials + completedAssignments;
        }
    }
}
