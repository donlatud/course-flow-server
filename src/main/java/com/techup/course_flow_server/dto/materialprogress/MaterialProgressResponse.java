package com.techup.course_flow_server.dto.materialprogress;

import com.techup.course_flow_server.entity.MaterialProgress;
import java.math.BigDecimal;
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
public class MaterialProgressResponse {
    private UUID id;
    private UUID enrollmentId;
    private UUID materialId;
    private MaterialProgress.Status status;
    private Integer lastPosition;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    private Boolean moduleCompleted;
    private BigDecimal enrollmentProgressPercentage;
}
