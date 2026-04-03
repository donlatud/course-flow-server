package com.techup.course_flow_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MockAuthFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ID_ATTR = "authenticatedUserId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Always allow CORS preflight requests (OPTIONS) to pass through
        // so that browsers can perform the CORS handshake successfully.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Public auth endpoints — no token required
        if (path.startsWith("/api/auth/")) {
            return true;
        }

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

            UUID fromJwt = extractSubFromJwt(token);
            if (fromJwt != null) {
                return fromJwt;
            }
        }

        String fromHeader = request.getHeader("X-User-Id");
        return parseUuid(fromHeader);
    }

    private UUID extractSubFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            byte[] payloadBytes = Base64.getUrlDecoder().decode(addPadding(parts[1]));
            String payload = new String(payloadBytes);
            int subStart = payload.indexOf("\"sub\":\"");
            if (subStart == -1) return null;
            int valueStart = subStart + 7;
            int valueEnd = payload.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;
            return parseUuid(payload.substring(valueStart, valueEnd));
        } catch (Exception ex) {
            return null;
        }
    }

    private String addPadding(String base64) {
        int mod = base64.length() % 4;
        if (mod == 2) return base64 + "==";
        if (mod == 3) return base64 + "=";
        return base64;
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
