package com.techup.course_flow_server.dto.materialprogress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateRequest {
    @NotEmpty(message = "Updates list cannot be empty")
    @Size(max = 50, message = "Cannot update more than 50 items at once")
    @Valid
    private List<BatchUpdateItem> updates;
}
