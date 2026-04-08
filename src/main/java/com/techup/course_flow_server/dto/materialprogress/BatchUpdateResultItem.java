package com.techup.course_flow_server.dto.materialprogress;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateResultItem {
    private UUID progressId;
    private boolean success;
    private String errorMessage;
    private MaterialProgressResponse data;
}
