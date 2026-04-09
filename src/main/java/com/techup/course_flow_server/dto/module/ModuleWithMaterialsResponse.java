package com.techup.course_flow_server.dto.module;

import java.util.List;
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
public class ModuleWithMaterialsResponse {
    private UUID moduleId;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<MaterialSummaryResponse> materials;
}
