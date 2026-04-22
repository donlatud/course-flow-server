package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.wishlist.WishlistResponse;
import com.techup.course_flow_server.service.WishlistService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/my")
    public List<WishlistResponse> getMyWishlist(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return wishlistService.getMyWishlist(userId);
    }

    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistResponse addToWishlist(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return wishlistService.addToWishlist(userId, courseId);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        wishlistService.removeFromWishlist(userId, courseId);
    }

    @GetMapping("/{courseId}/status")
    public Map<String, Boolean> checkWishlistStatus(
        @PathVariable UUID courseId,
        @AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return Map.of("isInWishlist", wishlistService.isInWishlist(userId, courseId));
    }
}