package com.techup.course_flow_server.dto.materialprogress;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateResponse {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<BatchUpdateResultItem> results;
}
