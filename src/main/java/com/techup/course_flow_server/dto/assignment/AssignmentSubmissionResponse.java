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
public class AssignmentSubmissionResponse {
    private UUID submissionId;
    private UUID assignmentId;
    private UUID userId;
    private AssignmentSubmission.Status status;
    private String submissionText;
    private String fileUrl;
    private LocalDateTime submittedAt;
    /**
     * Model answer / solution text for the assignment.
     * Returned after successful submission.
     */
    private String solution;
}
