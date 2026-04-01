package com.techup.course_flow_server.dto.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {}