package com.techup.course_flow_server.dto.courselearning;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ModuleLearningResponse {
    private UUID moduleId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean completed;
    private List<MaterialLearningResponse> materials;
}
