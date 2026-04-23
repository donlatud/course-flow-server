package com.techup.course_flow_server.dto.admin.course;

import com.techup.course_flow_server.entity.PromoCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
public class CreatePromoCodeRequest {

    @NotBlank(message = "Promo code must not be blank")
    private String code;

    @NotNull(message = "Discount type is required: PERCENTAGE or FIXED_AMOUNT")
    private PromoCode.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Discount value must be at least 0")
    @DecimalMax(value = "99999999.99", message = "Discount value is too large")
    private BigDecimal discountValue;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    /**
     * Optional list of course IDs that this promo code can be applied to.
     * If omitted in course-create flow, service will link it to the created course.
     */
    private List<UUID> courseIds;
}
