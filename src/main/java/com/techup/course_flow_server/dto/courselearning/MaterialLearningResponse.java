package com.techup.course_flow_server.dto.courselearning;

import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.MaterialProgress;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MaterialLearningResponse {
    private UUID materialId;
    private String title;
    private Integer orderIndex;
    private String fileUrl;
    private Material.FileType fileType;
    private Integer duration;
    private MaterialProgress.Status status;
    private Integer lastPosition;
    private Boolean completed;
}
