package com.techup.course_flow_server.dto.materialprogress;

import com.techup.course_flow_server.entity.MaterialProgress;
import java.time.LocalDateTime;
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
public class MaterialProgressUpdateRequest {
    private MaterialProgress.Status status;
    private Integer lastPosition;
    private LocalDateTime completedAt;
}
