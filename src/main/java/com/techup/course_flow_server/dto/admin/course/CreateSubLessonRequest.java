package com.techup.course_flow_server.dto.admin.course;

import com.techup.course_flow_server.entity.Material;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateSubLessonRequest {

    @NotBlank(message = "Sub-lesson title must not be blank")
    private String title;

    @NotNull(message = "Media type (fileType) is required: VIDEO or IMAGE")
    private Material.FileType fileType;

    private String detail;
}
