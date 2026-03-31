package com.techup.course_flow_server.dto.assignment;

import jakarta.validation.constraints.AssertTrue;
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
public class AssignmentSubmissionRequest {
    private String submissionText;
    private String fileUrl;

    @AssertTrue(message = "Either submissionText or fileUrl is required")
    public boolean hasSubmissionContent() {
        return hasText(submissionText) || hasText(fileUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
