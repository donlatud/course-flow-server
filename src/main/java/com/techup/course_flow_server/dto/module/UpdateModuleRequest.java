package com.techup.course_flow_server.dto.module;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateModuleRequest {

    @NotBlank(message = "Lesson name must not be blank")
    private String title;

    private String description;

    private Integer orderIndex;

    private Boolean isSample;
}
