package com.techup.course_flow_server.dto.courselearning;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CourseLearningResponse {
    private UUID enrollmentId;
    private UUID courseId;
    private String courseTitle;
    private String courseDescription;
    private Integer totalMaterials;
    private Integer completedMaterials;
    private Integer inProgressMaterials;
    private BigDecimal progressPercentage;
    private List<ModuleLearningResponse> modules;
}
