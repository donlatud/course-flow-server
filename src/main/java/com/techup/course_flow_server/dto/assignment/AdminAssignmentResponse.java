package com.techup.course_flow_server.dto.assignment;

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
public class AdminAssignmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID moduleId;
    private String moduleTitle;
    private UUID materialId;
    private String materialTitle;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
