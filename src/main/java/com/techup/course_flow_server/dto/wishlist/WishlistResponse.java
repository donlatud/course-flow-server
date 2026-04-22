package com.techup.course_flow_server.dto.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WishlistResponse(
    UUID wishlistId,
    UUID courseId,
    String courseTitle,
    String courseDescription,
    String coverImageUrl,
    BigDecimal price,
    Integer totalLearningTime,
    LocalDateTime addedAt
) {}