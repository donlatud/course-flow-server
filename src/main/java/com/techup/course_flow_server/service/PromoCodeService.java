package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.payment.PromoCodeValidationResponse;
import com.techup.course_flow_server.entity.PromoCode;
import com.techup.course_flow_server.repository.PromoCodeRepository;
import com.techup.course_flow_server.repository.PromoRedemptionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Encapsulates promo code validation rules for checkout and order reuse.
 */
@Service
public class PromoCodeService {

    /** Minimum course price required to use promo codes (in THB) */
    public static final BigDecimal MIN_COURSE_PRICE_FOR_PROMO = new BigDecimal("200.00");

    /** Minimum final price after discount (in THB) */
    public static final BigDecimal MIN_FINAL_PRICE = new BigDecimal("100.00");

    /** Minimum payment amount (in THB) - used by OrderService/PaymentService */
    public static final BigDecimal MIN_PAYMENT_AMOUNT = new BigDecimal("100.00");

    private final PromoCodeRepository promoCodeRepository;
    private final PromoRedemptionRepository promoRedemptionRepository;

    public PromoCodeService(
            PromoCodeRepository promoCodeRepository,
            PromoRedemptionRepository promoRedemptionRepository) {
        this.promoCodeRepository = promoCodeRepository;
        this.promoRedemptionRepository = promoRedemptionRepository;
    }

    /**
     * Validates a promo code for the current user and returns a UI-friendly
     * response containing the computed discount values.
     *
     * Business rules:
     * - Course price must be greater than 200 THB to use promo codes
     * - Final price after discount must be at least 100 THB
     */
    public PromoCodeValidationResponse validatePromoCode(UUID userId, String code, BigDecimal originalPrice) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null || originalPrice == null || originalPrice.signum() < 0) {
            return invalidResponse(normalizedCode, PromoCodeValidationResponse.Reason.INVALID);
        }

        // Check minimum course price for promo eligibility (must be > 200 THB)
        if (originalPrice.compareTo(MIN_COURSE_PRICE_FOR_PROMO) <= 0) {
            return invalidResponse(normalizedCode, PromoCodeValidationResponse.Reason.COURSE_PRICE_TOO_LOW);
        }

        PromoCode promoCode = promoCodeRepository.findByCodeIgnoreCase(normalizedCode)
                .orElse(null);

        if (promoCode == null) {
            return invalidResponse(normalizedCode, PromoCodeValidationResponse.Reason.NOT_FOUND);
        }

        PromoCodeValidationResponse.Reason reason = getInvalidReason(userId, promoCode, LocalDateTime.now());
        if (reason != null) {
            return invalidResponse(normalizedCode, reason);
        }

        BigDecimal discountAmount = calculateDiscountAmount(promoCode, originalPrice);
        BigDecimal finalPrice = originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // Check minimum final price after discount (must be >= 100 THB)
        if (finalPrice.compareTo(MIN_FINAL_PRICE) < 0) {
            return invalidResponse(normalizedCode, PromoCodeValidationResponse.Reason.FINAL_PRICE_TOO_LOW);
        }

        return PromoCodeValidationResponse.builder()
                .valid(true)
                .code(promoCode.getCode())
                .discountType(promoCode.getDiscountType())
                .discountValue(promoCode.getDiscountValue())
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .build();
    }

    /**
     * Returns a validated promo code entity that can be attached to an order.
     * Null means no promo code was supplied.
     */
    public PromoCode resolvePromoCodeForOrder(UUID userId, String code, BigDecimal originalPrice) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return null;
        }

        PromoCodeValidationResponse validation = validatePromoCode(userId, normalizedCode, originalPrice);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Promo code is invalid: " + validation.getReason());
        }

        return promoCodeRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));
    }

    /**
     * Computes the amount to subtract based on the promo configuration.
     */
    public BigDecimal calculateDiscountAmount(PromoCode promoCode, BigDecimal originalPrice) {
        if (promoCode == null || originalPrice == null || originalPrice.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discountAmount = switch (promoCode.getDiscountType()) {
            case PERCENTAGE -> originalPrice
                    .multiply(promoCode.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> promoCode.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
        };

        if (discountAmount.compareTo(originalPrice) > 0) {
            return originalPrice.setScale(2, RoundingMode.HALF_UP);
        }

        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private PromoCodeValidationResponse invalidResponse(
            String code,
            PromoCodeValidationResponse.Reason reason) {
        return PromoCodeValidationResponse.builder()
                .valid(false)
                .code(code)
                .reason(reason)
                .build();
    }

    private PromoCodeValidationResponse.Reason getInvalidReason(
            UUID userId,
            PromoCode promoCode,
            LocalDateTime referenceTime) {
        if (promoCode.getValidFrom() != null && referenceTime.isBefore(promoCode.getValidFrom())) {
            return PromoCodeValidationResponse.Reason.EXPIRED;
        }

        if (promoCode.getValidUntil() != null && referenceTime.isAfter(promoCode.getValidUntil())) {
            return PromoCodeValidationResponse.Reason.EXPIRED;
        }

        if (promoCode.getUsageLimit() != null
                && promoCode.getUsageCount() != null
                && promoCode.getUsageCount() >= promoCode.getUsageLimit()) {
            return PromoCodeValidationResponse.Reason.USAGE_LIMIT_REACHED;
        }

        if (promoRedemptionRepository.existsByPromoCodeIdAndUserId(promoCode.getId(), userId)) {
            return PromoCodeValidationResponse.Reason.ALREADY_USED;
        }

        return null;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toUpperCase(Locale.ROOT);
    }
}
