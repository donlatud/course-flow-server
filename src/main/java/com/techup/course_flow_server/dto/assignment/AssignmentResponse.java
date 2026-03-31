package com.techup.course_flow_server.dto.assignment;

import com.techup.course_flow_server.entity.AssignmentSubmission;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private UUID assignmentId;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean submitted;
    private UUID submissionId;
    private AssignmentSubmission.Status submissionStatus;
    private LocalDateTime submittedAt;
}
