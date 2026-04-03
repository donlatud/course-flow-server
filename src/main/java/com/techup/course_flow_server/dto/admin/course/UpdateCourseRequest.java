package com.techup.course_flow_server.dto.admin.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class UpdateCourseRequest {

    @NotBlank(message = "Course title must not be blank")
    private String title;

    private String description;

    private String detail;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;

    @NotNull(message = "Total learning time is required")
    @Min(value = 1, message = "Total learning time must be at least 1 hour")
    private Integer totalLearningTime;

    private String coverImageUrl;

    private String trailerVideoUrl;

    private String attachmentUrl;

    @Valid
    private CreatePromoCodeRequest promoCode;

    @Valid
    private List<CreateModuleRequest> modules;
}
