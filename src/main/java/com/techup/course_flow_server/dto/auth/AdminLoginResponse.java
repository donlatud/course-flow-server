package com.techup.course_flow_server.dto.auth;

public record AdminLoginResponse(
        String accessToken,
        String userId,
        String role
) {}
