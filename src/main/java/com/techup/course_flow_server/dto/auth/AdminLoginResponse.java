package com.techup.course_flow_server.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth2-style JSON names so clients and Jackson always agree (camelCase vs snake_case).
 */
public record AdminLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("user_id") String userId,
        @JsonProperty("role") String role
) {}
