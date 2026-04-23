package com.techup.course_flow_server.dto.admin.promo;

import com.techup.course_flow_server.entity.PromoCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class AdminUpsertPromoCodeRequest {

    @NotBlank(message = "Promo code must not be blank")
    private String code;

    @NotNull(message = "Discount type is required")
    private PromoCode.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Discount value must be at least 0")
    private BigDecimal discountValue;

    private BigDecimal minimumPurchaseAmount;

    private List<UUID> courseIds;
}
