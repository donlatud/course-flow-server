package com.techup.course_flow_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//@Component
public class MockAuthFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ID_ATTR = "authenticatedUserId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        UUID userId = extractUserId(request);
        if (userId == null) {
            writeUnauthorized(response, request);
            return;
        }

        request.setAttribute(AUTHENTICATED_USER_ID_ATTR, userId);
        filterChain.doFilter(request, response);
    }

    private UUID extractUserId(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            UUID fromBearer = parseUuid(token);
            if (fromBearer != null) {
                return fromBearer;
            }
        }

        String fromHeader = request.getHeader("X-User-Id");
        return parseUuid(fromHeader);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String message = "Provide Authorization: Bearer <user-id> or X-User-Id header";
        String path = request.getRequestURI();
        String body = "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\""
                + escapeJson(message)
                + "\",\"path\":\""
                + escapeJson(path)
                + "\"}";
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
