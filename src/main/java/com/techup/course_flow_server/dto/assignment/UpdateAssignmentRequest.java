package com.techup.course_flow_server.dto.assignment;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateAssignmentRequest {

    private UUID courseId;

    private UUID moduleId;

    private UUID materialId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
