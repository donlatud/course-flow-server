package com.techup.course_flow_server.dto.admin.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
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
public class CreateModuleRequest {

    @NotBlank(message = "Lesson name must not be blank")
    private String title;

    @NotEmpty(message = "At least one sub-lesson is required per lesson")
    @Valid
    private List<CreateSubLessonRequest> subLessons;
}
