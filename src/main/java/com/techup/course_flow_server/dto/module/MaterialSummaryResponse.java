package com.techup.course_flow_server.dto.module;

import com.techup.course_flow_server.entity.Material;
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
public class MaterialSummaryResponse {
    private UUID id;
    private String title;
    private Integer orderIndex;
    private String fileUrl;
    private String detail;
    private Material.FileType fileType;
    private Integer duration;
}
