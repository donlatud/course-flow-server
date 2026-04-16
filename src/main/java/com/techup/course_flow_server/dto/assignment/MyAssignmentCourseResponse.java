package com.techup.course_flow_server.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAssignmentCourseResponse {
    private UUID courseId;
    private String courseTitle;
    private String courseDescription;
    private String coverImageUrl;
    private Integer lessonCount;
    private Integer totalHours;
    private long totalAssignments;
    private long submittedAssignments;
    private AssignmentStatus assignmentStatus;

    public enum AssignmentStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
}

