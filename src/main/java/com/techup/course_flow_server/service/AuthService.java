package com.techup.course_flow_server.service;

import com.techup.course_flow_server.config.SupabaseProperties;
import com.techup.course_flow_server.dto.auth.AdminLoginResponse;
import com.techup.course_flow_server.dto.auth.AuthResponse;
import com.techup.course_flow_server.dto.auth.LoginRequest;
import com.techup.course_flow_server.dto.auth.RegisterRequest;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.UserRepository;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final SupabaseProperties supabaseProperties;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    public AuthService(SupabaseProperties supabaseProperties,
            UserService userService,
            UserRepository userRepository) {
        this.supabaseProperties = supabaseProperties;
        this.userService = userService;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseProperties.apiKey());
        return headers;
    }

    public AuthResponse login(LoginRequest request) {
        String url = supabaseProperties.url() + "/auth/v1/token?grant_type=password";
        Map<String, String> body = Map.of(
                "email", request.email(),
                "password", request.password());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers());
        Map response = restTemplate.postForObject(url, entity, Map.class);
        return new AuthResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (String) response.get("token_type"),
                Long.valueOf(response.get("expires_in").toString()));
    }

    public AdminLoginResponse adminLogin(LoginRequest request) {
        // 1. Authenticate with Supabase
        String url = supabaseProperties.url() + "/auth/v1/token?grant_type=password";
        Map<String, String> body = Map.of(
                "email", request.email(),
                "password", request.password());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers());

        Map supabaseResponse;
        try {
            supabaseResponse = restTemplate.postForObject(url, entity, Map.class);
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (supabaseResponse == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No response from auth server");
        }

        String accessToken = (String) supabaseResponse.get("access_token");
        if (accessToken == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve access token");
        }

        // 2. Extract user UUID from JWT sub claim (no network call needed)
        UUID userId = extractSubFromJwt(accessToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse token");
        }

        // 3. Look up user in DB – must exist AND be ADMIN
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin access required"));

        if (user.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return new AdminLoginResponse(accessToken, userId.toString(), user.getRole().name());
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            String url = supabaseProperties.url() + "/auth/v1/signup";
            Map<String, String> body = Map.of(
                    "email", request.email(),
                    "password", request.password());
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers());
            Map response = restTemplate.postForObject(url, entity, Map.class);

            Map userMap = (Map) response.get("user");
            String supabaseUserId = (String) userMap.get("id");

            userService.createUser(request, supabaseUserId);

            return new AuthResponse(
                    (String) response.get("access_token"),
                    (String) response.get("refresh_token"),
                    (String) response.get("token_type"),
                    response.get("expires_in") != null
                            ? Long.valueOf(response.get("expires_in").toString())
                            : null);
        } catch (HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            if (body.contains("already registered") || body.contains("user_already_exists")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Register failed");
        }
    }

    private UUID extractSubFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                return null;
            byte[] payloadBytes = Base64.getUrlDecoder().decode(addPadding(parts[1]));
            String payload = new String(payloadBytes);
            int subStart = payload.indexOf("\"sub\":\"");
            if (subStart == -1)
                return null;
            int valueStart = subStart + 7;
            int valueEnd = payload.indexOf("\"", valueStart);
            if (valueEnd == -1)
                return null;
            return UUID.fromString(payload.substring(valueStart, valueEnd));
        } catch (Exception ex) {
            return null;
        }
    }

    private String addPadding(String base64) {
        int mod = base64.length() % 4;
        if (mod == 2)
            return base64 + "==";
        if (mod == 3)
            return base64 + "=";
        return base64;
    }
}
