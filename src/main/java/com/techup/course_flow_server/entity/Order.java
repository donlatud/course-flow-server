package com.techup.course_flow_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Pending orders expire 60 minutes after creation.
     * This value is used by service-layer lazy expiry checks.
     */
    private static final long EXPIRY_MINUTES = 60L;

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Returns the timestamp after which this order should no longer be used
     * for payment attempts.
     */
    public LocalDateTime getExpiresAt() {
        if (expiresAt != null) {
            return expiresAt;
        }
        return createdAt == null ? null : createdAt.plusMinutes(EXPIRY_MINUTES);
    }

    /**
     * Helper used by services to lazily detect expired pending orders without
     * requiring a dedicated expires_at column in the first phase.
     */
    public boolean isExpiredAt(LocalDateTime referenceTime) {
        if (status != Status.PENDING || createdAt == null || referenceTime == null) {
            return false;
        }

        return !referenceTime.isBefore(createdAt.plusMinutes(EXPIRY_MINUTES));
    }

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED,
        EXPIRED
    }
}
