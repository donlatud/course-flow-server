package com.techup.course_flow_server.dto.admin.course;

import com.techup.course_flow_server.entity.PromoCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;
}
