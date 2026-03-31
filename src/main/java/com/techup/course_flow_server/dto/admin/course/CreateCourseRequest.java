package com.techup.course_flow_server.dto.admin.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateCourseRequest {

    /** Matches UI field: Course name */
    @NotBlank(message = "Course title must not be blank")
    private String title;

    /** Matches UI field: Course summary */
    private String description;

    /** Matches UI field: Course detail */
    private String detail;

    /** Matches UI field: Price */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;

    /** Matches UI field: Total learning time (hours) */
    @NotNull(message = "Total learning time is required")
    @Min(value = 1, message = "Total learning time must be at least 1 hour")
    private Integer totalLearningTime;

    /** Optional — present only when promoEnabled is true in the UI */
    @Valid
    private CreatePromoCodeRequest promoCode;

    @NotEmpty(message = "At least one lesson is required")
    @Valid
    private List<CreateModuleRequest> modules;
}
