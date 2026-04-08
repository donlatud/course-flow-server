package com.techup.course_flow_server.dto.materialprogress;

import com.techup.course_flow_server.entity.MaterialProgress;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateItem {
    @NotNull(message = "Progress ID is required")
    private UUID progressId;

    private MaterialProgress.Status status;
    private Integer lastPosition;
    private LocalDateTime completedAt;
}
